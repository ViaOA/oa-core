package com.viaoa.object;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;

import org.junit.jupiter.api.Test;

class OAObjectSerializationSmokeTest {

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
    void javaSerializationRoundTripPreservesGuidAndProperty() throws Exception {
        Item item = new Item();
        item.setName("A");

        Item copy = roundTrip(item);

        assertNotNull(copy);
        assertEquals(item.getGuid(), copy.getGuid());
        assertEquals("A", copy.getName());
    }

    @Test
    void objectKeyRoundTripsWithOAObject() throws Exception {
        Item item = new Item();

        Item copy = roundTrip(item);

        assertEquals(item.getObjectKey(), copy.getObjectKey());
    }

    @Test
    void serializationDoesNotClearRuntimeIdentityOfOriginal() throws Exception {
        Item item = new Item();
        var guid = item.getGuid();

        roundTrip(item);

        assertEquals(guid, item.getGuid());
        assertEquals(guid, item.getObjectKey().getGuid());
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
