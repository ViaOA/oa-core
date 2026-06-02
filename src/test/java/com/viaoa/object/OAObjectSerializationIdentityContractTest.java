package com.viaoa.object;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;

import org.junit.jupiter.api.Test;

class OAObjectSerializationIdentityContractTest {

    public static class Item extends OAObject implements Serializable {
        private static final long serialVersionUID = 1L;
        private String name;

        public String getName() { return name; }

        public void setName(String name) {
            String old = this.name;
            this.name = name;
            firePropertyChange("name", old, name);
        }
    }

    @Test
    void twoReferencesToSameObjectRemainSameAfterRoundTrip() throws Exception {
        Item item = new Item();
        item.setName("A");

        Object[] arr = new Object[] { item, item };

        Object[] copy = roundTrip(arr);

        assertSame(copy[0], copy[1]);
        assertInstanceOf(Item.class, copy[0]);
        assertEquals("A", ((Item) copy[0]).getName());
    }

    @Test
    void duplicateSerializedObjectResolvesToCanonicalIdentityWithinStream() throws Exception {
        Item item = new Item();

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeObject(item);
            out.writeObject(item);
        }

        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()))) {
            Object a = in.readObject();
            Object b = in.readObject();

            assertSame(a, b);
        }
    }

    @Test
    void roundTripPreservesLifecycleFlagsOrDocumentsRuntimeReset() throws Exception {
        Item item = new Item();
        item.setNew(false);
        item.setChanged(false);
        item.setDeleted(false);

        Item copy = roundTrip(item);

        assertEquals(item.getGuid(), copy.getGuid());
        assertFalse(copy.isNew());
        assertFalse(copy.isChanged());
        assertFalse(copy.isDeleted());
    }

    @Test
    void changedDeletedStateRoundTripsConsistently() throws Exception {
        Item item = new Item();
        item.setDeleted(true);
        item.setChanged(true);

        Item copy = roundTrip(item);

        assertEquals(item.getGuid(), copy.getGuid());
        assertEquals(item.isDeleted(), copy.isDeleted());
        assertEquals(item.isChanged(), copy.isChanged());
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
