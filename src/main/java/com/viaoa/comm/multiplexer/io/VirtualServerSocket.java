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
	 * Registered logical name of this VirtualServerSocket. Remote multiplexer
	 * clients use this name when requesting creation of a corresponding
	 * VirtualSocket on the server.
	 */
	private String _name;

	/**
	 * Creates a new VirtualServerSocket with the specified logical name. The
	 * socket does not bind to a physical port; it becomes registered with the
	 * MultiplexerServerSocketController so that virtual connections can be
	 * accepted based on this name.
	 *
	 * @param name logical server-socket name used by clients when creating a
	 *             VirtualSocket
	 * @throws IOException if superclass initialization fails
	 */
    public VirtualServerSocket(String name) throws IOException {
        this._name = name;
    }

    /**
     * Returns the registered logical name for this VirtualServerSocket.
     *
     * @return logical name used by clients to target this virtual endpoint
     */
    public String getName() {
        return _name;
    }

    /**
     * Closes this VirtualServerSocket. Delegates to {@link ServerSocket#close()}.
     *
     * @throws IOException if closing the underlying ServerSocket fails
     */
    @Override
    public void close() throws IOException {
        super.close();
    }
}
