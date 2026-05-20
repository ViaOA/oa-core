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
package com.viaoa.io;

import java.io.*;
import java.util.logging.Logger;
import java.util.zip.*;

/*qqqqqqqqqqqqqqqqqqqqqqq
CODEX

1. src/main/java/com/viaoa/io/OACompressWrapper.java / writeObject and readObject

  Concrete bug: compressed object data has no explicit length boundary, so InflaterInputStream can over-read bytes
  belonging to the enclosing ObjectInputStream.

  Runtime scenario: OA remote/multiplexer wraps large args/responses in OACompressWrapper. writeObject writes a
  boolean, then writes compressed object bytes directly onto the parent ObjectOutputStream at lines 86-91. readObject
  then creates InflaterInputStream directly over the parent stream at lines 112-115. Since no compressed byte length
  is written, the inflater can buffer/read beyond the end of the compressed payload and consume bytes belonging to the
  next field/object in the enclosing serialization stream.

  Why this violates OA/OG I/O semantics: remote/sync serialization boundaries must be exact. A compressed wrapper must
  not corrupt the parent object stream or make subsequent remote args/responses unreadable.

  Minimal fix direction: compress into a ByteArrayOutputStream, finish/flush the inner ObjectOutputStream/deflater,
  then write the compressed byte length and byte array to the parent stream. On read, read exactly that byte array and
  inflate from ByteArrayInputStream.

  Suggested CODEX comment location: OACompressWrapper.writeObject around line 87 and readObject around line 113.

  2. src/main/java/com/viaoa/io/OAFile.java / copy, copyResourceToFile, readResourceTextFile, readTextFile,
     writeTextFile

  Concrete bug: streams/readers/writers opened by OA are not closed on exception paths.

  Runtime scenario: a file copy, resource copy, text read, or text write throws during read/write. The methods only
  close streams after successful completion, for example:

  - copy(File, File) lines 318-332
  - copyResourceToFile lines 357-381
  - readResourceTextFile lines 398-415
  - readTextFile lines 469-479 / 496-509 / 525-533
  - writeTextFile lines 553-559 / 577-583

  If an exception occurs before the close call, file handles remain open.

  Why this violates OA/OG I/O semantics: OA-opened streams must be closed on success and failure unless ownership is
  transferred. Leaks can block config/log/tlog/model file updates and destabilize repeated runtime/tooling operations.

  Minimal fix direction: use try-with-resources for every stream/reader/writer opened in these helpers.

  Suggested CODEX comment location: first affected block in OAFile.copy(File, File) around line 318, with note that
  the same pattern exists in resource/text helpers.


 4. src/main/java/com/viaoa/io/OACompressWrapper.java / writeObject and readObject

  Concrete bug: Deflater and Inflater native resources are not explicitly released.

  Runtime scenario: OA remote/multiplexer compresses many arguments/responses using OACompressWrapper. writeObject
  creates a Deflater at line 86 and does not close the DeflaterOutputStream or call d.end(). readObject creates an
  Inflater at line 112 and does not close the inflater stream or call inflater.end(). Under high remote/sync traffic,
  native compression resources can accumulate until GC finalization/cleaning happens.

  Why this violates OA/OG I/O semantics: compression/decompression resources are part of I/O lifecycle. OA remote/sync
  infrastructure should not rely on delayed GC for native resource release under production load.

  Minimal fix direction: use byte-array bounded compression/decompression and close local wrapper streams safely, or
  call Deflater.end() / Inflater.end() in finally without closing the parent object stream.

  Suggested CODEX comment location: line 86 and line 112.


*/

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
    
    /**
     * The object to be serialized and compressed.
     */
    private Object object; // object to serialize

    /**
     * Creates a new wrapper for the specified object.
     *
     * @param object the object to serialize and compress
     */
    public OACompressWrapper(Object object) {
        this.object = object;
    }

    /**
     * Returns the wrapped object.
     *
     * @return the wrapped object
     */
    public Object getObject() {
        return object;
    }
    
    /**
     * Custom serialization hook used to write the wrapped object.
     *
     * This method writes a flag indicating whether an object is present and,
     * if so, serializes the object through a compressed output stream.
     *
     * @param stream the {@link ObjectOutputStream} used for serialization
     * @throws IOException if an I/O error occurs during writing
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
     * Custom deserialization hook used to read the wrapped object.
     *
     * This method reads a presence flag and, if set, restores the wrapped
     * object from a compressed input stream.
     *
     * @param stream the {@link ObjectInputStream} used for deserialization
     * @throws IOException if an I/O error occurs during reading
     * @throws ClassNotFoundException if the wrapped object's class cannot be found
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


