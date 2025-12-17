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

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.io.StreamCorruptedException;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Customized {@link ObjectInputStream} used by OA's remoting system. This
 * implementation eliminates the standard Java stream header and replaces
 * Java's class-descriptor mechanism with a compact ID-based protocol.
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Reads class descriptors using small integer IDs shared between
 *       client and server.</li>
 *   <li>Caches known {@link ObjectStreamClass} instances in a
 *       {@link java.util.concurrent.ConcurrentHashMap}.</li>
 *   <li>Allows nested streams (e.g., compressed or embedded remote objects)
 *       to reuse the parent class-descriptor cache.</li>
 *   <li>Provides a fast ASCII string reader for small protocol strings.</li>
 * </ul>
 *
 * <p>
 * This class is a core component of OA's lightweight serialization protocol,
 * which significantly reduces network bandwidth when remoting objects.
 * </p>
 *
 * @author vvia
 */
public class RemoteObjectInputStream extends ObjectInputStream {
	
	/**
	 * Cache mapping integer class identifiers to their corresponding
	 * {@link ObjectStreamClass} instances.
	 * <p>
	 * This map is shared between client and server to avoid repeatedly
	 * transmitting full class descriptors.
	 * </p>
	 */
    private ConcurrentHashMap<Integer, ObjectStreamClass> hmClassDesc;

    /**
     * Creates a {@code RemoteObjectInputStream} using a socket input stream.
     * <p>
     * Initializes the stream from the socket and assigns the shared
     * class-descriptor cache.
     * </p>
     *
     * @param socket the socket providing the input stream
     * @param hmClassDesc shared map of class descriptor identifiers
     * @throws IOException if an I/O error occurs
     */
    public RemoteObjectInputStream(Socket socket, 
            ConcurrentHashMap<Integer, ObjectStreamClass> hmClassDesc) throws IOException {
        super(socket.getInputStream());

        this.hmClassDesc = hmClassDesc;
    }
    
    /**
     * Creates a {@code RemoteObjectInputStream} using an existing input stream.
     * <p>
     * If a parent {@code RemoteObjectInputStream} is provided, its class-descriptor
     * cache is reused.
     * </p>
     *
     * @param is the input stream to read from
     * @param rois an existing {@code RemoteObjectInputStream} whose cache is reused,
     *        or {@code null}
     * @throws IOException if an I/O error occurs
     */
    public RemoteObjectInputStream(InputStream is, RemoteObjectInputStream rois) throws IOException {
        super(is);
        if (rois != null) {
            this.hmClassDesc = rois.hmClassDesc;
        }
    }
    
    
    /**
     * Overrides the default stream-header reader.
     * <p>
     * This implementation intentionally does nothing, eliminating the standard
     * Java serialization stream header.
     * </p>
     *
     * @throws IOException if an I/O error occurs
     * @throws StreamCorruptedException if the stream is corrupted
     */
    @Override
    protected void readStreamHeader() throws IOException, StreamCorruptedException {
    }

    /**
     * Reads a class descriptor using an integer identifier protocol.
     * <p>
     * If the identifier is non-negative, the descriptor is retrieved from the
     * shared cache. Otherwise, the descriptor is read from the stream and
     * optionally cached.
     * </p>
     *
     * @return the resolved {@link ObjectStreamClass}
     * @throws IOException if an I/O error occurs
     * @throws ClassNotFoundException if the class cannot be resolved
     */
    @Override
    protected ObjectStreamClass readClassDescriptor() throws IOException, ClassNotFoundException {
        ObjectStreamClass osc;
        int id = readInt();
        if (id >= 0) {
            osc = hmClassDesc.get(id);
        }
        else {
            id = readInt();
            osc = super.readClassDescriptor();
            if (id >= 0) {
                hmClassDesc.put(id, osc);
            }
        }
        return osc;
    }

    // faster then using readUTF
    /**
     * Reads an ASCII-encoded string from the stream.
     * <p>
     * The string length is read as a short value, followed by the raw bytes.
     * A length of zero returns {@code null}.
     * </p>
     *
     * @return the decoded ASCII string, or {@code null} if length is zero
     * @throws IOException if an I/O error occurs
     */
    public String readAsciiString() throws IOException {
        short x = readShort();
        if (x == 0) return null;
        byte[] bs = new byte[x];
        readFully(bs);
        String s = new String(bs, 0); // ascii only
        return s;
    }
}
