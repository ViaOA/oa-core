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
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.OutputStream;
import java.io.StreamCorruptedException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.viaoa.comm.multiplexer.io.VirtualSocket;

/**
 * Customized {@link ObjectOutputStream} used by OA's remoting system.
 * Implements a compact class-descriptor protocol that sends full class
 * metadata only once and assigns small integer IDs for subsequent writes.
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Eliminates the standard Java stream header.</li>
 *   <li>Uses {@link RemoteBufferedOutputStream} for high-performance writes.</li>
 *   <li>Shares class-descriptor caches across nested remote streams.</li>
 *   <li>Writes new class descriptors lazily and moves them into the global
 *       cache only after the entire object has been flushed, avoiding race
 *       conditions.</li>
 *   <li>Provides a fast ASCII string writer specialized for protocol data.</li>
 * </ul>
 *
 * <p>
 * Combined with {@link RemoteObjectInputStream}, this forms OA’s
 * high-performance serialization format used for all Multiplexer-based
 * remoting.
 * </p>
 *
 * @author vvia
 */
public class RemoteObjectOutputStream extends ObjectOutputStream {
	
	/**
	 * Cache mapping fully qualified class names to assigned integer
	 * class-descriptor identifiers.
	 * <p>
	 * Used to avoid repeatedly transmitting full {@link ObjectStreamClass}
	 * metadata over the stream.
	 * </p>
	 */
    private ConcurrentHashMap<String, Integer> hmClassDesc;
    
    /**
     * Atomic counter used to generate new class-descriptor identifiers.
     */
    private AtomicInteger aiClassDesc;
    
    /**
     * Temporary cache for newly assigned class-descriptor identifiers.
     * <p>
     * Entries are promoted to the main cache only after the stream is flushed,
     * preventing race conditions.
     * </p>
     */
    private HashMap<String, Integer> hmTemp; 

    /**
     * Creates a {@code RemoteObjectOutputStream} using a virtual socket.
     *
     * @param socket the virtual socket providing the output stream
     * @throws IOException if an I/O error occurs
     */
    public RemoteObjectOutputStream(VirtualSocket socket) throws IOException {
        this(socket, null, null);
    }

    /**
     * Creates a {@code RemoteObjectOutputStream} using an existing output stream.
     * <p>
     * If a parent {@code RemoteObjectOutputStream} is provided, its class-descriptor
     * caches and counters are reused.
     * </p>
     *
     * @param os the output stream to write to
     * @param ros an existing {@code RemoteObjectOutputStream} whose caches are reused,
     *        or {@code null}
     * @throws IOException if an I/O error occurs
     */
    public RemoteObjectOutputStream(OutputStream os, RemoteObjectOutputStream ros) throws IOException {
        super(new RemoteBufferedOutputStream(os));
        if (ros != null) {
            this.hmClassDesc = ros.hmClassDesc;
            this.aiClassDesc = ros.aiClassDesc;
            this.hmTemp = ros.hmTemp;
        }
    }
    
    /**
     * Creates a {@code RemoteObjectOutputStream} with explicit class-descriptor caches.
     * <p>
     * Uses a {@link RemoteBufferedOutputStream} for high-performance writes.
     * </p>
     *
     * @param socket the virtual socket providing the output stream
     * @param hmClassDesc shared map of class names to descriptor identifiers
     * @param aiClassDesc shared atomic counter for descriptor identifiers
     * @throws IOException if an I/O error occurs
     */
    public RemoteObjectOutputStream(
            VirtualSocket socket, 
            ConcurrentHashMap<String, Integer> hmClassDesc, 
            AtomicInteger aiClassDesc) throws IOException {

        // slowest  207000ns rt, no buffering        
        // super(socket.getOutputStream());
        
        // 95000ns rt
        // super( new BufferedOutputStream(socket.getOutputStream()) );
        
        // fastest: 76000 rt (plus less gc)
        super(new RemoteBufferedOutputStream(socket.getOutputStream()));
        this.hmClassDesc = hmClassDesc;
        this.aiClassDesc = aiClassDesc;
    }
    
    /**
     * Overrides the default stream-header writer.
     * <p>
     * This implementation intentionally writes no stream header.
     * </p>
     *
     * @throws IOException if an I/O error occurs
     * @throws StreamCorruptedException if the stream is corrupted
     */
    @Override
    protected void writeStreamHeader() throws IOException, StreamCorruptedException {
        // do nothing
    }

    /**
     * Flushes the stream and finalizes any newly written class descriptors.
     * <p>
     * Promotes temporary class-descriptor identifiers to the shared cache
     * after all objects have been fully written.
     * </p>
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void flush() throws IOException {
        super.flush();
        if (hmTemp == null) return;
        
        // now that the objects have been sent with any new classDesc, add them to the hm 
        // has to be done after it has been fully written, to avoid race condition
        for (Map.Entry<String, Integer> entry : hmTemp.entrySet()) {
           hmClassDesc.put(entry.getKey(), entry.getValue()); 
        }
        hmTemp.clear();
    }
    
    /**
     * Writes a class descriptor using an integer identifier protocol.
     * <p>
     * Sends the full descriptor only once and assigns a small integer identifier
     * for subsequent occurrences.
     * </p>
     *
     * @param desc the class descriptor to write
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void writeClassDescriptor(ObjectStreamClass desc) throws IOException {
        String s = desc.getName();

        Object objx; 
        if (hmClassDesc != null) {
            objx = hmClassDesc.get(s);
            if (objx == null && hmTemp != null) objx = hmTemp.get(s);
        }
        else {
            objx = null;
        }
        
        int id;
        if (objx == null) {
            if (hmClassDesc == null || aiClassDesc == null) {
                id = -1;
            }
            else {
                id = aiClassDesc.getAndIncrement();
                if (hmTemp == null) hmTemp = new HashMap<String, Integer>();
                hmTemp.put(s, id);
            }
            writeInt(-1);
            writeInt(id);
            super.writeClassDescriptor(desc);
        }
        else {
            id = ((Integer) objx).intValue();
            writeInt(id);
        }
    }
    
    /**
     * Writes an ASCII-encoded string to the stream.
     * <p>
     * The string length is written as a short value, followed by the raw bytes.
     * A {@code null} value is written as a zero length.
     * </p>
     *
     * @param s the ASCII string to write, or {@code null}
     * @throws IOException if an I/O error occurs
     * @throws StreamCorruptedException if the stream is corrupted
     */
    public void writeAsciiString(String s) throws IOException, StreamCorruptedException {
        if (s == null) {
            writeShort(0);
        }
        else {
            short x = (short) s.length();
            writeShort(x);
            byte[] bs = new byte[x];
            s.getBytes(0, x, bs, 0);
            write(bs);
        }
    }
}
