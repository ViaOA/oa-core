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
package com.viaoa.remote.multiplexer.io;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/*qqqqqqqqqqq
CODEX

1. file/class/method
     src/main/java/com/viaoa/remote/multiplexer/io/RemoteBufferedOutputStream.java
     close()
  2. concrete bug
     close() can discard buffered bytes instead of flushing them.
  3. runtime scenario
     RemoteBufferedOutputStream.write(...) buffers data in bsBuffer. If caller closes the stream without an explicit
     prior flush(), close() does:

  freeBuffer();
  super.close();

  freeBuffer() releases/nulls bsBuffer before FilterOutputStream.close() gets a chance to flush. The buffered bytes
  are lost.

  4. why this violates OA/OG remote semantics
     OA remote framing depends on every written byte reaching the virtual socket in order. Even though many current
     call sites explicitly call flush() before close(), this class violates the normal OutputStream.close() contract
     and can silently truncate a frame for any normal OA path that relies on close-to-flush behavior.
  5. minimal fix direction
     Call writeBuffer() or flush() before freeBuffer() in close(). Ensure the buffer is released in a finally so
     write/flush failures do not leak pooled buffers.
  6. suggested CODEX comment location
     At the start of RemoteBufferedOutputStream.close(), before freeBuffer().

1. file/class/method
     src/main/java/com/viaoa/remote/multiplexer/io/RemoteBufferedOutputStream.java
     flush()
  2. concrete bug
     A pooled buffer is not released if the underlying out.flush() throws.
  3. runtime scenario
     RemoteObjectOutputStream.flush() calls through to RemoteBufferedOutputStream.flush(). writeBuffer() writes
     buffered bytes, then out.flush() throws because the virtual socket/connection failed. Since freeBuffer() runs
     only after out.flush(), the pooled buffer remains marked in-use.
  4. why this violates OA/OG remote semantics
     Remote I/O failures are normal production paths during disconnect/reconnect. A failed remote flush should fail
     the frame/connection visibly, but it should not leak pooled buffer state. Repeated failed writes can drain the
     static buffer pool and permanently degrade the remoting layer.
  5. minimal fix direction
     Use try/finally around writeBuffer() / out.flush() so freeBuffer() always runs. The original IOException should
     still propagate.
  6. suggested CODEX comment location
     At RemoteBufferedOutputStream.flush(), around:

  writeBuffer();
  out.flush();
  freeBuffer();



*/

/**
 * High-performance buffered output stream used by OA's remoting and
 * multiplexer layers. Unlike {@link java.io.BufferedOutputStream}, this
 * implementation uses a shared pool of reusable byte[] buffers to avoid
 * per-stream allocation and reduce garbage collection pressure.
 *
 * <p>Characteristics:</p>
 * <ul>
 *   <li>Maintains a static pool of buffers of varying sizes for optimal throughput.</li>
 *   <li>Designed for single-threaded use—no synchronization overhead.</li>
 *   <li>Automatically releases its buffer back to the pool when flushed or closed.</li>
 *   <li>Falls back to a small private buffer if all pooled buffers are in use.</li>
 *   <li>Used internally by {@link RemoteObjectOutputStream} to improve remote
 *       serialization performance.</li>
 * </ul>
 *
 * <p>
 * This stream significantly outperforms standard buffering approaches in
 * tight remoting loops and dramatically reduces garbage creation under load.
 * </p>
 *
 * @author vvia
 */
public class RemoteBufferedOutputStream extends FilterOutputStream {
  
	/**
	 * Total number of pooled byte buffers available for reuse.
	 */
	private static final int TotalBuffers = 32;
    
	/**
	 * Base size, in bytes, used to create pooled buffers.
	 */
	private static final int BufferSize = 8 * 1024;
    
	/**
	 * Active byte buffer currently used to accumulate output data.
	 */
	protected byte[] bsBuffer;
    
	/**
	 * Number of bytes currently written into {@link #bsBuffer}.
	 */
	protected int count;
    
	/**
	 * Flag indicating whether the current buffer is privately owned
	 * and not part of the shared buffer pool.
	 */
	protected boolean bOwnedBuffer;  // true if the bsBuffer is not from the pool

	/**
	 * Creates a new buffered output stream that writes to the specified
	 * underlying output stream.
	 *
	 * @param out the underlying output stream
	 */
    public RemoteBufferedOutputStream(OutputStream out) {
        super(out);
    }

    /**
     * Tracks which pooled buffers are currently in use.
     */
    static boolean[] isUsed = new boolean[TotalBuffers];

    /**
     * Shared pool of reusable byte buffers.
     */
    static byte[][] buffers = new byte[TotalBuffers][];
    
    /**
     * Lock object used to synchronize access to the buffer pool.
     */
    static final Object Lock = new Object();

    /**
     * Retrieves an available buffer from the shared pool.
     * <p>
     * Marks the buffer as in use and lazily allocates it if needed.
     * </p>
     *
     * @return a pooled byte buffer, or {@code null} if none are available
     */
    protected static byte[] getPoolBuffer() {
        synchronized (Lock) {
            for (int i = 0; i < TotalBuffers; i++) {
                if (!isUsed[i]) {
                    isUsed[i] = true;
                    if (buffers[i] == null) {
                        int x = BufferSize;
                        if (i < 2) x *= 8;
                        else if (i < 4) x *= 4;
                        else if (i < 6) x *= 3;
                        else if (i < 8) x *= 2;
                        buffers[i] = new byte[ x ];
                    }
                    return buffers[i];
                }
            }
        }
        return null;
    }
    
    /**
     * Releases a pooled buffer back to the shared pool.
     *
     * @param bs the buffer to release
     */
    protected static void releasePoolBuffer(byte[] bs) {
        if (bs == null) return;
        synchronized (Lock) {
            for (int i = 0; i < TotalBuffers; i++) {
                if (buffers[i] == bs) {
                    isUsed[i] = false;
                    break;
                }
            }
        }
    }
    
    /**
     * Obtains a buffer for writing.
     * <p>
     * Attempts to retrieve a pooled buffer and falls back to a privately
     * owned buffer if none are available.
     * </p>
     *
     * @return a byte buffer for writing
     */
    protected byte[] getBuffer() {
        byte[] bs = getPoolBuffer();
        if (bs == null) {
            bs = new byte[2048]; // use a smaller size
            bOwnedBuffer = true;
        }
        return bs;
    }

    /**
     * Frees the current buffer.
     * <p>
     * Returns pooled buffers to the shared pool and clears the reference.
     * </p>
     */
    private void freeBuffer() {
        if (!bOwnedBuffer && bsBuffer != null) {
            releasePoolBuffer(bsBuffer);
            bsBuffer = null;
        }
    }
    
    /**
     * Closes this stream.
     * <p>
     * Frees any allocated buffer and then closes the underlying output stream.
     * </p>
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void close() throws IOException {
        freeBuffer();
        super.close();
    }

    /**
     * Ensures that any allocated buffer is released before garbage collection.
     *
     * @throws Throwable if an error occurs during finalization
     */
    @Override
    protected void finalize() throws Throwable {
        freeBuffer();
        super.finalize();
    }
    
    /**
     * Writes the contents of the internal buffer to the underlying output stream
     * and resets the buffer position.
     *
     * @throws IOException if an I/O error occurs
     */
    private void writeBuffer() throws IOException {
        if (count > 0 && bsBuffer != null) {
            out.write(bsBuffer, 0, count);
            count = 0;
        }
    }

    /**
     * Writes a single byte to this output stream.
     *
     * @param b the byte to write
     * @throws IOException if an I/O error occurs
     */
    public void write(int b) throws IOException {
        if (bsBuffer == null) {
            bsBuffer = getBuffer();
        }
        else if (count >= bsBuffer.length) {
            writeBuffer();
        }
        bsBuffer[count++] = (byte) b;
    }

    /**
     * Writes a portion of a byte array to this output stream.
     *
     * @param b the source byte array
     * @param off the start offset in the array
     * @param len the number of bytes to write
     * @throws IOException if an I/O error occurs
     */
    public void write(byte b[], int off, int len) throws IOException {
        if (bsBuffer == null) {
            bsBuffer = getBuffer();
        }
        if (len >= bsBuffer.length) {
            if (count > 0) {
                writeBuffer();
            }
            out.write(b, off, len);
            return;
        }
        if (len > bsBuffer.length - count) {
            writeBuffer();
        }
        System.arraycopy(b, off, bsBuffer, count, len);
        count += len;
    }

    /**
     * Flushes this output stream.
     * <p>
     * Writes any buffered data to the underlying stream, flushes it,
     * and releases the buffer back to the pool.
     * </p>
     *
     * @throws IOException if an I/O error occurs
     */
    public void flush() throws IOException {
        writeBuffer();
        out.flush();
        freeBuffer();  // good chance that it is not needed anymore
    }
}
