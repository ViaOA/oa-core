package com.viaoa.object;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OAObjectPropertyBasicTest {

    public static class Item extends OAObject {
        private String name;
        private int count;
        private boolean active;

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
    void getPropertyFindsBeanGetterValue() {
        Item item = new Item();
        item.setName("A");

        assertEquals("A", item.getProperty("name"));
    }

    @Test
    void setPropertyUsesBeanSetterValue() {
        Item item = new Item();

        item.setProperty("name", "A");

        assertEquals("A", item.getName());
        assertEquals("A", item.getProperty("name"));
    }

    @Test
    void primitiveSetPropertyOverloadsUseBoxedValues() {
        Item item = new Item();

        item.setProperty("count", 5);
        item.setProperty("active", true);

        assertEquals(5, item.getCount());
        assertTrue(item.getActive());
    }

    @Test
    void setNullClearsReferenceProperty() {
        Item item = new Item();
        item.setName("A");

        item.setNull("name");

        assertNull(item.getName());
    }

    @Test
    void removePropertyClearsReferenceProperty() {
        Item item = new Item();
        item.setName("A");

        item.removeProperty("name");

        assertNull(item.getName());
    }

    @Test
    void propertyAsStringReturnsEmptyForNullValue() {
        Item item = new Item();

        assertEquals("", item.getPropertyAsString("name"));
        assertEquals("NULL", item.getPropertyAsString("name", null, "NULL"));
    }

    @Test
    void propertyAsStringFormatsPrimitiveValue() {
        Item item = new Item();
        item.setCount(7);

        assertEquals("7", item.getPropertyAsString("count"));
    }

    @Test
    void missingPropertyReturnsNullOrFailsVisiblyCurrentContract() {
        Item item = new Item();

        try {
            assertNull(item.getProperty("missing"));
        } catch (RuntimeException ex) {
            assertNotNull(ex.getMessage());
        }
    }

    @Test
    void isValidPropertyChangeDefaultsToTrueForSimpleModel() {
        Item item = new Item();

        assertTrue(item.isValidPropertyChange("name", null, "A"));
        assertTrue(item.isValidPropertyChange("name", "A"));
    }
}
