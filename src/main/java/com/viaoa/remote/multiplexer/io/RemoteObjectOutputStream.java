/*  Copyright 1999 Vince Via vvia@viaoa.com
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/
package com.viaoa.remote.multiplexer.io;

import java.io.DataOutputStream;
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
 * Used internally for remoting objects between clients and servers.
 * @author vvia
 */
public class RemoteObjectOutputStream extends ObjectOutputStream {
    private ConcurrentHashMap<String, Integer> hmClassDesc;
    private AtomicInteger aiClassDesc;
    private HashMap<String, Integer> hmTemp; 

    public RemoteObjectOutputStream(VirtualSocket socket) throws IOException {
        this(socket, null, null);
    }

    // 20141121 used by OAObjectSerializer to embed compressed objects and share the outer remoteObjectStream
    public RemoteObjectOutputStream(OutputStream os, RemoteObjectOutputStream ros) throws IOException {
        super(new RemoteBufferedOutputStream(os));
        if (ros != null) {
            this.hmClassDesc = ros.hmClassDesc;
            this.aiClassDesc = ros.aiClassDesc;
            this.hmTemp = ros.hmTemp;
        }
    }
    
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
    
    @Override
    protected void writeStreamHeader() throws IOException, StreamCorruptedException {
        // do nothing
    }

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
