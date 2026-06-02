package com.viaoa.hub;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class HubCoreConstructionTest {

    public static class Item extends OAObject {
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

    @Test
    void classConstructorSetsObjectClassAndStartsEmpty() {
        Hub<Item> hub = new Hub<>(Item.class);

        assertEquals(Item.class, hub.getObjectClass());
        assertEquals(0, hub.getSize());
        assertEquals(0, hub.size());
        assertTrue(hub.isEmpty());
        assertNull(hub.getAO());
        assertEquals(-1, hub.getPos());
    }

    @Test
    void objectConstructorInfersObjectClassAndAddsObject() {
        Item item = new Item("A");

        Hub<Item> hub = new Hub<>(item);

        assertEquals(Item.class, hub.getObjectClass());
        assertEquals(1, hub.getSize());
        assertSame(item, hub.getAt(0));
        assertTrue(hub.contains(item));
    }

    @Test
    void defaultConstructorAllowsUnspecifiedObjectClass() {
        Hub<OAObject> hub = new Hub<>();

        assertNull(hub.getObjectClass());
        assertEquals(0, hub.getSize());
        assertTrue(hub.isEmpty());
    }

    @Test
    void ensureCapacityAndResizeToFitAreSafeNoopsForEmptyHub() {
        Hub<Item> hub = new Hub<>(Item.class);

        assertDoesNotThrow(() -> hub.ensureCapacity(100));
        assertDoesNotThrow(hub::resizeToFit);

        assertEquals(0, hub.getSize());
    }

    @Test
    void propertyBagRoundTripsAndRemovesValues() {
        Hub<Item> hub = new Hub<>(Item.class);

        hub.setProperty("x", "value");

        assertEquals("value", hub.getProperty("x"));

        hub.removeProperty("x");

        assertNull(hub.getProperty("x"));
    }

    @Test
    void toStringIsNonNullAndStableEnoughForEmptyHub() {
        Hub<Item> hub = new Hub<>(Item.class);

        assertNotNull(hub.toString());
        assertEquals(hub.toString(), hub.toString());
    }

    @Test
    void toArrayAndToListReflectCurrentMembershipOrder() {
        Hub<Item> hub = new Hub<>(Item.class);
        Item a = new Item("A");
        Item b = new Item("B");

        hub.add(a);
        hub.add(b);

        Object[] arr = hub.toArray();
        List<Item> list = hub.toList();

        assertArrayEquals(new Object[] { a, b }, arr);
        assertEquals(List.of(a, b), list);
    }

    @Test
    void copyIntoArrayPreservesOrder() {
        Hub<Item> hub = new Hub<>(Item.class);
        Item a = new Item("A");
        Item b = new Item("B");
        hub.add(a);
        hub.add(b);

        Item[] arr = new Item[2];
        hub.copyInto(arr);

        assertArrayEquals(new Item[] { a, b }, arr);
    }

    @Test
    void typedToArrayPreservesOrder() {
        Hub<Item> hub = new Hub<>(Item.class);
        Item a = new Item("A");
        Item b = new Item("B");
        hub.add(a);
        hub.add(b);

        Item[] arr = hub.toArray(new Item[0]);

        assertArrayEquals(new Item[] { a, b }, arr);
    }
}
