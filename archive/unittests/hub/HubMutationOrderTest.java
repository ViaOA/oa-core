package com.viaoa.hub;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class HubMutationOrderTest {

    public static class Item extends OAObject {
        private String name;
        public Item() { }
        public Item(String name) { this.name = name; }
        public String getName() { return name; }
    }

    private static Hub<Item> hub() {
        Hub<Item> hub = new Hub<>(Item.class);
        hub.add(new Item("A"));
        hub.add(new Item("B"));
        hub.add(new Item("C"));
        return hub;
    }

    @Test
    void swapExchangesPositions() {
        Hub<Item> hub = hub();
        Item a = hub.getAt(0);
        Item c = hub.getAt(2);

        hub.swap(0, 2);

        assertSame(c, hub.getAt(0));
        assertSame(a, hub.getAt(2));
    }

    @Test
    void moveRepositionsObjectAndPreservesAllMembers() {
        Hub<Item> hub = hub();
        Item a = hub.getAt(0);
        Item b = hub.getAt(1);
        Item c = hub.getAt(2);

        hub.move(0, 2);

        assertEquals(List.of(b, c, a), hub.toList());
    }

    @Test
    void replaceChangesObjectAtPosition() {
        Hub<Item> hub = hub();
        Item x = new Item("X");

        hub.replace(1, x);

        assertSame(x, hub.getAt(1));
        assertEquals(3, hub.getSize());
    }

    @Test
    void removeByIndexAndRemoveAtBehaveConsistently() {
        Hub<Item> hub = hub();
        Item b = hub.getAt(1);

        assertSame(b, hub.remove(1));

        assertEquals(2, hub.getSize());
        assertFalse(hub.contains(b));
    }

    @Test
    void removeAllAliasClearsHub() {
        Hub<Item> hub = hub();

        hub.removeAll();

        assertTrue(hub.isEmpty());
        assertEquals(0, hub.getSize());
    }

    @Test
    void copyIntoHubAppendsCurrentMembership() {
        Hub<Item> source = hub();
        Hub<Item> target = new Hub<>(Item.class);

        source.copyInto(target);

        assertEquals(source.toList(), target.toList());
    }

    @Test
    void cloneCreatesIndependentHubWithSameMembership() throws Exception {
        Hub<Item> hub = hub();

        @SuppressWarnings("unchecked")
        Hub<Item> clone = (Hub<Item>) hub.clone();

        assertNotSame(hub, clone);
        assertEquals(hub.toList(), clone.toList());

        clone.removeAt(0);

        assertEquals(3, hub.getSize());
        assertEquals(2, clone.getSize());
    }

    @Test
    void enabledFlagRoundTrips() {
        Hub<Item> hub = hub();

        assertTrue(hub.getEnabled());

        hub.setEnabled(false);
        assertFalse(hub.getEnabled());

        hub.setEnabled(true);
        assertTrue(hub.getEnabled());
    }
}
