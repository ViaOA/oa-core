package com.viaoa.object;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;

import org.junit.jupiter.api.Test;

class OAObjectReadResolveDeepContractTest {

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
    void readResolveCollapsesDuplicateReferencesWithinSameStream() throws Exception {
        Item item = new Item();
        item.setName("A");

        Object[] arr = new Object[] { item, item };

        Object[] copy = roundTrip(arr);

        assertSame(copy[0], copy[1]);
        assertEquals(item.getGuid(), ((Item) copy[0]).getGuid());
    }

    @Test
    void readResolveCanReturnExistingCachedObjectForSameGuidDesiredContract() throws Exception {
        Item item = new Item();
        item.setName("cached");

        // Ensure item is initialized/cached before stream read.
        item.getObjectKey();

        byte[] bytes = write(item);

        Object obj = read(bytes);

        assertInstanceOf(Item.class, obj);
        assertEquals(item.getGuid(), ((Item) obj).getGuid());
    }

    @Test
    void objectKeyAndGuidRoundTripIdentity() throws Exception {
        Item item = new Item();
        OAObjectKey key = item.getObjectKey();

        Item copy = roundTrip(item);

        assertEquals(item.getGuid(), copy.getGuid());
        assertEquals(key, copy.getObjectKey());
    }

    @Test
    void lifecycleFlagsRoundTripWithoutFalseReset() throws Exception {
        Item item = new Item();
        item.setNew(false);
        item.setChanged(false);
        item.setDeleted(true);

        Item copy = roundTrip(item);

        assertEquals(item.isNew(), copy.isNew());
        assertEquals(item.isChanged(), copy.isChanged());
        assertEquals(item.isDeleted(), copy.isDeleted());
    }

    @SuppressWarnings("unchecked")
    private static <T> T roundTrip(T obj) throws Exception {
        return (T) read(write(obj));
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
