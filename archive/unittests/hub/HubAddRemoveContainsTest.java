package com.viaoa.hub;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class HubAddRemoveContainsTest {

    public static class Item extends OAObject {
        private String name;
        public Item() { }
        public Item(String name) { this.name = name; }
        public String getName() { return name; }
    }

    @Test
    void addAppendsObjectAndContainsByIdentity() {
        Hub<Item> hub = new Hub<>(Item.class);
        Item a = new Item("A");

        assertTrue(hub.add(a));

        assertEquals(1, hub.getSize());
        assertSame(a, hub.getAt(0));
        assertTrue(hub.contains(a));
        assertEquals(0, hub.indexOf(a));
    }

    @Test
    void addNullIsRejectedOrIgnoredWithoutChangingHub() {
        Hub<Item> hub = new Hub<>(Item.class);

        try {
            assertFalse(hub.add(null));
        } catch (RuntimeException | IllegalArgumentException ex) {
            assertNotNull(ex.getMessage());
        }

        assertEquals(0, hub.getSize());
    }

    @Test
    void addListAppendsAllInOrder() {
        Hub<Item> hub = new Hub<>(Item.class);
        Item a = new Item("A");
        Item b = new Item("B");

        hub.add(List.of(a, b));

        assertEquals(2, hub.getSize());
        assertSame(a, hub.getAt(0));
        assertSame(b, hub.getAt(1));
    }

    @Test
    void addHubAppendsAllInOrder() {
        Hub<Item> source = new Hub<>(Item.class);
        Item a = new Item("A");
        Item b = new Item("B");
        source.add(a);
        source.add(b);

        Hub<Item> target = new Hub<>(Item.class);
        target.add(source);

        assertEquals(2, target.getSize());
        assertSame(a, target.getAt(0));
        assertSame(b, target.getAt(1));
    }

    @Test
    void insertPlacesObjectAtPosition() {
        Hub<Item> hub = new Hub<>(Item.class);
        Item a = new Item("A");
        Item b = new Item("B");
        Item c = new Item("C");

        hub.add(a);
        hub.add(c);

        assertTrue(hub.insert(b, 1));

        assertEquals(List.of(a, b, c), hub.toList());
    }

    @Test
    void removeByObjectRemovesOnlyThatObject() {
        Hub<Item> hub = new Hub<>(Item.class);
        Item a = new Item("A");
        Item b = new Item("B");
        hub.add(a);
        hub.add(b);

        assertTrue(hub.remove(a));

        assertEquals(1, hub.getSize());
        assertFalse(hub.contains(a));
        assertTrue(hub.contains(b));
        assertSame(b, hub.getAt(0));
    }

    @Test
    void removeMissingObjectReturnsFalseAndDoesNotMutate() {
        Hub<Item> hub = new Hub<>(Item.class);
        Item a = new Item("A");
        Item b = new Item("B");
        hub.add(a);

        assertFalse(hub.remove(b));

        assertEquals(1, hub.getSize());
        assertSame(a, hub.getAt(0));
    }

    @Test
    void removeAtReturnsRemovedObjectAndShiftsRemaining() {
        Hub<Item> hub = new Hub<>(Item.class);
        Item a = new Item("A");
        Item b = new Item("B");
        hub.add(a);
        hub.add(b);

        assertSame(a, hub.removeAt(0));

        assertEquals(1, hub.getSize());
        assertSame(b, hub.getAt(0));
    }

    @Test
    void clearRemovesAllObjects() {
        Hub<Item> hub = new Hub<>(Item.class);
        hub.add(new Item("A"));
        hub.add(new Item("B"));

        hub.clear();

        assertEquals(0, hub.getSize());
        assertTrue(hub.isEmpty());
        assertNull(hub.getAt(0));
    }

    @Test
    void getAtBoundsReturnNull() {
        Hub<Item> hub = new Hub<>(Item.class);
        hub.add(new Item("A"));

        assertNull(hub.getAt(-1));
        assertNull(hub.getAt(1));
    }
}
