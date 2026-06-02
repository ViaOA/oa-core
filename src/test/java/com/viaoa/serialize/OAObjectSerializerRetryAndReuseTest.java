package com.viaoa.serialize;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;

import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OAObjectSerializerRetryAndReuseTest {
    public static class Item extends OAObject implements Serializable {
        private static final long serialVersionUID = 1L;
        private String name;
        public Item() { }
        public Item(String name) { this.name = name; }
        public String getName() { return name; }
    }

    @Test
    void sameSerializerCanBeWrittenTwiceWithoutRetainingCompressedDeflater() throws Exception {
        OAObjectSerializer<Item> ser = new OAObjectSerializer<>(new Item("A"), true);

        OAObjectSerializer<Item> a = roundTrip(ser);
        OAObjectSerializer<Item> b = roundTrip(ser);

        assertEquals("A", a.getObject().getName());
        assertEquals("A", b.getObject().getName());
        assertEquals(-1, ser.getCompressedWritten());
    }

    @Test
    void retryWithNewSerializerAfterFailureSucceeds() throws Exception {
        class ExplodingObject implements Serializable {
            private static final long serialVersionUID = 1L;
            private void writeObject(ObjectOutputStream out) throws IOException {
                throw new IOException("boom");
            }
        }

        OAObjectSerializer<Object> bad = new OAObjectSerializer<>(new ExplodingObject(), false);
        assertThrows(IOException.class, () -> roundTrip(bad));

        OAObjectSerializer<Item> good = new OAObjectSerializer<>(new Item("A"), false);
        OAObjectSerializer<Item> copy = roundTrip(good);

        assertEquals("A", copy.getObject().getName());
    }

    @Test
    void independentContextsRemainCleanAfterOneIsUsed() {
        OASerializeContext a = new OASerializeContext();
        OASerializeContext b = new OASerializeContext();
        Item item = new Item("A");

        a.markWritten(item);
        a.pushDepth();
        a.setIncludeNulls(true);

        assertFalse(b.hasWritten(item));
        assertEquals(0, b.getDepth());
        assertFalse(b.getIncludeNulls());
    }

    @Test
    void serializerRuntimeStacksAreEmptyAfterSuccessfulRoundTrip() throws Exception {
        OAObjectSerializer<Item> ser = new OAObjectSerializer<>(new Item("A"), false);

        roundTrip(ser);

        assertEquals(0, ser.getStackSize());
        assertEquals(0, ser.getLevelsDeep());
    }

    @SuppressWarnings("unchecked")
    private static <T> T roundTrip(T obj) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeObject(obj);
        }
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()))) {
            return (T) in.readObject();
        }
    }
}
