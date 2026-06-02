package com.viaoa.serialize;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.util.Arrays;

import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OAObjectSerializerFailureVisibilityTest {
    public static class Item extends OAObject implements Serializable {
        private static final long serialVersionUID = 1L;
        private String name;
        public Item() { }
        public Item(String name) { this.name = name; }
        public String getName() { return name; }
    }

    public static class ExplodingObject implements Serializable {
        private static final long serialVersionUID = 1L;
        private void writeObject(ObjectOutputStream out) throws IOException {
            throw new IOException("write exploded");
        }
    }

    @Test
    void rootWriteFailurePropagatesAsIOException() {
        OAObjectSerializer<Object> ser = new OAObjectSerializer<>(new ExplodingObject(), false);

        assertThrows(IOException.class, () -> write(ser));
    }

    @Test
    void extraObjectWriteFailurePropagatesAsIOException() {
        OAObjectSerializer<Item> ser = new OAObjectSerializer<>(new Item("A"), new ExplodingObject(), false, null);

        assertThrows(IOException.class, () -> write(ser));
    }

    @Test
    void truncatedPayloadsFailVisibly() throws Exception {
        byte[] uncompressed = write(new OAObjectSerializer<>(new Item("A"), false));
        assertThrows(Exception.class, () -> read(Arrays.copyOf(uncompressed, Math.max(1, uncompressed.length / 2))));

        byte[] compressed = write(new OAObjectSerializer<>(new Item("A"), true));
        assertThrows(Exception.class, () -> read(Arrays.copyOf(compressed, Math.max(1, compressed.length / 2))));
    }

    @Test
    void corruptPayloadFailsVisibly() throws Exception {
        byte[] bytes = write(new OAObjectSerializer<>(new Item("A"), false));
        bytes[bytes.length - 1] = (byte) (bytes[bytes.length - 1] ^ 0x7f);

        assertThrows(Exception.class, () -> read(bytes));
    }

    @Test
    void failedWriteDoesNotMakeSerializerLookLikeCompleteSuccess() {
        OAObjectSerializer<Object> ser = new OAObjectSerializer<>(new ExplodingObject(), false);

        assertThrows(IOException.class, () -> write(ser));
        assertFalse(ser.hasReachedMax());
    }

    private static byte[] write(Object obj) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeObject(obj);
        }
        return bos.toByteArray();
    }

    private static Object read(byte[] bytes) throws Exception {
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            return in.readObject();
        }
    }
}
