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

import java.io.DataInputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.viaoa.object.OAObject;
import com.viaoa.runtime.OARuntime;

/**
 * Manages the shared {@link java.io.DataInputStream} for a single physical
 * TCP connection used by the multiplexer. All incoming data for every
 * {@link VirtualSocket} channel is read by this controller and then
 * coordinated so that the correct virtual socket can consume its payload.
 *
 * <p>
 * The controller runs a dedicated read loop that:
 * </p>
 *
 * <ul>
 *   <li>Reads a header containing the virtual socket id and payload length.</li>
 *   <li>Dispatches command frames to {@link #processCommand(int, int)}.</li>
 *   <li>Assigns data frames to the appropriate {@link VirtualSocket} by
 *       setting {@code _nextReadId} and waking the waiting thread.</li>
 *   <li>Enforces per-socket timeouts and connection-level error detection.</li>
 * </ul>
 *
 * <p>
 * Individual virtual sockets call {@link #read(VirtualSocket, byte[], int, int)}
 * to consume their assigned payload. The controller guarantees that only the
 * virtual socket identified by the current header can read from the underlying
 * stream, and that the full frame length is honored before the next header is
 * processed.
 * </p>
 *
 * <p>
 * Subclasses (typically {@code MultiplexerSocketController}) implement the
 * abstract methods to:
 * </p>
 *
 * <ul>
 *   <li>Create and close virtual sockets.</li>
 *   <li>Close the real socket when commanded.</li>
 *   <li>Resolve virtual socket ids and validate maximum ids.</li>
 * </ul>
 *
 * <p>
 * Read statistics (count and total size) are tracked for monitoring and
 * performance diagnostics.
 * </p>
 */
public abstract class MultiplexerInputStreamController {
    private static Logger LOG = Logger.getLogger(MultiplexerInputStreamController.class.getName());

    /**
     * Identifier shared by all VirtualSockets belonging to this real connection.
     */
    private int _connectionId;

    /**
     * Underlying input stream for the physical socket. All VirtualSockets read
     * through this shared stream according to routing rules.
     */
    private DataInputStream _dataInputStream;

    /**
     * Flag indicating whether the input side of the real socket has been closed.
     * When true, all waiting VirtualSockets are released.
     */
    private boolean _bIsClosed;
    
    /**
     * Timestamp (ms) of the last successful read operation on the real socket.
     */
    private long msLastRead;

    /**
     * Maximum time (in seconds) a VirtualSocket is allowed to block while waiting
     * for its turn to read data after a header is assigned.
     */
    private final int _timeoutSeconds = 20;
    
    /**
     * Lock used to synchronize state between the real-socket read thread and
     * VirtualSockets that are consuming their assigned payload.
     *
     * This ensures that only one VirtualSocket can read at a time.
     */
    private final transient Object READLOCK = new Object();

    /**
     * Indicates that the real-socket thread is currently waiting for the
     * VirtualSocket to finish reading its assigned payload.
     */
    private volatile boolean _bRealReaderIsWaiting;

    /**
     * VirtualSocket ID that is next allowed to read from the real socket.
     * A value of 0 indicates a command frame; negative means "no active reader."
     */
    private volatile int _nextReadId = -1; // from the header, < 0 means that it is not assigned, 0 is
                                           // for commands.
    /**
     * The total number of bytes assigned to the VirtualSocket for the current
     * data frame.
     */
    private volatile int _nextReadLen; // from the header, length of data for the next reader to read.

    /**
     * Number of bytes already read by the VirtualSocket for the current frame.
     * Ensures that partial reads are tracked correctly.
     */
    private volatile int _nextReadOffset; // used when reading n bytes at a time. This keeps track of
                                          // the amount of bytes from _nextRadLen that have been read.

    /**
     * Metrics tracking the cumulative byte count and number of read() operations
     * performed across all VirtualSockets using this controller.
     */
    private AtomicLong aiReadSize = new AtomicLong();
    private AtomicLong aiReadCnt = new AtomicLong();
    
    /**
     * Creates a controller for the specified connection.
     *
     * @param connectionId unique id assigned to the real socket connection
     */
    MultiplexerInputStreamController(int connectionId) {
        this._connectionId = connectionId;
    }

    /**
     * Assigns the shared real-socket input stream used by all VirtualSockets.
     *
     * @param dataInputStream the underlying DataInputStream for the connection
     */
    void setDataInputStream(DataInputStream dataInputStream) {
        this._dataInputStream = dataInputStream;
    }

    /**
     * Marks the controller as closed and wakes all VirtualSockets waiting on
     * READLOCK or their own lock objects. No further reads are permitted.
     *
     * @throws IOException never thrown here; signature reserved for subclasses
     */
    protected void close() throws IOException {
        synchronized (READLOCK) {
            this._bIsClosed = true;
            READLOCK.notifyAll();
        }
    }

    /**
     * Returns the timestamp of the most recent read from the real socket.
     *
     * @return last-read time in milliseconds
     */
    public long getLastReadTime() {
        return msLastRead;
    }
    
    /**
     * Main loop run by the MultiplexerSocketController’s dedicated thread.
     * Continuously reads:
     *
     * <ol>
     *   <li>A VirtualSocket id</li>
     *   <li>The payload length</li>
     * </ol>
     *
     * <p>If the id is {@code CMD_Command}, the frame is processed as a command
     * via {@link #processCommand(int, int)}.</p>
     *
     * <p>Otherwise, the controller:</p>
     * <ul>
     *   <li>Retrieves the target VirtualSocket</li>
     *   <li>Checks for corruption or closed sockets</li>
     *   <li>Assigns the frame to that socket</li>
     *   <li>Blocks until that VirtualSocket finishes reading its payload</li>
     * </ul>
     *
     * <p>Loop exits when the connection is closed or an unrecoverable error occurs.</p>
     *
     * @throws Exception if the stream is corrupted or a fatal IO error occurs
     */
    void readRealSocketLoop() throws Exception {
        long msLastStackDump = 0;
        for (int cntx = 0; !_bIsClosed; cntx++) {
            int readId = _dataInputStream.readInt(); // socket.id or 0 for command
            
            msLastRead = System.currentTimeMillis();
            _nextReadLen = _dataInputStream.readInt(); // the length of data for the vsocket to read.

            if (readId == MultiplexerSocketController.CMD_Command) {
                // internal command
                // this needs to match what was sent by sendCommand, which is Short + Integer + Integer.
                int param = _dataInputStream.readInt();
                processCommand(_nextReadLen, param);
                continue;
            }

            VirtualSocket vs = getSocket(readId);

            // check for errors, should only happen if socket is disconnected, or data is corrupted.
            boolean bError;
            if (vs == null) {
                if (readId > getMaxSocketId()) {
                    bError = true;
                }
                else {
                    // consume the message, since the socket is closed
                    skipFully(_nextReadLen);
                    continue;
                }
            }
            else if (vs.isClosed()) {
                skipFully(_nextReadLen);
                continue;
            }
            else if (_nextReadLen < 0) bError = true;
            else bError = false;

            if (bError) {
                String s = "MultiplexerInputStreamController: Socket stream is corrupted, received id=" + readId + ", length=" + _nextReadLen + ", vsc.id=" + readId + ". ";
                if (vs == null) s += "VirtualSocket not in list, maxSocketId=" + getMaxSocketId() + ". ";
                s += "Real socket will be closed.";
                LOG.log(Level.WARNING, s);
                throw new Exception(s);
            }

            _nextReadOffset = 0;

            synchronized (vs._lockObject) {
                if (vs.isClosed()) {
                    skipFully(_nextReadLen);
                    continue;
                }
                _nextReadId = readId; // this is the VS.id that needs to read the data after the header
                vs._lockObject.notifyAll();
            }

            synchronized (READLOCK) { // wait for VSocket (vsId=nextReadId) to perform read.
                for (int cnt = 0; (_nextReadId >= 0) && !_bIsClosed; cnt++) {
                    _bRealReaderIsWaiting = true;
                    try {
                        READLOCK.wait(250);
                        if (_nextReadId >= 0) {
                            if (vs.isClosed()) {
                                synchronized (vs._lockObject) {
                                    if (_nextReadId >= 0) {
                                        skipFully(_nextReadLen - _nextReadOffset);
                                        _nextReadId = -1;
                                        _nextReadOffset = _nextReadLen;
                                    }
                                }
                            }
                            else if (cnt == (_timeoutSeconds * 4)) {
                                if (!OAObject.getDebugMode()) {
                                    LOG.warning("Connection="+_connectionId+", VSocket id=" + vs._id + ", name=" + vs.getServerSocketName() + ", has been timed out, will disconnect socket and continue");
                                    long ms = System.currentTimeMillis();
                                    if (msLastStackDump + 30000 < ms) {
                                        LOG.warning(OARuntime.thread().getAllStackTraces());
                                        msLastStackDump = ms;
                                    }
                                    synchronized (vs._lockObject) {
                                        vs.close(); // this will notify the thread
                                    }
                                }
                            }
                        }
                    }
                    catch (InterruptedException e) {
                        // dont rethrow, since this is internal to real socket and would cause corrupted
                        // stream.
                    }
                    finally {
                        _bRealReaderIsWaiting = false;
                    }
                }
            }
        }
        LOG.fine("MultiplexerInputStreamController: socket has been closed, (leaving readRealSocket loop)");
    }

    /**
     * Returns the total number of read() operations performed across all
     * VirtualSockets.
     *
     * @return read count
     */
    public long getReadCount() {
        return aiReadCnt.get();
    }

    /**
     * Returns the cumulative number of bytes read from the real socket.
     *
     * @return total bytes read
     */
    public long getReadSize() {
        return aiReadSize.get();
    }
    
    /**
     * Called by VirtualSockets to read their portion of the real-stream payload.
     * Delegates to {@link #_read(VirtualSocket, byte[], int, int)} and ensures
     * the controller is released afterward.
     *
     * @return number of bytes read
     * @throws IOException if the real socket has been closed or a timeout occurs
     */
    int read(VirtualSocket vs, byte[] bs, int off, int len) throws IOException {
        int x;
        try {
            x = _read(vs, bs, off, len);
        }
        finally {
            _releaseInputStream(vs);
        }
        
        aiReadCnt.incrementAndGet();
        aiReadSize.addAndGet(x);
        
        return x;
    }

    /**
     * Performs the actual blocking read for the given VirtualSocket.
     *
     * <p>Behavior:</p>
     * <ul>
     *   <li>Wait until the controller assigns this VirtualSocket as the active reader</li>
     *   <li>Read up to the remaining frame length</li>
     *   <li>Continue waiting or throw timeout/closed errors as appropriate</li>
     * </ul>
     *
     * @return number of bytes read
     * @throws IOException if closed or timed out
     */
    private int _read(VirtualSocket vs, byte[] bs, int off, int len) throws IOException {
        int readAmt = 0;

        int timeoutSeconds = vs.getTimeoutSeconds();
        for (int i=0; ;i++) {
            synchronized (vs._lockObject) {
                if (vs._id == _nextReadId) {
                    readAmt = _dataInputStream.read(bs, off, Math.min(len, (_nextReadLen - _nextReadOffset)));
                    _nextReadOffset += readAmt;
                    return readAmt;
                }
                try {
                    if (this._bIsClosed || vs.isClosed()) {
                        throw new IOException("socket has been closed");
                    }
                    if (i > 0 && timeoutSeconds > 0 && i > timeoutSeconds) {
                        throw new IOException("timeout wating to read, timeout="+timeoutSeconds);
                    }
                    vs._lockObject.wait(1000);
                }
                catch (InterruptedException e) {
                    // ignore
                }
            }
        }
    }

    /**
     * Consumes and discards a frame intended for a VirtualSocket that has been
     * closed. Ensures stream alignment is preserved.
     *
     * @param skipAmount number of bytes to skip
     * @throws IOException if EOF occurs
     */
    private void skipFully(int skipAmount) throws IOException {
        for (; skipAmount > 0;) {
            long x = _dataInputStream.skip(skipAmount);
            if (x < 0) break; // EOF
            skipAmount -= x;
        }
    }

    /**
     * Releases the controller after a VirtualSocket finishes reading its frame.
     * Signals the real-socket thread that it may proceed to the next header.
     *
     * @param vs the VirtualSocket that finished reading
     */
    private void _releaseInputStream(VirtualSocket vs) {
        synchronized (READLOCK) {
            if ((_nextReadOffset == _nextReadLen) && (vs._id == _nextReadId)) // done reading full
                                                                              // amount from real
                                                                              // socket.
            {
                _nextReadId = -1; // this is used so that the real socket reader will "know" that the
                                  // vsocket is done reading.
                if (_bRealReaderIsWaiting) {
                    READLOCK.notifyAll();
                }
            }
            // else: will need to do another read for the rest of the data from real socket.
        }
    }

    /**
     * Processes multiplexed command frames sent by the remote controller.
     *
     * <p>Commands include:</p>
     * <ul>
     *   <li>Create new VirtualSocket</li>
     *   <li>Close VirtualSocket</li>
     *   <li>Close real socket</li>
     *   <li>Ping</li>
     * </ul>
     *
     * <p>Subcommands that require reading additional payload bytes must match the
     * encoding used by {@code MultiplexerOutputStreamController#sendCommand}.</p>
     *
     * @param cmd command identifier
     * @param param optional integer parameter
     * @throws Exception if socket creation/closure fails
     */
    protected void processCommand(int cmd, int param) throws Exception {
        switch (cmd) {
        case MultiplexerSocketController.CMD_CreateVSocket:
            String serverSocketName;
            int len = _dataInputStream.readInt();
            byte[] bs = new byte[len];
            _dataInputStream.readFully(bs);
            serverSocketName = new String(bs);
            createNewSocket(_connectionId, param, serverSocketName);
            break;
        case MultiplexerSocketController.CMD_CloseVSocket:
            closeSocket(param, false);
            break;
        case MultiplexerSocketController.CMD_CloseRealSocket:
            closeRealSocket();
            break;
        case MultiplexerSocketController.CMD_Ping:
            break;
        }
    }

    /**
     * Abstract methods that are implemented by MultiplexerSocketController.
     */

    /*
     * These methods are needed to be supplied by the user of this object. Currently used/created by
     * MultiplexerSocketController
     */

    /**
     * Creates a new VirtualSocket when instructed by a command frame.
     *
     * @param connectionId id of the real connection
     * @param id virtual socket id
     * @param serverSocketName logical server-socket binding
     */
    protected abstract void createNewSocket(int connectionId, int id, String serverSocketName);

    /**
     * Closes an existing VirtualSocket identified by id.
     *
     * @param id virtual socket id
     * @param bSendCommand whether the remote side should be notified
     */
    protected abstract void closeSocket(int id, boolean bSendCommand);

    /**
     * Closes the underlying real socket in response to a CMD_CloseRealSocket command.
     */
    protected abstract void closeRealSocket();

    /**
     * Returns the VirtualSocket for the given id or null if none exists.
     *
     * @param id virtual socket id
     * @return VirtualSocket or null
     */
    protected abstract VirtualSocket getSocket(int id);

    /**
     * Returns the maximum valid VirtualSocket id. Used to detect corrupted frames.
     *
     * @return highest allowed id
     */
    protected abstract int getMaxSocketId();
}
