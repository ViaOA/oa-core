package com.viaoa.object;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

class OAObjectSerializationFailureFinalTest {

    public static class Item extends OAObject implements Serializable {
        private static final long serialVersionUID = 1L;
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            String old = this.name;
            this.name = name;
            firePropertyChange("name", old, name);
        }
    }

    @Test
    void truncatedObjectPayloadFailsVisibly() throws Exception {
        Item item = new Item();
        item.setName("A");

        byte[] bytes = write(item);
        byte[] truncated = Arrays.copyOf(bytes, Math.max(1, bytes.length / 2));

        assertThrows(Exception.class, () -> read(truncated));
    }

    @Test
    void corruptObjectPayloadFailsVisiblyOrRejectsInvalidStream() throws Exception {
        Item item = new Item();
        item.setName("A");

        byte[] bytes = write(item);
        for (int i = 8; i < Math.min(bytes.length, 24); i++) {
            bytes[i] = (byte) (bytes[i] ^ 0x5A);
        }

        assertThrows(Exception.class, () -> read(bytes));
    }

    @Test
    void failedReadDoesNotAffectLaterIndependentRead() throws Exception {
        Item badSource = new Item();
        badSource.setName("bad");

        byte[] bad = write(badSource);
        byte[] truncated = Arrays.copyOf(bad, Math.max(1, bad.length / 2));

        assertThrows(Exception.class, () -> read(truncated));

        Item good = new Item();
        good.setName("good");

        Item copy = (Item) read(write(good));

        assertEquals("good", copy.getName());
        assertEquals(good.getGuid(), copy.getGuid());
    }

    @Test
    void failedWriteFromNonSerializablePropertyPropagates() {
        class BadItem extends OAObject {
            private Object value = new Object();
            public Object getValue() { return value; }
        }

        BadItem item = new BadItem();

        assertThrows(Exception.class, () -> write(item));
    }

    @Test
    void objectKeySerializationFailureForBadIdPropagates() {
        OAObjectKey key = new OAObjectKey(new Object[] { new Object() });

        assertThrows(NotSerializableException.class, () -> write(key));
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
