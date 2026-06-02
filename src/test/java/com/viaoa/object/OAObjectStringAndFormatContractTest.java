package com.viaoa.object;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OAObjectStringAndFormatContractTest {

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
    void toStringIsNonNullAndStableForUnchangedObject() {
        Item item = new Item();

        String a = item.toString();
        String b = item.toString();

        assertNotNull(a);
        assertEquals(a, b);
    }

    @Test
    void propertyAsStringWithNullValueReturnsProvidedNullText() {
        Item item = new Item();

        assertEquals("n/a", item.getPropertyAsString("name", null, "n/a"));
    }

    @Test
    void propertyAsStringWithFormatForPrimitiveIsDeterministic() {
        Item item = new Item();
        item.setCount(7);

        String s = item.getPropertyAsString("count", "000");

        assertEquals("007", s);
    }

    @Test
    void getPropertyAsStringDefaultFormattingBooleanIsSafe() {
        Item item = new Item();
        item.setName("A");

        assertEquals("A", item.getPropertyAsString("name", true));
        assertEquals("A", item.getPropertyAsString("name", false));
    }

    @Test
    void missingPropertyAsStringReturnsEmptyOrFailsVisibly() {
        Item item = new Item();

        try {
            assertEquals("", item.getPropertyAsString("missing"));
        } catch (RuntimeException ex) {
            assertNotNull(ex.getMessage());
        }
    }
}
