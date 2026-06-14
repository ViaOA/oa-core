package com.viaoa.remote.multiplexer.io;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectStreamClass;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

class RemoteObjectOutputStreamTest {

    @Test
    void constructorWithOutputStreamWritesNoStandardStreamHeader() throws Exception {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();

        new RemoteObjectOutputStream(bout, null).flush();

        assertEquals(0, bout.size());
    }

    @Test
    void flushPromotesTemporaryClassDescriptorIds() throws Exception {
        ConcurrentHashMap<String, Integer> outputCache = new ConcurrentHashMap<>();
        AtomicInteger id = new AtomicInteger(1);
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        RemoteObjectOutputStream out = new RemoteObjectOutputStream(new TestVirtualSocket(bout), outputCache, id);

        out.writeObject(new Payload("value"));
        assertTrue(outputCache.isEmpty());

        out.flush();

        assertEquals(Integer.valueOf(1), outputCache.get(Payload.class.getName()));
    }

    @Test
    void writeAsciiStringWritesNullAsZeroLength() throws Exception {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        RemoteObjectOutputStream out = new RemoteObjectOutputStream(bout, null);

        out.writeAsciiString(null);
        out.flush();

        RemoteObjectInputStream in = new RemoteObjectInputStream(new ByteArrayInputStream(bout.toByteArray()), null);
        assertNull(in.readAsciiString());
    }

    @Test
    void writeAsciiStringWritesLengthAndBytes() throws Exception {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        RemoteObjectOutputStream out = new RemoteObjectOutputStream(bout, null);

        out.writeAsciiString("abc");
        out.flush();

        RemoteObjectInputStream in = new RemoteObjectInputStream(new ByteArrayInputStream(bout.toByteArray()), null);
        assertEquals("abc", in.readAsciiString());
    }

    @Test
    void writeObjectRoundTripsWithRemoteObjectInputStream() throws Exception {
        ConcurrentHashMap<String, Integer> outputCache = new ConcurrentHashMap<>();
        ConcurrentHashMap<Integer, ObjectStreamClass> inputCache = new ConcurrentHashMap<>();
        AtomicInteger id = new AtomicInteger(1);
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        RemoteObjectOutputStream out = new RemoteObjectOutputStream(new TestVirtualSocket(bout), outputCache, id);

        out.writeObject(new Payload("value"));
        out.flush();

        RemoteObjectInputStream in = new RemoteObjectInputStream(new TestSocketInputStream(bout.toByteArray()), null);
        setInputDescriptorCache(in, inputCache);
        Object obj = in.readObject();
        assertInstanceOf(Payload.class, obj);
        assertEquals("value", ((Payload) obj).value);
        assertNotNull(inputCache.get(1));
    }

    static class Payload implements java.io.Serializable {
        private static final long serialVersionUID = 1L;
        final String value;

        Payload(String value) {
            this.value = value;
        }
    }

    static void setInputDescriptorCache(RemoteObjectInputStream in, ConcurrentHashMap<Integer, ObjectStreamClass> cache) throws Exception {
        java.lang.reflect.Field field = RemoteObjectInputStream.class.getDeclaredField("hmClassDesc");
        field.setAccessible(true);
        field.set(in, cache);
    }

    static class TestVirtualSocket extends com.viaoa.comm.multiplexer.io.VirtualSocket {
        private final ByteArrayOutputStream out;

        TestVirtualSocket(ByteArrayOutputStream out) {
            super(1, 1, "test");
            this.out = out;
        }

        @Override
        public java.io.OutputStream getOutputStream() {
            return out;
        }

        @Override public int read(byte[] bs, int off, int len) { return -1; }
        @Override public int read() { return -1; }
        @Override public void write(byte[] bs, int off, int len) { }
        @Override public void write(int b) { }
        @Override public void close(boolean bSendCommand) { }
    }

    private static class TestSocketInputStream extends java.io.InputStream {
        private final ByteArrayInputStream in;

        TestSocketInputStream(byte[] bytes) {
            this.in = new ByteArrayInputStream(bytes);
        }

        @Override
        public int read() {
            return in.read();
        }

        @Override
        public int read(byte[] b, int off, int len) {
            return in.read(b, off, len);
        }
    }
}
