package com.viaoa.remote.multiplexer.io;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.test.pos.model.oa.Register;
import com.viaoa.oa.OA;
import com.viaoa.runtime.OARuntime;

class RemoteBufferedOutputStreamTest {

    @BeforeEach
    void beforeEach() {
        synchronized (RemoteBufferedOutputStream.Lock) {
            Arrays.fill(RemoteBufferedOutputStream.isUsed, false);
        }
        OA oa = OARuntime.createDefaultOA(Register.class);
    }

    @AfterEach
    void afterEach() {
        OARuntime.oa(Register.class).close();
    }
    
    
    @Test
    void constructorDoesNotWriteUntilDataIsFlushed() throws Exception {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();

        new RemoteBufferedOutputStream(bout);

        assertEquals(0, bout.size());
    }

    @Test
    void getPoolBufferReturnsReusableBuffer() {
        byte[] buffer = RemoteBufferedOutputStream.getPoolBuffer();

        assertNotNull(buffer);
        assertTrue(buffer.length >= 8 * 1024);
        RemoteBufferedOutputStream.releasePoolBuffer(buffer);
        assertSame(buffer, RemoteBufferedOutputStream.getPoolBuffer());
    }

    @Test
    void releasePoolBufferIgnoresNullAndUnknownBuffers() {
        assertDoesNotThrow(() -> RemoteBufferedOutputStream.releasePoolBuffer(null));
        assertDoesNotThrow(() -> RemoteBufferedOutputStream.releasePoolBuffer(new byte[4]));
    }

    @Test
    void writeSingleBytesAreBufferedUntilFlush() throws Exception {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        RemoteBufferedOutputStream out = new RemoteBufferedOutputStream(bout);

        out.write(65);
        out.write(66);
        assertEquals(0, bout.size());

        out.flush();
        assertArrayEquals(new byte[] { 65, 66 }, bout.toByteArray());
    }

    @Test
    void writeByteArrayBuffersSmallPayloadUntilFlush() throws Exception {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        RemoteBufferedOutputStream out = new RemoteBufferedOutputStream(bout);

        out.write(new byte[] { 1, 2, 3, 4 }, 1, 2);
        assertEquals(0, bout.size());

        out.flush();
        assertArrayEquals(new byte[] { 2, 3 }, bout.toByteArray());
    }

    @Test
    void writeByteArrayBypassesBufferWhenPayloadIsLarge() throws Exception {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        RemoteBufferedOutputStream out = new RemoteBufferedOutputStream(bout);
        byte[] payload = new byte[70_000];
        payload[0] = 1;
        payload[payload.length - 1] = 2;

        out.write(payload, 0, payload.length);

        assertEquals(payload.length, bout.size());
        assertEquals(1, bout.toByteArray()[0]);
        assertEquals(2, bout.toByteArray()[payload.length - 1]);
    }

    @Test
    void flushWritesBufferFlushesDelegateAndReleasesPoolBuffer() throws Exception {
        TrackingOutputStream bout = new TrackingOutputStream();
        RemoteBufferedOutputStream out = new RemoteBufferedOutputStream(bout);

        out.write(1);
        byte[] buffer = out.bsBuffer;
        out.flush();

        assertTrue(bout.flushed);
        assertNull(out.bsBuffer);
        byte[] next = RemoteBufferedOutputStream.getPoolBuffer();
        assertSame(buffer, next);
    }

    @Test
    void flushPropagatesDelegateFailure() throws Exception {
        RemoteBufferedOutputStream out = new RemoteBufferedOutputStream(new FailingOutputStream());
        out.write(1);

        assertThrows(IOException.class, out::flush);
    }

    private static class TrackingOutputStream extends ByteArrayOutputStream {
        boolean flushed;

        @Override
        public void flush() throws IOException {
            flushed = true;
            super.flush();
        }
    }

    private static class FailingOutputStream extends ByteArrayOutputStream {
        @Override
        public void flush() throws IOException {
            throw new IOException("flush failed");
        }
    }
}
