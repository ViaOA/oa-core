package com.viaoa.object;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class OAObjectIdentityFinalContractTest {

    public static class Item extends OAObject {
        private String name;
        private int id;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            String old = this.name;
            this.name = name;
            firePropertyChange("name", old, name);
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            int old = this.id;
            this.id = id;
            firePropertyChange("id", old, id);
        }
    }

    @Test
    void guidNeverChangesAcrossPropertyAndLifecycleMutations() {
        Item item = new Item();
        UUID guid = item.getGuid();

        item.setName("A");
        item.setId(123);
        item.setChanged(false);
        item.setNew(false);
        item.setDeleted(true);
        item.setDeleted(false);

        assertEquals(guid, item.getGuid());
        assertEquals(guid, item.getObjectKey().getGuid());
    }

    @Test
    void objectKeyGuidMatchesObjectGuidAcrossLifecycleChanges() {
        Item item = new Item();

        item.setNew(false);
        item.setChanged(false);
        item.setDeleted(true);

        assertEquals(item.getGuid(), item.getObjectKey().getGuid());
    }

    @Test
    void objectKeyReturnedIdsCannotMutateKeyState() {
        OAObjectKey key = new OAObjectKey(new Object[] { "A", 1 });
        Object[] ids = key.getObjectIds();

        ids[0] = "B";

        assertArrayEquals(new Object[] { "A", 1 }, key.getObjectIds());
        assertEquals(new OAObjectKey(new Object[] { "A", 1 }), key);
    }

    @Test
    void equalityDoesNotCollapseDifferentNewInstances() {
        Item a = new Item();
        Item b = new Item();

        assertNotEquals(a, b);
        assertNotEquals(a.getGuid(), b.getGuid());
        assertNotEquals(a.getObjectKey(), b.getObjectKey());
    }

    @Test
    void objectCompareToIsAntiSymmetricForDistinctInstances() {
        Item a = new Item();
        Item b = new Item();

        int ab = a.compareTo(b);
        int ba = b.compareTo(a);

        assertNotEquals(0, ab);
        assertEquals(Integer.signum(ab), -Integer.signum(ba));
    }

    @Test
    void compareToNonObjectIsDefinedAndStable() {
        Item item = new Item();
        Object other = new Object();

        int a = item.compareTo(other);
        int b = item.compareTo(other);

        assertEquals(Integer.signum(a), Integer.signum(b));
    }

    @Test
    void hashCodeStableAcrossPropertyAndLifecycleMutations() {
        Item item = new Item();
        int hash = item.hashCode();

        item.setName("A");
        item.setId(1);
        item.setNew(false);
        item.setChanged(false);
        item.setDeleted(true);

        assertEquals(hash, item.hashCode());
    }

    @Test
    void objectKeyHashStableAcrossReturnedArrayMutation() {
        OAObjectKey key = new OAObjectKey(new Object[] { "A", 1 });
        int hash = key.hashCode();

        Object[] ids = key.getObjectIds();
        ids[0] = "B";

        assertEquals(hash, key.hashCode());
    }
}
