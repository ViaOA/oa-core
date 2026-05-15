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
package com.viaoa.comm.multiplexer;

import java.io.IOException;
import java.net.Socket;
import java.net.URI;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.viaoa.comm.multiplexer.io.VirtualSocket;
import com.viaoa.comm.multiplexer.io.MultiplexerSocketController;

/**
 * Client-side endpoint for the OA Multiplexer system.
 * <p>
 * <b>Purpose:</b> Allows a single real TCP connection to host multiple
 * independent {@link com.viaoa.comm.multiplexer.io.VirtualSocket} channels.
 * Each virtual socket behaves like an independent client connection, but all
 * traffic is multiplexed over one physical socket.
 * <p>
 * This enables:
 * <ul>
 *   <li>Many logical connections over a single firewall-friendly TCP socket</li>
 *   <li>Reduced connection overhead and simplified routing/load-balancing</li>
 *   <li>High-performance message delivery with optional throughput throttling</li>
 *   <li>Automatic keep-alive and connection health monitoring</li>
 * </ul>
 * <p>
 * <b>Lifecycle:</b>
 * <ol>
 *   <li>Construct using host/port or {@link java.net.URI}</li>
 *   <li>Call {@link #start()} to open the real socket and initialize the controller</li>
 *   <li>Create virtual sockets using {@link #createSocket(String)}</li>
 *   <li>Optionally configure keep-alive and throughput throttling</li>
 *   <li>Close using {@link #close()}</li>
 * </ol>
 * <p>
 * Subclasses may override:
 * <ul>
 *   <li>{@link #onSocketException(Exception)} – notification of underlying socket failures</li>
 *   <li>{@link #onClose(boolean)} – invoked when the controller closes the real socket</li>
 * </ul>
 * <p>
 * Thread-safe and designed for long-lived connections in distributed OA applications.
 */
public class OAMultiplexerClient {
    private static Logger LOG = Logger.getLogger(OAMultiplexerClient.class.getName());

    /**
     * Server port to which the real TCP socket will connect.
     */
    private int _port;

    /**
     * Hostname or IP address of the multiplexer server.
     */
    private String _host;

    /**
     * Internal flag indicating whether the real socket and controller have been
     * successfully created.
     */
    private volatile boolean _bCreated;

    /**
     * The underlying real TCP socket connected to the multiplexer server.
     */
    private Socket _socket;

    /**
     * Controller responsible for routing all reads and writes across the real
     * socket and managing all virtual socket instances.
     */
    private MultiplexerSocketController _controlSocket;

    /**
     * Maximum number of megabytes per second that may be written through the
     * multiplexer output stream. A value of zero disables throttling.
     */
    private int mbThrottleLimit;

    /**
     * Background thread that periodically sends keep-alive pings to the server to
     * prevent idle connection timeouts.
     */
    private volatile Thread keepAliveThread;

    /**
     * Interval, in seconds, used by the keep-alive thread to determine when to
     * send the next ping.
     */
    private int keepAliveSeconds;
    
    /**
     * Constructs a client using the host and port parsed from a URI.
     *
     * @param uri URI containing the multiplexer server host and port
     * @throws Exception if initialization fails
     */
    public OAMultiplexerClient(URI uri) throws Exception {
        this(uri.getHost(), uri.getPort());
        LOG.fine("uri=" + uri);
    }

    /**
     * Constructs a client configured with an explicit host and port.
     *
     * @param host server hostname or IP address
     * @param port port number to connect to
     */
    public OAMultiplexerClient(String host, int port) {
        LOG.fine("host=" + host + ", port=" + port);
        this._host = host;
        this._port = port;
    }

    /**
     * Establishes the real socket connection to the server and initializes the
     * multiplexer controller.
     *
     * <p>Behavior:</p>
     * <ul>
     *   <li>Creates the real TCP socket and enables TCP no-delay</li>
     *   <li>Initializes the {@link MultiplexerSocketController}</li>
     *   <li>Installs controller callbacks for socket exceptions and close events</li>
     *   <li>Applies any configured write-throughput limits</li>
     *   <li>Starts the keep-alive thread if enabled</li>
     * </ul>
     *
     * @throws Exception if the socket cannot be created or the controller fails to initialize
     */
    public void start() throws Exception {
        if (_bCreated) return;
        LOG.fine(String.format("creating real socket, host=%s, port=%d", _host, _port));

        _socket = new Socket(_host, _port);
        _socket.setTcpNoDelay(true);

        _controlSocket = new MultiplexerSocketController(_socket) {
            protected void onSocketException(Exception e) {
                if (_controlSocket != null && !_controlSocket.wasCloseAlreadyCalled()) {
                    OAMultiplexerClient.this.onSocketException(e);
                }
                try {
                    close(true);
                }
                catch (Exception e2) {
                }
            };
            @Override
            protected void close(boolean bError) throws IOException {
                _bCreated = false;
                if (!_controlSocket.wasCloseAlreadyCalled()) {
                    super.close(bError);
                    OAMultiplexerClient.this.onClose(bError);
                }
            }
        };
        setThrottleLimit(this.mbThrottleLimit);
        _controlSocket.start();
        runKeepAliveThread();        
        if (_controlSocket.isClosed()) throw new Exception("socket is closed");
        _bCreated = true;
    }

    /**
     * Callback invoked when the underlying real socket encounters an exception.
     * Subclasses may override to implement logging or reconnection logic.
     *
     * @param e exception raised from the real socket or controller
     */
    protected void onSocketException(Exception e) {
    }

    /**
     * Callback invoked when the controller closes the real socket, either
     * normally or due to an error.
     *
     * @param bError true if the socket closed because of an unexpected error
     */
    protected void onClose(boolean bError) {
    }
    
    /**
     * Enables or disables keep-alive behavior. When enabled, a background thread
     * periodically sends ping commands to the server to prevent the connection
     * from timing out.
     *
     * @param seconds delay between keep-alive pings; values less than 1 disable the feature
     */
    public void setKeepAlive(int seconds) {
        this.keepAliveSeconds = seconds;
        if (seconds < 1) return;
        if (keepAliveThread == null && _bCreated) {
            runKeepAliveThread();
        }
    }

    /**
     * Returns the keep-alive interval in seconds.
     *
     * @return number of seconds between pings
     */
    public int getKeepAlive() {
        return keepAliveSeconds;
    }
    
    /**
     * Starts the keep-alive thread if it is not already running. The thread
     * monitors the time since the last read and sends a ping if the connection
     * has been idle longer than {@code keepAliveSeconds}.
     */
    public void runKeepAliveThread() {
        if (keepAliveSeconds < 1) return;
        if (keepAliveThread != null) return;
        keepAliveThread = new Thread(new Runnable() {
            @Override
            public void run() {
                long msLast = 0;
                Thread threadHold = keepAliveThread;
                for (;;) {
                    try {
                        if (keepAliveSeconds < 1) break;
                        if (threadHold != keepAliveThread) break;

                        long msNow = System.currentTimeMillis();
                        if (_controlSocket != null) {
                            msLast = Math.max(msLast, _controlSocket.getInputStreamController().getLastReadTime());
                        }
                        if (msLast < 1) msLast = msNow;

                        long msWait = (keepAliveSeconds * 1000L) - (msNow - msLast);
                        if (msWait > 0) {
                            Thread.sleep(msWait);
                        }
                        else {
                            pingServer();
                            msLast = System.currentTimeMillis();
                        }
                    }
                    catch (Exception e) {
                        if (isConnected()) {
                            LOG.log(Level.WARNING, "", e);
                        }
                        break;
                    }
                }
                OAMultiplexerClient.this.keepAliveThread = null;
            }
        }, "MultiplexerClient.keepalive");
        keepAliveThread.setDaemon(true);
        keepAliveThread.start();
    }
    
    /**
     * Sends a ping command through the multiplexer output stream controller.
     *
     * @throws Exception if the controller cannot send the ping
     */
    public void pingServer() throws Exception {
        if (_controlSocket != null) {
            _controlSocket.getOutputStreamController().sendPingCommand();
        }
    }
    
    /**
     * Sets an upper limit on the number of megabytes per second that can be
     * written through the multiplexer output stream.
     *
     * @param mbPerSecond maximum MB/sec allowed; zero disables throttling
     */
    public void setThrottleLimit(int mbPerSecond) {
        mbThrottleLimit = mbPerSecond;
        if (_controlSocket != null) {
            _controlSocket.getOutputStreamController().setThrottleLimit(mbThrottleLimit);
        }
    }

    /**
     * Returns the current write-throughput throttle limit in MB/sec.
     *
     * @return throttle limit
     */
    public int getThrottleLimit() {
        if (_controlSocket != null) {
            mbThrottleLimit = _controlSocket.getOutputStreamController().getThrottleLimit();
        }
        return mbThrottleLimit;
    }

    /**
     * Creates a client-side virtual socket bound to the named virtual server
     * socket on the multiplexer server.
     *
     * @param serverSocketName name assigned on the server when the virtual server socket was created
     * @return new VirtualSocket instance
     * @throws IOException if the virtual socket cannot be created
     */
    public VirtualSocket createSocket(String serverSocketName) throws IOException {
        LOG.fine("creating new socket, name=" + serverSocketName);
        VirtualSocket vs = _controlSocket.createSocket(serverSocketName);
        aiCreateSocketCnt.incrementAndGet();
        return vs;
    }

    private AtomicInteger aiCreateSocketCnt = new AtomicInteger();

    /**
     * Returns the cumulative number of virtual sockets created by this client.
     *
     * @return number of created virtual sockets
     */
    public int getCreatedSocketCount() {
        return aiCreateSocketCnt.get();
    }
    
    /**
     * Returns the number of virtual sockets that are currently active.
     *
     * @return live virtual socket count
     */
    public int getLiveSocketCount() {
        return _controlSocket.getLiveSocketCount();
    }
    
    /**
     * Closes the real socket and shuts down the multiplexer controller. Virtual
     * sockets are also terminated.
     *
     * @throws IOException if controller shutdown encounters an error
     */
    public void close() throws IOException {
        LOG.fine("closing real socket");
        _bCreated = false;
        if (_controlSocket != null) {
            _controlSocket.close();
        }
    }


    /**
     * Returns the unique connection identifier assigned by the server to this
     * client session.
     *
     * @return connection id, or -1 if not yet connected
     */
    public int getConnectionId() {
        if (_controlSocket == null) return -1;
        return _controlSocket.getId();
    }
    
    public boolean isConnected() {
        if (_controlSocket == null) return false;
        try {
            return !_controlSocket.isClosed();
        }
        catch (Exception e) {
        }
        return false;
    }

    /**
     * Returns the underlying real TCP socket used by the multiplexer client.
     *
     * @return real socket instance
     */
    public Socket getSocket() {
        return _socket;
    }

    /**
     * Returns the server port configured for the real socket connection.
     *
     * @return port number
     */
    public int getPort() {
        return _port;
    }

    /**
     * Returns the hostname or IP address of the multiplexer server.
     *
     * @return host string
     */
    public String getHost() {
        return _host;
    }
    
    /**
     * Returns the number of write operations performed through the multiplexer
     * output stream controller.
     *
     * @return write count
     */
    public long getWriteCount() {
        if (_controlSocket == null) return 0;
        return _controlSocket.getOutputStreamController().getWriteCount(); 
    }

    /**
     * Returns the total number of bytes written through the multiplexer output
     * stream controller.
     *
     * @return number of bytes written
     */
    public long getWriteSize() {
        if (_controlSocket == null) return 0;
        return _controlSocket.getOutputStreamController().getWriteSize(); 
    }
    
    /**
     * Returns the number of read operations performed by the multiplexer input
     * stream controller.
     *
     * @return read count
     */
    public long getReadCount() {
        if (_controlSocket == null) return 0;
        return _controlSocket.getInputStreamController().getReadCount(); 
    }

    /**
     * Returns the total number of bytes read by the multiplexer input stream
     * controller.
     *
     * @return number of bytes read
     */
    public long getReadSize() {
        if (_controlSocket == null) return 0;
        return _controlSocket.getInputStreamController().getReadSize(); 
    }
    
}
