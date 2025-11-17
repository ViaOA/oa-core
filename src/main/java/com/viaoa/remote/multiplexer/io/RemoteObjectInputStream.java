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
    private ConcurrentHashMap<Integer, ObjectStreamClass> hmClassDesc;

    public RemoteObjectInputStream(Socket socket, 
            ConcurrentHashMap<Integer, ObjectStreamClass> hmClassDesc) throws IOException {
        super(socket.getInputStream());

        this.hmClassDesc = hmClassDesc;
    }
    
    // 20141121 used by OAObjectSerializer to embed compressed objects
    public RemoteObjectInputStream(InputStream is, RemoteObjectInputStream rois) throws IOException {
        super(is);
        if (rois != null) {
            this.hmClassDesc = rois.hmClassDesc;
        }
    }
    
    
    @Override
    protected void readStreamHeader() throws IOException, StreamCorruptedException {
    }

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
    public String readAsciiString() throws IOException {
        short x = readShort();
        if (x == 0) return null;
        byte[] bs = new byte[x];
        readFully(bs);
        String s = new String(bs, 0); // ascii only
        return s;
    }
}
