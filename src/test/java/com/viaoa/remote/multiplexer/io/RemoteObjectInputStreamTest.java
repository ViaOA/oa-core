package com.viaoa.remote.multiplexer.io;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectStreamClass;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

class RemoteObjectInputStreamTest {

    @Test
    void constructorWithInputStreamDoesNotRequireStandardHeader() {
        assertDoesNotThrow(() -> new RemoteObjectInputStream(new ByteArrayInputStream(new byte[0]), null));
    }

    @Test
    void constructorWithParentReusesParentDescriptorCache() throws Exception {
        ConcurrentHashMap<String, Integer> outputCache = new ConcurrentHashMap<>();
        ConcurrentHashMap<Integer, ObjectStreamClass> inputCache = new ConcurrentHashMap<>();
        AtomicInteger id = new AtomicInteger(1);
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        RemoteObjectOutputStream out = new RemoteObjectOutputStream(new RemoteObjectOutputStreamTest.TestVirtualSocket(bout), outputCache, id);
        out.writeObject(new RemoteObjectOutputStreamTest.Payload("value"));
        out.flush();

        RemoteObjectInputStream parent = new RemoteObjectInputStream(new ByteArrayInputStream(bout.toByteArray()), null);
        RemoteObjectOutputStreamTest.setInputDescriptorCache(parent, inputCache);
        Object obj = parent.readObject();
        assertInstanceOf(RemoteObjectOutputStreamTest.Payload.class, obj);
        assertEquals("value", ((RemoteObjectOutputStreamTest.Payload) obj).value);

        RemoteObjectInputStream child = new RemoteObjectInputStream(new ByteArrayInputStream(new byte[] { 0, 0 }), parent);
        assertSame(inputCache, descriptorCache(child));
    }

    @Test
    void readAsciiStringReturnsNullForZeroLength() throws Exception {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        RemoteObjectOutputStream out = new RemoteObjectOutputStream(bout, null);
        out.writeAsciiString(null);
        out.flush();

        RemoteObjectInputStream in = new RemoteObjectInputStream(new ByteArrayInputStream(bout.toByteArray()), null);

        assertNull(in.readAsciiString());
    }

    @Test
    void readAsciiStringReadsLengthPrefixedBytes() throws Exception {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        RemoteObjectOutputStream out = new RemoteObjectOutputStream(bout, null);
        out.writeAsciiString("abc");
        out.flush();

        RemoteObjectInputStream in = new RemoteObjectInputStream(new ByteArrayInputStream(bout.toByteArray()), null);

        assertEquals("abc", in.readAsciiString());
    }

    @SuppressWarnings("unchecked")
    private static ConcurrentHashMap<Integer, ObjectStreamClass> descriptorCache(RemoteObjectInputStream in) throws Exception {
        java.lang.reflect.Field field = RemoteObjectInputStream.class.getDeclaredField("hmClassDesc");
        field.setAccessible(true);
        return (ConcurrentHashMap<Integer, ObjectStreamClass>) field.get(in);
    }
}
