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
package com.viaoa.util;

import java.io.*;
import java.util.logging.Logger;
import java.util.zip.*;

/**
 * Wrapper used to compress an object during Java serialization. The wrapper
 * intercepts the normal serialization process using the private
 * {@code writeObject} and {@code readObject} hooks and applies Deflate
 * compression to the serialized form of the wrapped object. <p>
 *
 * On serialization, the wrapped object is encoded through an
 * {@link ObjectOutputStream} layered on top of a
 * {@link DeflaterOutputStream}. On deserialization, the bytes are restored
 * using an {@link InflaterInputStream} and passed into an
 * {@link ObjectInputStream} to reconstruct the original object. <p>
 *
 * Only the wrapped object's serialized representation is compressed; the
 * wrapper itself remains a normal serializable object. Streams are not closed
 * to avoid closing the parent {@link ObjectOutputStream} or
 * {@link ObjectInputStream} provided by the caller. The wrapper is intended
 * for use in OA's remote calls, caching, and distributed communication
 * subsystems where bandwidth efficiency is required.
 */
public final class OACompressWrapper implements Serializable {
    static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(OACompressWrapper.class.getName());
    
    private Object object; // object to serialize

    /**
     * @param object to serialize
     */
    public OACompressWrapper(Object object) {
        this.object = object;
    }

    public Object getObject() {
        return object;
    }
    
    /**
     * Called by objectStream to serialize wrapper.  
     */
    private void writeObject(java.io.ObjectOutputStream stream) throws IOException {
        stream.writeBoolean(object != null);
        if (object != null) {
            Deflater d = new Deflater(Deflater.DEFAULT_COMPRESSION);//BEST_SPEED BEST_COMPRESSION);
        	DeflaterOutputStream dos = new DeflaterOutputStream(stream, d, 1024*2);
        	ObjectOutputStream oos = new ObjectOutputStream(dos);
        	oos.writeObject(object);
            //oos.flush();
            dos.finish();
            //dos.flush();
            // dos.close(); // might affect stream by closing it (?? not sure)
            // long sizeBefore = d.getBytesRead();
            // long sizeAfter = d.getBytesWritten();
        }        
    }


    /**
     * Called by objectStream to deserialize a wrapper. 
     */
    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        if (stream.readBoolean()) {
    		Inflater inflater = new Inflater();
        	InflaterInputStream iis = new InflaterInputStream(stream, inflater, 1024*2);
        	
        	ObjectInputStream ois = new ObjectInputStream(iis);
        	object = ois.readObject();
    	}
    }

    

}


