/*
 * Copyright 1999–2025 Vince Via (vvia@viaoa.com)
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

import java.io.IOException;
import java.net.ServerSocket;

/**
 * Represents a logical server-side endpoint used by the multiplexer. Unlike a
 * normal {@link ServerSocket}, this socket does not bind to a physical port.
 * Instead, it is registered by name so that remote clients can request a
 * VirtualSocket connection to it.
 *
 * A VirtualServerSocket is created and managed by the
 * MultiplexerServerSocketController and serves as the factory for virtual
 * client connections.
 */
public class VirtualServerSocket extends ServerSocket {
    /**
     * registered name for the socket, that is used by MultiplexerClient to create a MultiplexerSocket connection.
     */
    private String _name;

    /**
     * Create a new ServerSocket.
     * 
     * @param name
     *            name for MultiplexerClient to use to create a connection.
     */
    public VirtualServerSocket(String name) throws IOException {
        this._name = name;
    }

    /**
     * Registered name for the socket, that is used by MultiplexerClient when creating a MultiplexerSocket connection.
     */
    public String getName() {
        return _name;
    }

    @Override
    public void close() throws IOException {
        super.close();
    }
}
