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

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * Manages server-side multiplexed connections. A single real
 * {@link java.net.ServerSocket} is used to accept client connections,
 * and each accepted socket is wrapped by a {@link MultiplexerSocketController}
 * that can host many {@link VirtualSocket} channels.
 *
 * <p>
 * Responsibilities:
 * </p>
 *
 * <ul>
 *   <li>Accepting new client connections on the real ServerSocket.</li>
 *   <li>Creating a {@code MultiplexerSocketController} per client connection.</li>
 *   <li>Tracking all live controllers and aggregating read/write statistics.</li>
 *   <li>Managing named {@link VirtualServerSocket} instances and routing
 *       new virtual connections to their {@link java.net.ServerSocket#accept()}
 *       calls.</li>
 *   <li>Timing out connections that fail to complete the multiplexer handshake
 *       within a fixed period.</li>
 *   <li>Propagating throttle limits to each connection's output controller.</li>
 * </ul>
 *
 * <p>
 * Server code typically:
 * </p>
 *
 * <ol>
 *   <li>Creates a real {@link java.net.ServerSocket}.</li>
 *   <li>Calls {@link #start(ServerSocket)} to begin accepting connections.</li>
 *   <li>Uses {@link #getServerSocket(String)} to obtain named
 *       {@link VirtualServerSocket} instances.</li>
 *   <li>Calls {@code accept()} on the VirtualServerSocket to obtain
 *       {@link VirtualSocket} connections.</li>
 * </ol>
 *
 * <p>
 * Hook methods {@link #onClientConnect(Socket, int)} and
 * {@link #onClientDisconnect(int)} can be overridden to integrate logging,
 * session tracking, or custom connection policies.
 * </p>
 */
public class MultiplexerServerSocketController {
    private static Logger LOG = Logger.getLogger(MultiplexerServerSocketController.class.getName());

    /**
     * Lock object used to synchronize VirtualServerSocket.accept() calls with new
     * incoming VirtualSocket creations. Ensures that each VirtualSocket is delivered
     * to exactly one waiting accept() call.
     */
    private Object ACCEPTLOCK = new Object();

    /**
     * Lock used by the timeout thread to wait and signal when connection validation
     * activity occurs.
     */
    private final Object TIMEOUTLOCK = new Object();

    /**
     * Real ServerSocket used to accept physical TCP client connections.
     */
    private ServerSocket _serverSocket;

    /**
     * The next VirtualSocket to be returned by a VirtualServerSocket.accept() call.
     * Set when a client creates a VirtualSocket and the server routes it to the
     * appropriate VirtualServerSocket.
     */
    private volatile VirtualSocket _nextSocket;

    /**
     * Name of the VirtualServerSocket that should receive the next VirtualSocket
     * assigned to {@link #_nextSocket}.
     */
    private volatile String _nextServerSocketName;

    /**
     * Sequential counter used to assign unique connection IDs for newly accepted
     * client sockets. Server is implicitly connectionId=0; first client is 1.
     */
    private int _cntSocketController; // note: first connection needs to start at "1", since server is assumed to be "0"

    /**
     * Number of client connections that successfully completed the multiplexer
     * handshake and were validated as legitimate Multiplexer clients.
     */
    private int _totalValidSocketsCreated;

    /**
     * Background thread responsible for accepting new real client connections.
     */
    private Thread _threadAccept;

    /**
     * Background thread that monitors new connections for handshake timeout
     * (5-second limit).
     */
    private Thread _threadTimeout;

    /**
     * Indicates whether the controller and underlying server socket have been
     * closed. When true, accept loops and timeout loops exit.
     */
    private volatile boolean _bClosed;

    /**
     * List of all active MultiplexerSocketController instances. Each entry
     * represents one real client connection.
     */
    private ArrayList<MultiplexerSocketController> _alSocketController = new ArrayList<MultiplexerSocketController>();

    /**
     * Mapping of VirtualServerSocket names to VirtualServerSocket instances used
     * for routing VirtualSockets to their accept() callers.
     */
    private Hashtable<String, VirtualServerSocket> hashServerSocketName = new Hashtable<String, VirtualServerSocket>();

    /**
     * Message sent to clients that connect but fail to identify themselves as a
     * valid Multiplexer client.
     */
    private String _invalidConnectionMessage;

    /**
     * Creates a new controller for managing server-side multiplexed connections.
     * The real ServerSocket is assigned later via {@link #start(ServerSocket)}.
     */
    public MultiplexerServerSocketController() {
    }

    /**
     * Returns the message that is sent to invalid clients who fail the handshake.
     *
     * @return invalid-connection message
     */
    public String getInvalidConnectionMessage() {
        return _invalidConnectionMessage;
    }
    
    /**
     * Sets the message that will be sent to clients when they connect but do not
     * provide a valid multiplexer handshake.
     *
     * @param msg text to send before disconnecting invalid clients
     */
    public void setInvalidConnectionMessage(String msg) {
        LOG.fine("InvalidConnectionMessage=" + msg);
        this._invalidConnectionMessage = msg;
    }

    /**
     * Starts the controller using the provided real ServerSocket. Creates and
     * starts:
     * <ul>
     *   <li>The accept thread (MultiplexerServerSocket.Accept)</li>
     *   <li>The timeout thread (MultiplexerServerSocket.timeout)</li>
     * </ul>
     *
     * @param ss real ServerSocket used to accept client connections
     * @throws IllegalArgumentException if ss is null
     */
    public synchronized void start(ServerSocket ss) {
        if (ss == null) throw new IllegalArgumentException("ServerSocket can not be null");

        if (_threadAccept != null) return;
        this._serverSocket = ss;

        // create a thread to accept all new connections.
        _threadAccept = new Thread(new Runnable() {
            // @Override
            public void run() {
                MultiplexerServerSocketController.this.acceptConnections();
            }
        }, "MultiplexerServerSocket.Accept");
        _threadAccept.setDaemon(true);
        _threadAccept.start();

        // thread to timeout connections
        _threadTimeout = new Thread(new Runnable() {
            // @Override
            public void run() {
                MultiplexerServerSocketController.this.timeoutConnections();
            }
        }, "MultiplexerServerSocket.timeout");
        _threadTimeout.setDaemon(true);
        _threadTimeout.setPriority(Thread.MIN_PRIORITY);
        _threadTimeout.start();
    }

    /**
     * Loop executed by the accept thread. Accepts incoming physical client
     * connections and delegates them to {@link #onAcceptRealClientConnection(Socket)}.
     *
     * <p>Terminates when the controller is closed.</p>
     */
    private void acceptConnections() {
        for (int i = 0; !_bClosed; i++) {
            try {
                Socket socket = this._serverSocket.accept();
                if (_bClosed) continue;
                onAcceptRealClientConnection(socket);
            }
            catch (Exception e) {
                LOG.finer("MultiplexerServerSocketController: exception while accepting new connections, ex="+ e);
            }
        }
    }

    /**
     * Global throttle limit applied to write throughput for each connection's
     * MultiplexerOutputStreamController.
     */
    private int mbThrottleLimit;

    /**
     * Sets the number of megabytes per second allowed across all writes for each
     * connection. Applied to each connection's output stream controller.
     *
     * @param mbPerSecond new throttle limit
     */
    public void setThrottleLimit(int mbPerSecond) {
        LOG.config("new value="+mbPerSecond);
        mbThrottleLimit = mbPerSecond;
    }

    /**
     * Returns the current global write-throttle limit.
     *
     * @return throttle limit in MB/sec
     */
    public int getThrottleLimit() {
        return mbThrottleLimit;
    }
    
    /**
     * Handles a newly accepted real client socket. Creates a new
     * {@link MultiplexerSocketController} to manage the connection.
     *
     * <p>The created controller overrides key behavior:</p>
     * <ul>
     *   <li>Handshake verification (increments valid-socket counter and invokes
     *       {@link #onClientConnect(Socket, int)}).</li>
     *   <li>createSocket(...) to route VirtualSockets to the correct
     *       VirtualServerSocket.accept().</li>
     *   <li>close(...) to remove the controller and invoke
     *       {@link #onClientDisconnect(int)}.</li>
     *   <li>getInvalidConnectionMessage() to supply server-side messaging.</li>
     * </ul>
     *
     * @param socket newly accepted TCP socket
     * @throws IOException if controller creation fails
     */
    protected void onAcceptRealClientConnection(final Socket socket) throws IOException {
        _cntSocketController++;  

        final int connectionId = _cntSocketController;

        /**
         * This will create a new SocketController that will manage the new connection. Methods are
         * overwritten to so that new MultiplexerSocket connections can be given to the correct
         * MultiplexerServerSocket.
         */
        MultiplexerSocketController sc = new MultiplexerSocketController(socket, connectionId) {
            @Override
            protected boolean verifyServerSideHandshake() throws IOException {
                boolean b = super.verifyServerSideHandshake();
                if (b) {
                    _totalValidSocketsCreated++;
                    onClientConnect(socket, connectionId);
                }
                return b;
            }
            @Override
            protected VirtualSocket createSocket(int connectionId, int id, String serverSocketName) throws IOException {
                // The Multiplexer sockets that are created on the client need to be sent to the
                // vserversocket accept().
                VirtualServerSocket serverSocket = hashServerSocketName.get(serverSocketName);
                if (serverSocket == null) {
                    LOG.warning("serverSocket not found, socketName=" + serverSocketName);
                    return null; // invalid request
                }

                VirtualSocket vs = super.createSocket(connectionId, id, serverSocketName);

                synchronized (ACCEPTLOCK) {
                    for (;;) {
                        if (_nextSocket == null) {
                            _nextServerSocketName = serverSocketName;
                            _nextSocket = vs;
                            ACCEPTLOCK.notifyAll(); // will give to MultiplexerServerSocket that is
                                                    // waiting on accept()
                            return vs;
                        }
                        try {
                            ACCEPTLOCK.wait();
                        }
                        catch (Exception e) {
                        }
                    }
                }
            }

            @Override
            protected void close(boolean error) throws IOException {
                super.close(error);
                MultiplexerServerSocketController.this.remove(this);
            }

            @Override
            public String getInvalidConnectionMessage() {
                return MultiplexerServerSocketController.this.getInvalidConnectionMessage();
            }
        };
        sc.getOutputStreamController().setThrottleLimit(getThrottleLimit());
        
        // add the socketcontroller to list.
        add(sc);
    }

    /**
     * Loop executed by the timeout thread. Periodically checks all connections to
     * ensure that they complete their multiplexer handshake within 5 seconds.
     *
     * <p>Connections that fail validation within the timeout interval are closed.</p>
     */
    protected void timeoutConnections() {
        for (; !_bClosed;) {
            boolean bFound = _timeoutConnections();
            try {
                if (!bFound) {
                    synchronized (TIMEOUTLOCK) {
                        TIMEOUTLOCK.wait();
                    }
                }
                if (!_bClosed) {
                    Thread.sleep(5000);
                }
            }
            catch (Exception e) {
            }
        }
    }

    /**
     * Performs the actual timeout scanning for incomplete handshakes.
     *
     * @return true if at least one unvalidated connection still remains within
     *         the timeout window; false if none require waiting
     */
    private boolean _timeoutConnections() {
        MultiplexerSocketController[] scs = getSocketControllers();
        long msNow = System.currentTimeMillis();
        boolean bFound = false;
        for (MultiplexerSocketController sc : scs) {
            try {
                if (!sc.isValid()) {
                    long ms = sc.getStartTimeMS();
                    if (ms > 0 && ((msNow - ms) > 5000)) {
                        LOG.fine("MultiplexerServerSocketController: connection timeout, closing now, Id="+sc.getId());
                        sc.close(true);
                    }
                    else {
                        bFound = true;
                    }
                }
            }
            catch (Exception e) {
            }
        }
        return bFound;
    }

    /**
     * Adds a newly created MultiplexerSocketController to the active list and
     * signals the timeout thread.
     *
     * @param vsc controller to add
     */
    protected void add(MultiplexerSocketController vsc) {
        aiCreatedConnectionCnt.incrementAndGet();
        synchronized (_alSocketController) {
            _alSocketController.add(vsc);
        }
        synchronized (TIMEOUTLOCK) {
            TIMEOUTLOCK.notifyAll();
        }
    }

    
    /**
     * Removes a controller from the active list, aggregates its IO statistics,
     * and invokes {@link #onClientDisconnect(int)} if the connection was valid.
     *
     * @param vsc controller to remove
     */
    protected void remove(MultiplexerSocketController vsc) {
        if (vsc == null) return;
        
        aiRemovedReadCnt.addAndGet(vsc.getInputStreamController().getReadCount());
        aiRemovedReadSize.addAndGet(vsc.getInputStreamController().getReadSize());

        aiRemovedWriteCnt.addAndGet(vsc.getOutputStreamController().getWriteCount());
        aiRemovedWriteSize.addAndGet(vsc.getOutputStreamController().getWriteSize());
        
        boolean b;
        synchronized (_alSocketController) {
            b = _alSocketController.remove(vsc) && vsc.isValid();
        }
        if (b) {
            onClientDisconnect(vsc.getId());
        }
    }

    /**
     * Returns a snapshot array of all currently active MultiplexerSocketController
     * instances.
     *
     * @return array of active controllers
     */
    protected MultiplexerSocketController[] getSocketControllers() {
        MultiplexerSocketController[] vscs;
        synchronized (_alSocketController) {
            vscs = new MultiplexerSocketController[_alSocketController.size()];
            _alSocketController.toArray(vscs);
        }
        return vscs;
    }

    /**
     * Returns the VirtualServerSocket associated with the given name, creating one
     * if necessary. The returned VirtualServerSocket blocks in its accept() method
     * until a matching VirtualSocket is created by a client connection.
     *
     * <p>Routing logic:</p>
     * <ul>
     *   <li>Clients create VirtualSockets with a name</li>
     *   <li>The server maps that name to a VirtualServerSocket</li>
     *   <li>The VirtualSocket is delivered to a waiting accept() caller</li>
     * </ul>
     *
     * @param serverSocketName name of the logical server socket
     * @return VirtualServerSocket instance
     */
    public VirtualServerSocket getServerSocket(final String serverSocketName) throws IOException {
        if (serverSocketName == null || serverSocketName.length() == 0) return null;

        VirtualServerSocket serverSocket = hashServerSocketName.get(serverSocketName);
        if (serverSocket != null) {
            return serverSocket;
        }

        // create the server socket that will accept new client connections, through the multiplexed
        // connection.
        serverSocket = new VirtualServerSocket(serverSocketName) {
            @Override
            public Socket accept() throws IOException {
                synchronized (ACCEPTLOCK) {
                    for (;;) {
                        if (_nextSocket != null && _nextServerSocketName != null && _nextServerSocketName.equals(serverSocketName)) {
                            VirtualSocket sock = _nextSocket;
                            _nextSocket = null;
                            _nextServerSocketName = null;
                            ACCEPTLOCK.notifyAll();
                            return sock;
                        }
                        try {
                            // this will wait until a new client connection has been requested through a
                            // client multiplexed socket.
                            ACCEPTLOCK.wait();
                        }
                        catch (Exception e) {
                        }
                    }
                }
            }
        };
        hashServerSocketName.put(serverSocketName, serverSocket);

        return serverSocket;
    }

    /**
     * Closes all active connections and the real ServerSocket. After closure, the
     * accept and timeout threads will stop.
     *
     * @throws Exception if closing the ServerSocket fails
     */
    public void close() throws Exception {
        LOG.fine("closing all connections");
        _bClosed = true;
        MultiplexerSocketController[] vscs = getSocketControllers();
        for (MultiplexerSocketController vsc : vscs) {
            try {
                vsc.close();
            }
            catch (Exception e) {
            }
        }
        if (_serverSocket != null) _serverSocket.close();
    }

    /**
     * Called when a new client connection completes a valid multiplexer handshake.
     * Default implementation does nothing.
     *
     * @param socket underlying real socket
     * @param connectionId assigned connection id
     */
    public void onClientConnect(Socket socket, int connectionId) {
    }

    /**
     * Called when a client connection closes and is removed. Default implementation
     * does nothing.
     *
     * @param connectionId id of the disconnected client
     */
    public void onClientDisconnect(int connectionId) {
    }


    
    /**
     * Aggregated IO statistics for connections that have already been closed and
     * removed from the controller list.
     *
     * Values from closed connections are added here so that overall statistics can
     * be accurately reported even after individual controllers are gone.
     */
    private AtomicLong aiRemovedReadCnt = new AtomicLong();
    private AtomicLong aiRemovedReadSize = new AtomicLong();
    
    private AtomicLong aiRemovedWriteCnt = new AtomicLong();
    private AtomicLong aiRemovedWriteSize = new AtomicLong();
    
    /**
     * Counter tracking the total number of MultiplexerSocketControllers that were
     * successfully created and added.
     */
    private AtomicInteger aiCreatedConnectionCnt = new AtomicInteger();
    
    /**
     * Returns the total number of write operations performed across all current
     * and previously closed connections.
     *
     * @return total write count
     */
    public long getWriteCount() {
        long cnt = aiRemovedWriteCnt.get();
        for (MultiplexerSocketController sc : _alSocketController) {
            cnt += sc.getOutputStreamController().getWriteCount();
        }
        return cnt;
    }
    
    /**
     * Returns the total number of bytes written across all connections, including
     * closed ones.
     *
     * @return cumulative bytes written
     */
    public long getWriteSize() {
        long size = aiRemovedWriteSize.get();
        for (MultiplexerSocketController sc : _alSocketController) {
            size += sc.getOutputStreamController().getWriteSize();
        }
        return size;
    }

    /**
     * Returns the total number of read operations across all current and closed
     * connections.
     *
     * @return total read count
     */
    public long getReadCount() {
        long cnt = aiRemovedReadCnt.get();
        for (MultiplexerSocketController sc : _alSocketController) {
            cnt += sc.getInputStreamController().getReadCount();
        }
        return cnt;
    }

    /**
     * Returns the total number of bytes read across all connections.
     *
     * @return cumulative bytes read
     */
    public long getReadSize() {
        long size = aiRemovedReadSize.get();
        for (MultiplexerSocketController sc : _alSocketController) {
            size += sc.getInputStreamController().getReadSize();
        }
        return size;
    }

    /**
     * Returns the number of MultiplexerSocketController instances created since
     * startup.
     *
     * @return created-connection count
     */
    public int getCreatedConnectionCount() {
        return aiCreatedConnectionCnt.get();
    }
    
    /**
     * Returns the number of currently active client connections.
     *
     * @return count of live controllers
     */
    public int getLiveConnectionCount() {
        if (_alSocketController == null) return 0;
        return _alSocketController.size();
    }
}
