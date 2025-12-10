/*
 * Copyright 1999–2025 ViaOA (info@viaoa.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.viaoa.comm.multiplexer.io;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * Manages the shared {@link java.io.DataOutputStream} for a single physical
 * TCP connection used by the multiplexer. All outbound data from every
 * {@link VirtualSocket} is serialized, framed, and written by this controller.
 *
 * <p>
 * Responsibilities:
 * </p>
 *
 * <ul>
 *   <li>Serializing virtual socket frames: write header (socket id, length)
 *       followed by the payload bytes.</li>
 *   <li>Providing exclusive, fair access to the underlying output stream so
 *       that multiple threads can write safely.</li>
 *   <li>Chunking large writes so that a single writer cannot monopolize the
 *       real socket.</li>
 *   <li>Optionally throttling throughput to a configured MB/second limit.</li>
 *   <li>Sending control commands (create/close virtual sockets, ping, close
 *       real socket) to the remote side.</li>
 * </ul>
 *
 * <p>
 * Virtual sockets call {@link #write(VirtualSocket, byte[], int, int)} to
 * send data. The controller enforces a cooperative fairness policy using
 * an internal wait queue and chunk-size adaptation based on the number of
 * waiting writers.
 * </p>
 *
 * <p>
 * Command frames are sent using {@link #sendCommand(int, int, String)} and
 * are consumed by the peer's {@link MultiplexerInputStreamController}
 * implementation.
 * </p>
 *
 * <p>
 * Write statistics (count and total size) are tracked for monitoring and
 * performance diagnostics.
 * </p>
 */
public class MultiplexerOutputStreamController {
	private static Logger LOG = Logger.getLogger(MultiplexerOutputStreamController.class.getName());

	/**
	 * The shared real-socket output stream onto which all multiplexer frames are
	 * written.
	 */
	private DataOutputStream _dataOutputStream;

	/**
	 * Indicates whether the real output stream has been flagged as closed. Writers
	 * must stop writing once this is set.
	 */
	private volatile boolean _bIsClosed;

	/**
	 * Internal flag indicating whether a VirtualSocket currently owns the write
	 * lock for the shared DataOutputStream.
	 */
	private volatile boolean _bWritingLock;

	/**
	 * Number of VirtualSockets currently waiting to acquire the write lock. Used
	 * to compute fair write chunk sizes.
	 */
	private volatile int _writeLockWaitingCount; // this is only changed within a synch block

	/**
	 * Maximum allowed write throughput in megabytes per second across all writers.
	 * A value of zero disables throttling.
	 */
	private int mbThrottleLimitPerSecond;

	/**
	 * Maximum number of bytes that may be written during one throttling interval
	 * (1 / {@link #iThrottleFractionOfSecond} seconds).
	 */
	private int iThrottleLimitPerFractionSecond; // number of bytes per 1/iThrottleGrandularity second

	/**
	 * Number of times per second throttling checks occur. Determines the size of
	 * each throttling time slice.
	 */
	private final int iThrottleFractionOfSecond = 250; // this will use 1000/250 = 4 miliseconds

	/**
	 * Lock object used to synchronize and serialize access to the shared output
	 * stream among multiple VirtualSocket writers.
	 */
	private final transient Object WRITELOCK = new Object();

	/**
	 * Metrics tracking total bytes written and total number of write operations
	 * performed through this controller.
	 */
	private AtomicLong aiWriteSize = new AtomicLong();
	private AtomicLong aiWriteCnt = new AtomicLong();
	
	/**
	 * Internal state used to enforce write throttling across small fractional time
	 * windows.
	 */
	private long throttleLastMs;
	private long throttleTotalBytesWritten;
	
	/**
	 * Creates a new output-stream controller. The controller begins in a locked
	 * state until {@link #setDataOutputStream(DataOutputStream)} provides the real
	 * underlying output stream.
	 */
	MultiplexerOutputStreamController() {
		this._bWritingLock = true;
	}

	/**
	 * Configures the maximum number of megabytes per second that the controller
	 * may write. A value of zero disables throttling.
	 *
	 * @param mbPerSecond desired write limit in MB/sec
	 */
	public void setThrottleLimit(int mbPerSecond) {
		mbThrottleLimitPerSecond = mbPerSecond;
		if (mbPerSecond > 0) {
			iThrottleLimitPerFractionSecond = (mbPerSecond * 1024 * 1024) / iThrottleFractionOfSecond;
		} else {
			iThrottleLimitPerFractionSecond = 0;
		}
	}

	/**
	 * Returns the configured MB/sec throttle limit.
	 *
	 * @return throttle limit, or zero if disabled
	 */
	public int getThrottleLimit() {
		return mbThrottleLimitPerSecond;
	}

	/**
	 * Sets the real output stream used for all writes. Releases the initial
	 * write lock so VirtualSockets may begin writing.
	 *
	 * @param dataOutputStream shared output stream for the real socket
	 */
	void setDataOutputStream(DataOutputStream dataOutputStream) {
		this._dataOutputStream = dataOutputStream;
		synchronized (WRITELOCK) {
			this._bWritingLock = false;
			WRITELOCK.notifyAll();
		}
	}

	/**
	 * Marks the controller as closed and wakes all VirtualSockets waiting to
	 * acquire the write lock. Future write attempts will fail.
	 */
	void close() {
		synchronized (WRITELOCK) {
			this._bIsClosed = true;
			WRITELOCK.notifyAll();
		}
	}

	/**
	 * Returns the number of write operations completed by this controller.
	 *
	 * @return count of writes
	 */
	public long getWriteCount() {
		return aiWriteCnt.get();
	}

	/**
	 * Returns the total number of bytes written to the real output stream.
	 *
	 * @return cumulative byte count
	 */
	public long getWriteSize() {
		return aiWriteSize.get();
	}

	/**
	 * Writes a full payload for the specified VirtualSocket. The payload is broken
	 * into chunks using {@link #getMaxWriteLength(VirtualSocket, int)} so that no
	 * single writer monopolizes the real socket.
	 *
	 * <p>After chunking, each subsection is written using {@link #_write}.</p>
	 *
	 * @param vs virtual socket issuing the write
	 * @param bs source buffer
	 * @param off starting offset in the buffer
	 * @param fullLength total number of bytes to write
	 * @throws IOException if the stream is closed or a write error occurs
	 */
	void write(VirtualSocket vs, byte[] bs, int off, int fullLength) throws IOException {
		// make sure that data is sent in chunks

		int pos = 0;
		do {
			int len = fullLength - pos;
			if (len > 8192) {
				// get max length of data "chunk", partially based on the number of threads waiting.
				len = getMaxWriteLength(vs, len);
			}
			_write(vs, bs, off + pos, len);
			pos += len;
		} while (pos < fullLength);

		aiWriteCnt.incrementAndGet();
		aiWriteSize.addAndGet(fullLength);
	}

	/**
	 * Writes a single chunk of the payload. Acquires the WRITELOCK via
	 * {@link #getOutputStream()} and ensures the stream is released via
	 * {@link #releaseOutputStream()}.
	 *
	 * <p>This method writes the virtual-socket header (id + payload length) and
	 * then the bytes for the chunk.</p>
	 *
	 * @param vs virtual socket issuing the write
	 * @param bs source buffer
	 * @param offset buffer position
	 * @param len number of bytes in this chunk
	 */
	private void _write(VirtualSocket vs, byte[] bs, int offset, int len) throws IOException {
		// this method will create a lock from other threads, since the outputStream is a shared
		// resource.
		// getOutputStream is synchronized until it is released, this will make sure that there are
		// no other threads using the shared objects
		DataOutputStream outputStream = getOutputStream();
		try {
			_write(vs, bs, offset, len, outputStream);
		} finally {
			releaseOutputStream(); // this will flush
		}
	}

	/**
	 * Performs the actual header and data write to the underlying stream. Updates
	 * throttling counters and sleeps if the current throttling window has been
	 * exceeded.
	 *
	 * @param vs virtual socket issuing the write
	 * @param bs buffer containing data
	 * @param offset buffer offset
	 * @param len number of bytes to write
	 * @param outputStream the locked shared output stream
	 * @throws IOException if writing to the stream fails
	 */
	private void _write(VirtualSocket vs, byte[] bs, int offset, int len, DataOutputStream outputStream) throws IOException {
		// System.out.println("   mosc_write=>  vs.id="+vs._id+", offset="+offset+", len="+len+", waitingCount="+_writeLockWaitingCount+", thread="+Thread.currentThread().getName());
		outputStream.writeInt(vs._id); // header
		outputStream.writeInt(len); // header
		outputStream.write(bs, offset, len);

		// this is to throttle the amount of data that can be written per fraction of a second (ex: 10x = 100ms)
		if (iThrottleLimitPerFractionSecond > 0) {
			throttleTotalBytesWritten += len;
			long msNow = System.currentTimeMillis();
			if (throttleLastMs == 0) {
				throttleLastMs = msNow;
			}
			long msDiff = msNow - throttleLastMs;
			if (msDiff >= (1000 / iThrottleFractionOfSecond)) { // compare in miliseconds
				// reset
				throttleTotalBytesWritten = len;
				throttleLastMs = msNow;
			} else if (throttleTotalBytesWritten > iThrottleLimitPerFractionSecond) {
				// need to sleep for remainder of fraction of second
				int msSleep = (int) ((1000 / iThrottleFractionOfSecond) - msDiff);
				try {
					Thread.sleep(msSleep);
				} catch (InterruptedException e) {
				}
				throttleTotalBytesWritten = (throttleTotalBytesWritten - iThrottleLimitPerFractionSecond);
				throttleLastMs = msNow + msSleep;
			}
		}
	}

	/**
	 * Returns the recommended maximum chunk size for a write operation based on
	 * the current number of threads waiting to acquire the write lock.
	 *
	 * <ul>
	 *   <li>No waiting threads → up to 32 KB</li>
	 *   <li>1 waiting thread → 16 KB</li>
	 *   <li>2–5 waiting threads → 8 KB</li>
	 *   <li>6+ waiting threads → 4 KB</li>
	 * </ul>
	 *
	 * @param vs virtual socket requesting the write
	 * @param requestSize total request size for this chunk
	 * @return permitted chunk size
	 */
	protected int getMaxWriteLength(VirtualSocket vs, int requestSize) {
		int max;
		if (_writeLockWaitingCount == 0) {
			max = 32768;
		} else if (_writeLockWaitingCount < 2) {
			max = 16384;
		} else if (_writeLockWaitingCount < 6) {
			max = 8192;
		} else {
			max = 4096;
		}
		max = Math.min(requestSize, max);
		return max;
	}

	// used to control thread fairness
	private final int MaxWaitingThreads = 15;
	private Thread[] waitingThreads = new Thread[MaxWaitingThreads];
	private long headWaitingThreads;
	private long tailWaitingThreads;

	/**
	 * Acquires exclusive access to the shared output stream. Implements a fairness
	 * policy using a FIFO wait queue so that long or frequent writers do not starve
	 * other threads.
	 *
	 * <p>Behavior:</p>
	 * <ul>
	 *   <li>If the stream is closed, throws an IOException.</li>
	 *   <li>If no writer holds the lock and conditions permit, grants the lock.</li>
	 *   <li>Otherwise, waits and enters the waiting queue when appropriate.</li>
	 * </ul>
	 *
	 * @return the locked DataOutputStream
	 * @throws IOException if the controller has been closed
	 */
	private DataOutputStream getOutputStream() throws IOException {
		// long tsBegin = System.nanoTime(); // measurement
		boolean bIsInWaitingThreads = false;
		synchronized (WRITELOCK) {
			for (int i = 0;; i++) {
				if (_bIsClosed) {
					throw new IOException("real socket has been closed");
				}
				/*
				System.out.println("getOutputStream "+Thread.currentThread().getName()+", _bWritingLock="+_bWritingLock+", i="+
				i+", _writeLockWaitingCount="+_writeLockWaitingCount+
				", headWaitingThreads="+headWaitingThreads +
				", tailWaitingThreads="+tailWaitingThreads
				);//qqqqqq
				*/

				if (!_bWritingLock) {
					if (tailWaitingThreads == headWaitingThreads) {
						if (_writeLockWaitingCount == 0 || i > 0) {
							_bWritingLock = true;
							return _dataOutputStream;
						}
					} else if (waitingThreads[(int) (tailWaitingThreads % MaxWaitingThreads)] == Thread.currentThread()) {
						_bWritingLock = true;
						tailWaitingThreads++;
						return _dataOutputStream;
					}
				}

				try {
					_writeLockWaitingCount++;
					if (!bIsInWaitingThreads && i > 5) {
						if (headWaitingThreads - tailWaitingThreads < MaxWaitingThreads) {
							bIsInWaitingThreads = true;
							int pos = (int) (headWaitingThreads++ % MaxWaitingThreads);
							waitingThreads[pos] = Thread.currentThread();
						}
					}
					WRITELOCK.wait(150);
				} catch (InterruptedException e) {
				} finally {
					_writeLockWaitingCount--;
				}
			}
		}
	}

	/**
	 * Releases the write lock and wakes waiting threads. Flushes the output stream
	 * when no writers are waiting.
	 *
	 * @throws IOException if flushing fails
	 */
	private void releaseOutputStream() throws IOException {
		if (_bIsClosed) {
			return;
		}
		synchronized (WRITELOCK) {
			if (_bIsClosed) {
				return;
			}
			try {
				if (_writeLockWaitingCount == 0) {
					_dataOutputStream.flush();
				}
			} catch (IOException e) {
				onSocketException(e);
				throw (e);
			} finally {
				_bWritingLock = false;
				WRITELOCK.notifyAll();
			}
		}
	}

	/**
	 * Sends a ping command to the remote side. Useful for keep-alive behavior.
	 *
	 * @throws IOException if the stream is closed or the write fails
	 */
	public void sendPingCommand() throws IOException {
		sendCommand(MultiplexerSocketController.CMD_Ping, 0, null);
	}

	/**
	 * Hook invoked when socket-level write failures occur. Subclasses may override
	 * to implement custom error handling or reconnection logic.
	 *
	 * @param e exception encountered during write
	 */
	protected void onSocketException(Exception e) {
	}

	/**
	 * Sends a multiplexer command without an associated server-socket name.
	 *
	 * @param cmd command identifier
	 * @param param command parameter
	 * @throws IOException if writing fails
	 */
	protected void sendCommand(int cmd, int param) throws IOException {
		sendCommand(cmd, param, null);
	}

	/**
	 * Sends a command frame to the peer, encoding:
	 * <pre>
	 *   CMD_Command
	 *   cmd
	 *   param
	 *   [ optional serverSocketName length + bytes ]
	 * </pre>
	 *
	 * <p>Must match the format expected by
	 * {@link MultiplexerInputStreamController#readRealSocketLoop()}.</p>
	 *
	 * @param cmd command identifier
	 * @param param command parameter
	 * @param serverSocketName optional name of virtual server socket
	 * @throws IOException if the stream is closed or write fails
	 */
	protected void sendCommand(int cmd, int param, String serverSocketName) throws IOException {
		if (this._bIsClosed) {
			return;
		}
		getOutputStream();
		try {
			// this needs to match what is read by readRealSocket, which is Short + Integer + Integer.
			_dataOutputStream.writeInt(MultiplexerSocketController.CMD_Command);
			_dataOutputStream.writeInt(cmd);
			_dataOutputStream.writeInt(param);
			if (serverSocketName != null) {
				_dataOutputStream.writeInt(serverSocketName.length());
				_dataOutputStream.writeBytes(serverSocketName);
			}
		} finally {
			releaseOutputStream();
		}
	}
}
