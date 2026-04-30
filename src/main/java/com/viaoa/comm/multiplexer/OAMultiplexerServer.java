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
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Logger;

import com.viaoa.comm.multiplexer.io.VirtualServerSocket;
import com.viaoa.comm.multiplexer.io.MultiplexerServerSocketController;

/**
 * Server-side endpoint for the OA Multiplexer system.
 * <p>
 * <b>Purpose:</b> Hosts a single real {@link java.net.ServerSocket} and exposes
 * multiple logical {@link com.viaoa.comm.multiplexer.io.VirtualServerSocket}
 * instances. Client-side {@code OAMultiplexerClient} objects can create matching
 * virtual sockets and communicate as if each were an independent TCP connection.
 * <p>
 * This allows:
 * <ul>
 *   <li>Many logical client connections over one physical port</li>
 *   <li>Firewall-friendly architectures with reduced socket overhead</li>
 *   <li>Centralized connection management and routing</li>
 *   <li>Optional throughput throttling</li>
 * </ul>
 *
 * <b>Lifecycle:</b>
 * <ol>
 *   <li>Construct with host/port</li>
 *   <li>Call {@link #start()} to begin accepting real client connections</li>
 *   <li>Create virtual server sockets via {@link #createServerSocket(String)}</li>
 *   <li>Handle client connect/disconnect through overridable hooks</li>
 *   <li>Stop using {@link #stop()} or {@link #stopServerSocket()}</li>
 * </ol>
 * <p>
 * <b>Extensibility:</b>
 * Override:
 * <ul>
 *   <li>{@link #onClientConnect(Socket, int)}</li>
 *   <li>{@link #onClientDisconnect(int)}</li>
 * </ul>
 * to integrate logging, session tracking, or custom connection policies.
 */
public class OAMultiplexerServer {
    private static Logger LOG = Logger.getLogger(OAMultiplexerServer.class.getName());

    /**
     * Network port on which the real ServerSocket listens for physical
     * client connections.
     */
    private int _port;

    /**
     * Hostname or IP address used when binding or reporting the server's identity.
     */
    private String _host;

    /**
     * Internal flag indicating whether the server is currently accepting new
     * real socket connections.
     */
    private boolean _bAllowConnections;

    
    /**
     * The single real ServerSocket that accepts client connections on behalf of
     * all virtual server sockets.
     */
    private ServerSocket _serverSocket;

    /**
     * Controller responsible for managing the real ServerSocket, routing new
     * connections, maintaining virtual socket mappings, and tracking metrics.
     */
    private MultiplexerServerSocketController _controlServerSocket;

    /**
     * Message sent to clients when an incoming connection is not a valid
     * multiplexer connection.
     */
    private String _invalidConnectionMessage;

    /**
     * Constructs a multiplexer server bound to the given host and port. If the
     * host is null, the local machine's IP address is used.
     *
     * @param host hostname or IP address to associate with this server
     * @param port port on which the real ServerSocket will listen
     */
    public OAMultiplexerServer(String host, int port) {
        try {
            if (host == null) host = InetAddress.getLocalHost().getHostAddress();
        }
        catch (Exception e) {
        }
        this._host = host;
        this._port = port;
        LOG.fine("host=" + host + ", port=" + port);
    }

    /**
     * Convenience constructor that binds the server to the local host using the
     * provided port.
     *
     * @param port listening port for the real ServerSocket
     */
    public OAMultiplexerServer(int port) {
        this(null, port);
    }

    /**
     * Sets an upper bound on the number of megabytes per second that can be
     * written across all multiplexer connections.
     *
     * @param mbPerSecond write-throughput limit in MB/sec
     * @throws Exception if the server socket controller cannot apply the limit
     */
    public void setThrottleLimit(int mbPerSecond) throws Exception {
        getServerSocketController().setThrottleLimit(mbPerSecond);
    }

    /**
     * Returns the currently configured throughput limit in MB/sec.
     *
     * @return throttle limit in MB/sec
     * @throws Exception if the controller is not initialized
     */
    public int getThrottleLimit() throws Exception {
        return getServerSocketController().getThrottleLimit();
    }
    
    /**
     * Starts the multiplexer server. Creates the real ServerSocket, initializes
     * the controller, and begins accepting new client connections.
     *
     * @throws Exception if socket creation or controller startup fails
     */
    public void start() throws Exception {
        if (_bAllowConnections) return;
        LOG.fine("starting");
        _bAllowConnections = true;

        // create the real ServerSocket
        _serverSocket = new ServerSocket(this._port);

        getServerSocketController().start(_serverSocket);
        LOG.fine("start completed");
    }

    /**
     * Stops the multiplexer and closes the controller. All virtual and real socket
     * processing is terminated.
     *
     * @throws Exception if the controller cannot be closed
     */
    public void stop() throws Exception {
        getServerSocketController().close();
        _bAllowConnections = false;
    }
    
    /**
     * Stops accepting new real client connections while keeping all existing
     * connections active. The real ServerSocket is closed but the controller
     * remains operational.
     *
     * @throws Exception if the underlying ServerSocket cannot be closed
     */
    public void stopServerSocket() throws Exception {
        if (!_bAllowConnections || _serverSocket == null) return;
        LOG.fine("stopping");
        _bAllowConnections = false;
        _serverSocket.close();
    }

    /**
     * Indicates whether the multiplexer server is actively accepting new
     * connections.
     *
     * @return true if accepting new connections, false otherwise
     */
    public boolean isStarted() {
        return this._bAllowConnections;
    }

    /**
     * Creates (or retrieves) a virtual server socket registered under the given
     * name. Clients must use the same name when creating matching virtual sockets.
     *
     * @param serverSocketName unique identifier for the virtual server socket
     * @return virtual server socket instance
     * @throws IOException if creation fails or the controller reports an error
     */
    public VirtualServerSocket createServerSocket(String serverSocketName) throws IOException {
        LOG.fine("serverSocketName=" + serverSocketName);
        VirtualServerSocket ss;
        try {
            ss = getServerSocketController().getServerSocket(serverSocketName);
        }
        catch (Exception e) {
            throw new IOException("Exception while creating server socket.", e);
        }
        return ss;
    }

    /**
     * Lazily creates and returns the server socket controller that manages all
     * real and virtual socket operations. The controller delegates connection
     * events back to this server for override handling.
     *
     * @return initialized controller instance
     * @throws Exception if initialization fails
     */
    private MultiplexerServerSocketController getServerSocketController() throws Exception {
        if (_controlServerSocket == null) {
            LOG.fine("creating single serverSocket controller, to manage all real socket connections");
            // create controller for server sockets
            _controlServerSocket = new MultiplexerServerSocketController() {
                @Override
                public String getInvalidConnectionMessage() {
                    return OAMultiplexerServer.this.getInvalidConnectionMessage();
                }

                @Override
                public void onClientDisconnect(int connectionId) {
                    OAMultiplexerServer.this.onClientDisconnect(connectionId);
                }

                @Override
                public void onClientConnect(Socket socket, int connectionId) {
                    OAMultiplexerServer.this.onClientConnect(socket, connectionId);
                }
            };
        }
        return _controlServerSocket;
    }

    /**
     * Returns the listening port of the real ServerSocket.
     *
     * @return port number
     */
    public int getPort() {
        return _port;
    }

    /**
     * Returns the hostname or IP address associated with the server.
     *
     * @return host string
     */
    public String getHost() {
        return _host;
    }

    /**
     * Defines the message sent to clients when a non-multiplexer connection
     * attempts to connect.
     *
     * @param msg descriptive message
     */
    public void setInvalidConnectionMessage(String msg) {
        LOG.fine("InvalidConnectionMessage=" + msg);
        this._invalidConnectionMessage = msg;
    }

    /**
     * Returns the invalid-connection message configured for this server.
     *
     * @return message text or null
     */
    public String getInvalidConnectionMessage() {
        return _invalidConnectionMessage;
    }

    /**
     * Callback invoked when a real client connection is disconnected. Subclasses
     * may override to perform cleanup, logging, or auditing.
     *
     * @param connectionId unique connection identifier assigned by the controller
     */
    protected void onClientDisconnect(int connectionId) {
        LOG.fine("connectionId=" + connectionId);
    }

    /**
     * Callback invoked when a new real client connection is established.
     * Subclasses may override to implement authentication, logging, or session
     * initialization.
     *
     * @param socket the underlying real socket
     * @param connectionId controller-assigned connection identifier
     */
    protected void onClientConnect(Socket socket, int connectionId) {
        LOG.fine("connectionId=" + connectionId);
    }

    /**
     * Returns the total number of read operations performed across all
     * multiplexer connections.
     *
     * @return read count, or 0 if the controller is not initialized
     */
    public long getReadCount() {
        if (_controlServerSocket == null) return 0;
        return _controlServerSocket.getReadCount(); 
    }

    /**
     * Returns the cumulative number of bytes read across all connections.
     *
     * @return number of bytes read
     */
    public long getReadSize() {
        if (_controlServerSocket == null) return 0;
        return _controlServerSocket.getReadSize(); 
    }

    /**
     * Returns the total number of write operations performed.
     *
     * @return write count
     */
    public long getWriteCount() {
        if (_controlServerSocket == null) return 0;
        return _controlServerSocket.getWriteCount(); 
    }

    /**
     * Returns the cumulative number of bytes written across all connections.
     *
     * @return number of bytes written
     */
    public long getWriteSize() {
        if (_controlServerSocket == null) return 0;
        return _controlServerSocket.getWriteSize(); 
    }

    /**
     * Returns the number of real client connections that have been created since
     * server startup.
     *
     * @return count of created connections
     */
    public int getCreatedConnectionCount() {
        if (_controlServerSocket == null) return 0;
        return _controlServerSocket.getCreatedConnectionCount();
    }

    /**
     * Returns the number of active real connections currently managed by the
     * controller.
     *
     * @return number of live connections
     */
    public int getLiveConnectionCount() {
        if (_controlServerSocket == null) return 0;
        return _controlServerSocket.getLiveConnectionCount();
    }
}
