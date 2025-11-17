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

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * High-performance buffered output stream used by OA's remoting and
 * multiplexer layers. Unlike {@link java.io.BufferedOutputStream}, this
 * implementation uses a shared pool of reusable byte[] buffers to avoid
 * per-stream allocation and reduce garbage collection pressure.
 *
 * <p>Characteristics:</p>
 * <ul>
 *   <li>Maintains a static pool of buffers of varying sizes for optimal throughput.</li>
 *   <li>Designed for single-threaded use—no synchronization overhead.</li>
 *   <li>Automatically releases its buffer back to the pool when flushed or closed.</li>
 *   <li>Falls back to a small private buffer if all pooled buffers are in use.</li>
 *   <li>Used internally by {@link RemoteObjectOutputStream} to improve remote
 *       serialization performance.</li>
 * </ul>
 *
 * <p>
 * This stream significantly outperforms standard buffering approaches in
 * tight remoting loops and dramatically reduces garbage creation under load.
 * </p>
 *
 * @author vvia
 */
public class RemoteBufferedOutputStream extends FilterOutputStream {
    private static final int TotalBuffers = 32;
    private static final int BufferSize = 8 * 1024;
    protected byte[] bsBuffer;
    protected int count;
    protected boolean bOwnedBuffer;  // true if the bsBuffer is not from the pool

    /**
     * Creates a new buffered output stream to write data to the specified underlying output stream with
     * the specified buffer size.
     */
    public RemoteBufferedOutputStream(OutputStream out) {
        super(out);
    }

    // create a pool of available buffers
    static boolean[] isUsed = new boolean[TotalBuffers];
    static byte[][] buffers = new byte[TotalBuffers][];
    static final Object Lock = new Object();

    protected static byte[] getPoolBuffer() {
        synchronized (Lock) {
            for (int i = 0; i < TotalBuffers; i++) {
                if (!isUsed[i]) {
                    isUsed[i] = true;
                    if (buffers[i] == null) {
                        int x = BufferSize;
                        if (i < 2) x *= 8;
                        else if (i < 4) x *= 4;
                        else if (i < 6) x *= 3;
                        else if (i < 8) x *= 2;
                        buffers[i] = new byte[ x ];
                    }
                    return buffers[i];
                }
            }
        }
        return null;
    }
    protected static void releasePoolBuffer(byte[] bs) {
        if (bs == null) return;
        synchronized (Lock) {
            for (int i = 0; i < TotalBuffers; i++) {
                if (buffers[i] == bs) {
                    isUsed[i] = false;
                    break;
                }
            }
        }
    }
    protected byte[] getBuffer() {
        byte[] bs = getPoolBuffer();
        if (bs == null) {
            bs = new byte[2048]; // use a smaller size
            bOwnedBuffer = true;
        }
        return bs;
    }

    private void freeBuffer() {
        if (!bOwnedBuffer && bsBuffer != null) {
            releasePoolBuffer(bsBuffer);
            bsBuffer = null;
        }
    }
    
    @Override
    public void close() throws IOException {
        freeBuffer();
        super.close();
    }

    @Override
    protected void finalize() throws Throwable {
        freeBuffer();
        super.finalize();
    }
    
    /** writes the internal buffer to output, and resets position to 0 */
    private void writeBuffer() throws IOException {
        if (count > 0 && bsBuffer != null) {
            out.write(bsBuffer, 0, count);
            count = 0;
        }
    }

    public void write(int b) throws IOException {
        if (bsBuffer == null) {
            bsBuffer = getBuffer();
        }
        else if (count >= bsBuffer.length) {
            writeBuffer();
        }
        bsBuffer[count++] = (byte) b;
    }

    public void write(byte b[], int off, int len) throws IOException {
        if (bsBuffer == null) {
            bsBuffer = getBuffer();
        }
        if (len >= bsBuffer.length) {
            if (count > 0) {
                writeBuffer();
            }
            out.write(b, off, len);
            return;
        }
        if (len > bsBuffer.length - count) {
            writeBuffer();
        }
        System.arraycopy(b, off, bsBuffer, count, len);
        count += len;
    }

    /**
     * Overwritten to include freeing the byte[] buffer from the shared pool.
     */
    public void flush() throws IOException {
        writeBuffer();
        out.flush();
        freeBuffer();  // good chance that it is not needed anymore
    }
}
