package com.viaoa.hub;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class HubActiveObjectAndPositionTest {

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
    void setPosChangesActiveObject() {
        Hub<Item> hub = hub();

        Item b = hub.getAt(1);

        assertSame(b, hub.setPos(1));
        assertEquals(1, hub.getPos());
        assertSame(b, hub.getAO());
        assertSame(b, hub.getActiveObject());
    }

    @Test
    void setActiveObjectByObjectChangesPosition() {
        Hub<Item> hub = hub();
        Item c = hub.getAt(2);

        hub.setActiveObject(c);

        assertEquals(2, hub.getPos());
        assertSame(c, hub.getAO());
    }

    @Test
    void setAOObjectReturnsActiveObject() {
        Hub<Item> hub = hub();
        Item c = hub.getAt(2);

        assertSame(c, hub.setAO((Object) c));

        assertEquals(2, hub.getPos());
    }

    @Test
    void setActiveObjectMissingObjectClearsOrLeavesDefinedState() {
        Hub<Item> hub = hub();
        Item missing = new Item("missing");

        hub.setActiveObject(missing);

        assertTrue(hub.getAO() == null || hub.getAO() != missing);
        assertTrue(hub.getPos() < 0 || hub.getPos() < hub.getSize());
    }

    @Test
    void setPosOutOfBoundsClearsActiveObjectOrReturnsNull() {
        Hub<Item> hub = hub();

        assertNull(hub.setPos(99));
        assertNull(hub.getAO());
        assertEquals(-1, hub.getPos());
    }

    @Test
    void resetAOClearsActiveObject() {
        Hub<Item> hub = hub();
        hub.setPos(1);

        hub.resetAO();

        assertNull(hub.getAO());
        assertEquals(-1, hub.getPos());
    }

    @Test
    void removingActiveObjectAdjustsActivePositionSafely() {
        Hub<Item> hub = hub();
        Item b = hub.getAt(1);
        hub.setAO(b);

        hub.remove(b);

        assertFalse(hub.contains(b));
        assertTrue(hub.getPos() < hub.getSize());
    }

    @Test
    void clearClearsActiveObject() {
        Hub<Item> hub = hub();
        hub.setPos(1);

        hub.clear();

        assertEquals(0, hub.getSize());
        assertNull(hub.getAO());
        assertEquals(-1, hub.getPos());
    }

    @Test
    void defaultPosRoundTrips() {
        Hub<Item> hub = hub();

        hub.setDefaultPos(2);

        assertEquals(2, hub.getDefaultPos());
    }

    @Test
    void getLastReturnsLastObjectOrNullForEmpty() {
        Hub<Item> hub = hub();

        assertSame(hub.getAt(2), hub.getLast());

        hub.clear();

        assertNull(hub.getLast());
    }
}
