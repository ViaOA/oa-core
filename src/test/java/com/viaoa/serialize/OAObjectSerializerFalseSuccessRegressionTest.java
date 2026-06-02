package com.viaoa.serialize;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;

import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OAObjectSerializerFalseSuccessRegressionTest {

    public static class Item extends OAObject implements Serializable {
        private static final long serialVersionUID = 1L;
        private String name;

        public Item() {
        }

        public Item(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    static class BadReadObject implements Serializable {
        private static final long serialVersionUID = 1L;

        private void readObject(ObjectInputStream in) throws IOException {
            throw new IOException("read failed");
        }
    }

    @Test
    void deserializeFailureDoesNotReturnPartiallyValidWrapper() throws Exception {
        byte[] payload = write(new OAObjectSerializer<>(new BadReadObject(), false));

        assertThrows(IOException.class, () -> read(payload));
    }

    @Test
    void corruptCompressionFlagOrPayloadFailsVisibly() throws Exception {
        byte[] payload = write(new OAObjectSerializer<>(new Item("A"), true));

        // Corrupt several bytes after stream header to maximize chance of hitting wrapper data while keeping stream header valid.
        for (int i = 8; i < Math.min(payload.length, 20); i++) {
            payload[i] = (byte) (payload[i] ^ 0x55);
        }

        assertThrows(Exception.class, () -> read(payload));
    }

    @Test
    void failedPayloadReadDoesNotAffectLaterIndependentRead() throws Exception {
        byte[] bad = write(new OAObjectSerializer<>(new BadReadObject(), false));
        assertThrows(IOException.class, () -> read(bad));

        OAObjectSerializer<Item> good = (OAObjectSerializer<Item>) read(write(new OAObjectSerializer<>(new Item("good"), false)));

        assertEquals("good", good.getObject().getName());
    }

    @Test
    void nonSerializedRuntimeFlagsDoNotCreateFalseAssuranceAfterRead() throws Exception {
        OAObjectSerializer<Item> ser = new OAObjectSerializer<>(new Item("A"), false);
        ser.setMax(1);
        ser.setMaxSize(1);
        ser.setIncludeBlobs(true);

        OAObjectSerializer<Item> copy = (OAObjectSerializer<Item>) read(write(ser));

        assertEquals("A", copy.getObject().getName());
        assertEquals(0, copy.getMax());
        assertEquals(0, copy.getMaxSize());
        assertFalse(copy.getIncludeBlobs());
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
