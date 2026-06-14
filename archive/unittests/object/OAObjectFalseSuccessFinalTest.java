package com.viaoa.object;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OAObjectFalseSuccessFinalTest {

    public static class Item extends OAObject {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            String old = this.name;
            if (!isValidPropertyChange("name", old, name)) return;
            this.name = name;
            firePropertyChange("name", old, name);
        }
    }

    public static class RejectingItem extends Item {
        @Override
        public boolean isValidPropertyChange(String propertyName, Object oldValue, Object newValue) {
            return false;
        }
    }

    @Test
    void rejectedPropertyChangeDoesNotPublishNewValue() {
        RejectingItem item = new RejectingItem();

        item.setName("A");

        assertNull(item.getName());
    }

    @Test
    void failedCompareAndSwapDoesNotMutateValue() {
        Item item = new Item();
        item.setName("A");

        assertFalse(item.compareAndSwap("name", "X", "B"));

        assertEquals("A", item.getName());
    }

    @Test
    void failedSaveDoesNotClearNewOrChangedFlags() {
        Item item = new Item();

        boolean wasNew = item.isNew();
        boolean wasChanged = item.isChanged();

        try {
            item.save();
        } catch (RuntimeException ex) {
            assertEquals(wasNew, item.isNew());
            assertEquals(wasChanged, item.isChanged());
            return;
        }

        assertFalse(item.isChanged());
    }

    @Test
    void failedDeleteDoesNotMarkDeletedComplete() {
        Item item = new Item();

        try {
            item.delete();
        } catch (RuntimeException ex) {
            assertFalse(item.isDeleted());
            return;
        }

        assertTrue(item.isDeleted());
    }

    @Test
    void failedSetPropertyMissingPropertyDoesNotMutateExistingState() {
        Item item = new Item();
        item.setName("A");

        try {
            item.setProperty("missing", "B");
        } catch (RuntimeException ex) {
            assertEquals("A", item.getName());
            return;
        }

        assertEquals("A", item.getName());
    }

    @Test
    void failedRemoveMissingPropertyDoesNotMutateExistingState() {
        Item item = new Item();
        item.setName("A");

        try {
            item.removeProperty("missing");
        } catch (RuntimeException ex) {
            assertEquals("A", item.getName());
            return;
        }

        assertEquals("A", item.getName());
    }
}
