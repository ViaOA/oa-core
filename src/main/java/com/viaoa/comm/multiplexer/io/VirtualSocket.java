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

import java.net.*;
import java.io.*;

/**
 * A logical socket connection that is multiplexed over a single physical TCP
 * connection. All read and write operations on a VirtualSocket are delegated
 * through the owning {@link MultiplexerSocketController}, allowing many
 * independent channels to share the same underlying real socket.
 *
 * Each VirtualSocket behaves like a normal {@link Socket}: it exposes
 * input/output streams, supports blocking reads, and maintains independent
 * close and timeout behavior. The multiplexer assigns each virtual channel
 * a unique id and routes all data frames accordingly.
 */
public abstract class VirtualSocket extends Socket {

	/**
	 * Identifier of the real TCP connection hosting this VirtualSocket. Assigned
	 * by the server during handshake.
	 */
    protected final int _connectionId;

    /**
     * Unique virtual-socket id assigned by the MultiplexerSocketController. Used
     * to route all inbound and outbound frames for this logical socket.
     */
    protected final int _id;

    /**
     * Name of the logical VirtualServerSocket on the server to which this
     * VirtualSocket is bound.
     */
    protected String _serverSocketName;

    /**
     * The shared-input wrapper returned by {@link #getInputStream()}. All reads
     * ultimately delegate to the virtualized {@link #read()} or
     * {@link #read(byte[], int, int)} methods implemented by the controller.
     */
    private InputStream _inputStream;

    /**
     * The shared-output wrapper returned by {@link #getOutputStream()}. All writes
     * ultimately delegate to the virtualized {@link #write(int)} or
     * {@link #write(byte[], int, int)} methods implemented by the controller.
     */
    private OutputStream _outputStream;

    /**
     * Lock used by subclasses and the controller when coordinating read access to
     * the real socket. VirtualSocket read calls may block on this lock until the
     * controller assigns a frame for this socket id.
     */
    protected final Object _lockObject = new Object();

    /**
     * Timeout duration (in seconds) applied to read operations. Used by the
     * controller to determine when a waiting VirtualSocket should yield or abort.
     */
    private int timeoutSeconds; 
    
    
    /**
     * Constructs a VirtualSocket associated with a specific real-connection id and
     * virtual-socket id. Initializes internal input and output stream wrappers.
     *
     * @param connectionId id of the real connection hosting this logical socket
     * @param id virtual-socket id assigned by the MultiplexerSocketController;
     *           must be >= 0
     * @param serverSocketName name of the logical server-side VirtualServerSocket
     * @throws IllegalArgumentException if id is less than 0
     */
    protected VirtualSocket(int connectionId, int id, String serverSocketName) {
        if (id < 0) {
            throw new IllegalArgumentException("id can not be less then 0");
        }
        this._connectionId = connectionId;
        this._id = id;
        this._serverSocketName = serverSocketName;
        createInputStream();
        createOutputStream();
    }

    /**
     * Lazily initializes the internal InputStream wrapper. All read operations on
     * this wrapper delegate to {@link #read()} or the multi-byte version of read.
     * The wrapper ensures that calling code never interacts directly with the real
     * socket.
     */
    protected synchronized void createInputStream() {
        if (_inputStream != null) return;

        _inputStream = new InputStream() {
            @Override
            public int read() throws IOException {
                int value = VirtualSocket.this.read();
                return value;
            }

            @Override
            public int read(byte[] bs) throws IOException {
                return this.read(bs, 0, bs.length);
            }

            @Override
            public int read(byte[] bs, int off, int len) throws IOException {
                if (bs == null || len < 1) return 0;
                int x = VirtualSocket.this.read(bs, off, len);
                return x;
            }
        };
    }

    /**
     * Returns this VirtualSocket's InputStream wrapper. The wrapper delegates all
     * reads to the virtualized read methods implemented by the controller.
     *
     * @return InputStream for this virtual socket
     * @throws SocketException if the socket has been closed
     */
    @Override
    public InputStream getInputStream() throws IOException {
        if (isClosed()) {
            throw new SocketException("Socket is closed");
        }

        if (_inputStream == null) {
            createInputStream();
        }
        return _inputStream;
    }

    /**
     * Lazily initializes the internal OutputStream wrapper. All write operations
     * on the wrapper delegate to {@link #write(int)} or the multi-byte version of
     * write, allowing the controller to serialize writes through the real socket.
     */
    protected synchronized void createOutputStream() {
        if (_outputStream != null) return;

        _outputStream = new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                VirtualSocket.this.write(b);
            }

            @Override
            public void write(byte[] bs) throws IOException {
                write(bs, 0, bs.length);
            }

            @Override
            public void write(byte[] bs, int off, int len) throws IOException {
                if (bs == null || len < 1) return;
                VirtualSocket.this.write(bs, off, len);
            }
        };
    }

    /**
     * Returns this VirtualSocket's OutputStream wrapper. Write operations on the
     * wrapper are forwarded to the virtualized write methods implemented by the
     * controller.
     *
     * @return OutputStream for this virtual socket
     * @throws SocketException if the socket has been closed
     */
    @Override
    public OutputStream getOutputStream() throws IOException {
        if (isClosed()) {
            throw new SocketException("Socket is closed");
        }

        if (_outputStream == null) {
            createOutputStream();
        }
        return _outputStream;
    }

    /**
     * Returns the id of the real TCP connection associated with this VirtualSocket.
     *
     * @return real-connection id
     */
    public int getConnectionId() {
        return _connectionId;
    }

    /**
     * Returns the unique virtual-socket id assigned by the MultiplexerSocketController.
     *
     * @return virtual-socket id
     */
    public int getId() {
        return _id;
    }

    /**
     * Returns the name of the VirtualServerSocket to which this VirtualSocket is
     * logically connected.
     *
     * @return server-socket name
     */
    public String getServerSocketName() {
        return this._serverSocketName;
    }

    /**
     * Reads up to {@code len} bytes for this VirtualSocket. The implementation is
     * provided by MultiplexerSocketController, which manages routing and ensures
     * that only this VirtualSocket may read its assigned frame.
     *
     * @param bs destination buffer
     * @param off offset into buffer
     * @param len maximum bytes to read
     * @return number of bytes read
     * @throws IOException if the connection is closed or a socket error occurs
     */
    public abstract int read(byte[] bs, int off, int len) throws IOException;

    /**
     * Reads a single byte for this VirtualSocket. Delegated through the controller
     * to the real socket, but only when the controller assigns this VirtualSocket
     * as the active reader.
     *
     * @return next byte, or -1 on end of stream
     * @throws IOException if the connection is closed or a socket error occurs
     */
    public abstract int read() throws IOException;

    /**
     * Writes {@code len} bytes for this VirtualSocket. The controller determines
     * chunking, fairness, throttling, and ordering when multiplexing data onto the
     * real socket.
     *
     * @param bs source buffer
     * @param off offset into buffer
     * @param len number of bytes to write
     * @throws IOException if the underlying real connection fails
     */
    public abstract void write(byte[] bs, int off, int len) throws IOException;

    /**
     * Writes a single byte for this VirtualSocket. Delegated through the
     * MultiplexerSocketController so the real socket is accessed safely.
     *
     * @param b byte to write
     * @throws IOException if writing fails
     */
    public abstract void write(int b) throws IOException;

    /**
     * Closes this VirtualSocket. If {@code bSendCommand} is true, the controller
     * sends a CMD_CloseVSocket command to the peer. Regardless of whether a
     * command is sent, all blocked readers/writers for this socket are released.
     *
     * @param bSendCommand true to notify peer of close; false to close locally only
     * @throws IOException if close notification or internal cleanup fails
     */
    public abstract void close(boolean bSendCommand) throws IOException;
    
    /**
     * Sets the timeout (in seconds) used when waiting for read operations. The
     * MultiplexerSocketController enforces this timeout when determining whether a
     * VirtualSocket has waited too long for its frame.
     *
     * @param x timeout value in seconds
     */
    public void setTimeoutSeconds(int x) {
        this.timeoutSeconds = x;
    }

    /**
     * Returns the read timeout (in seconds) assigned to this VirtualSocket.
     *
     * @return timeout in seconds
     */
    public int getTimeoutSeconds() {
        return this.timeoutSeconds;
    }
}
