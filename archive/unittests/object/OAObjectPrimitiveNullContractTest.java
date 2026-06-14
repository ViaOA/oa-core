package com.viaoa.object;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OAObjectPrimitiveNullContractTest {

    public static class Item extends OAObject {
        private int count;
        private boolean active;

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            int old = this.count;
            this.count = count;
            firePropertyChange("count", old, count);
        }

        public boolean getActive() {
            return active;
        }

        public void setActive(boolean active) {
            boolean old = this.active;
            this.active = active;
            firePropertyChange("active", old, active);
        }
    }

    @Test
    void primitiveNullCanDistinguishUnsetFromZeroDesiredContract() {
        Item item = new Item();

        item.setNull("count");

        assertTrue(item.isNull("count"),
            "primitive null must be distinct from primitive default value");
        assertEquals(0, item.getCount());
    }

    @Test
    void settingPrimitiveValueClearsPrimitiveNullDesiredContract() {
        Item item = new Item();

        item.setNull("count");
        item.setCount(0);

        assertFalse(item.isNull("count"),
            "explicit primitive assignment should clear primitive-null state even when value is default");
    }

    @Test
    void booleanPrimitiveNullCanBeTrackedSeparatelyFromFalseDesiredContract() {
        Item item = new Item();

        item.setNull("active");

        assertTrue(item.isNull("active"));
        assertFalse(item.getActive());

        item.setActive(false);

        assertFalse(item.isNull("active"));
    }

    @Test
    void referencePropertyNullIsReportedAsNull() {
        class RefItem extends OAObject {
            private String name;
            public String getName() { return name; }
            public void setName(String name) {
                String old = this.name;
                this.name = name;
                firePropertyChange("name", old, name);
            }
        }

        RefItem item = new RefItem();

        assertTrue(item.isNull("name"));

        item.setName("A");
        assertFalse(item.isNull("name"));

        item.setNull("name");
        assertTrue(item.isNull("name"));
    }

    @Test
    void missingPropertyNullCheckReturnsTrueOrFailsVisibly() {
        Item item = new Item();

        try {
            assertTrue(item.isNull("missing"));
        } catch (RuntimeException ex) {
            assertNotNull(ex.getMessage());
        }
    }
}
