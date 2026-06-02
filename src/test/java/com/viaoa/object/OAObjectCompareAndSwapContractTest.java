package com.viaoa.object;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OAObjectCompareAndSwapContractTest {

    public static class Item extends OAObject {
        private String name;
        private int count;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            String old = this.name;
            this.name = name;
            firePropertyChange("name", old, name);
        }

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            int old = this.count;
            this.count = count;
            firePropertyChange("count", old, count);
        }
    }

    @Test
    void compareAndSwapUpdatesWhenExpectedOldValueMatches() {
        Item item = new Item();
        item.setName("A");

        boolean ok = item.compareAndSwap("name", "A", "B");

        assertTrue(ok);
        assertEquals("B", item.getName());
    }

    @Test
    void compareAndSwapDoesNotUpdateWhenExpectedOldValueDiffers() {
        Item item = new Item();
        item.setName("A");

        boolean ok = item.compareAndSwap("name", "X", "B");

        assertFalse(ok);
        assertEquals("A", item.getName());
    }

    @Test
    void compareAndSwapSupportsNullOldValue() {
        Item item = new Item();

        assertTrue(item.compareAndSwap("name", null, "A"));
        assertEquals("A", item.getName());
    }

    @Test
    void compareAndSwapSupportsPrimitiveBoxedValues() {
        Item item = new Item();
        item.setCount(1);

        assertTrue(item.compareAndSwap("count", 1, 2));
        assertEquals(2, item.getCount());

        assertFalse(item.compareAndSwap("count", 1, 3));
        assertEquals(2, item.getCount());
    }

    @Test
    void compareAndSwapMissingPropertyFailsOrReturnsFalseWithoutMutation() {
        Item item = new Item();
        item.setName("A");

        try {
            assertFalse(item.compareAndSwap("missing", null, "B"));
            assertEquals("A", item.getName());
        } catch (RuntimeException ex) {
            assertNotNull(ex.getMessage());
            assertEquals("A", item.getName());
        }
    }
}
