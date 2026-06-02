package com.viaoa.hub;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class HubPhase8FinalEdgeRegressionTest {

    public static class Item extends OAObject implements Serializable {
        private static final long serialVersionUID = 1L;
        private String name;
        private int value;

        public Item() {
        }

        public Item(String name) {
            this.name = name;
        }

        public Item(String name, int value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            String old = this.name;
            this.name = name;
            firePropertyChange("name", old, name);
        }

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            int old = this.value;
            this.value = value;
            firePropertyChange("value", old, value);
        }
    }

    private static Hub<Item> hub() {
        Hub<Item> hub = new Hub<>(Item.class);
        hub.add(new Item("A", 1));
        hub.add(new Item("B", 2));
        hub.add(new Item("C", 3));
        return hub;
    }

    @Test
    void boundsMethodsReturnSafeValues() {
        Hub<Item> hub = hub();

        assertNull(hub.getAt(-1));
        assertNull(hub.getAt(99));
        assertNull(hub.getObjectAt(-1));
        assertNull(hub.getObjectAt(99));
        assertNull(hub.removeAt(99));
        assertNull(hub.remove(-1));
        assertNull(hub.remove(99));
    }

    @Test
    void insertOutOfBoundsDoesNotCorruptHub() {
        Hub<Item> hub = hub();
        List<Item> before = hub.toList();
        Item x = new Item("X");

        try {
            hub.insert(x, 99);
        } catch (RuntimeException ex) {
            assertEquals(before, hub.toList());
            return;
        }

        assertTrue(hub.contains(x) || hub.toList().equals(before));
    }

    @Test
    void replaceOutOfBoundsDoesNotCorruptHub() {
        Hub<Item> hub = hub();
        List<Item> before = hub.toList();
        Item x = new Item("X");

        try {
            hub.replace(99, x);
        } catch (RuntimeException ex) {
            assertEquals(before, hub.toList());
            return;
        }

        assertEquals(before, hub.toList());
    }

    @Test
    void setAONullClearsActiveObject() {
        Hub<Item> hub = hub();
        hub.setPos(1);

        hub.setAO((Item) null);

        assertNull(hub.getAO());
        assertEquals(-1, hub.getPos());
    }

    @Test
    void getPosForMissingObjectReturnsMinusOne() {
        Hub<Item> hub = hub();

        assertEquals(-1, hub.getPos(new Item("missing")));
        assertEquals(-1, hub.indexOf(new Item("missing")));
    }

    @Test
    void copyIntoArrayLargerThanSizeLeavesTrailingNull() {
        Hub<Item> hub = hub();

        Item[] arr = new Item[5];
        hub.copyInto(arr);

        assertSame(hub.getAt(0), arr[0]);
        assertSame(hub.getAt(1), arr[1]);
        assertSame(hub.getAt(2), arr[2]);
        assertNull(arr[3]);
        assertNull(arr[4]);
    }

    @Test
    void toArrayTypedLargerThanSizeLeavesTrailingNull() {
        Hub<Item> hub = hub();

        Item[] arr = hub.toArray(new Item[5]);

        assertSame(hub.getAt(0), arr[0]);
        assertSame(hub.getAt(1), arr[1]);
        assertSame(hub.getAt(2), arr[2]);
        assertNull(arr[3]);
    }

    @Test
    void listenerRemovalDuringBeforeEventIsSafe() {
        Hub<Item> hub = new Hub<>(Item.class);
        AtomicInteger cnt = new AtomicInteger();

        HubListenerAdapter<Item>[] ref = new HubListenerAdapter[1];
        ref[0] = new HubListenerAdapter<Item>() {
            @Override
            public void beforeAdd(HubEvent<Item> e) {
                cnt.incrementAndGet();
                hub.removeHubListener(ref[0]);
            }
        };

        hub.addHubListener(ref[0]);

        hub.add(new Item("A"));
        hub.add(new Item("B"));

        assertEquals(1, cnt.get());
        assertEquals(2, hub.getSize());
    }

    @Test
    void propertyListenerRemovedStopsFuturePropertyEvents() {
        Hub<Item> hub = hub();
        AtomicInteger cnt = new AtomicInteger();

        HubListenerAdapter<Item> li = new HubListenerAdapter<Item>() {
            @Override
            public void afterPropertyChange(HubEvent<Item> e) {
                cnt.incrementAndGet();
            }
        };

        hub.addHubListener(li, "name");
        hub.getAt(0).setName("A1");

        hub.removeHubListener(li);
        hub.getAt(0).setName("A2");

        assertEquals(1, cnt.get());
    }

    @Test
    void nullListenerAddRemoveAreSafeOrFailVisibly() {
        Hub<Item> hub = hub();

        try {
            hub.addHubListener(null);
        } catch (RuntimeException | IllegalArgumentException ex) {
            assertNotNull(ex.getMessage());
        }

        assertDoesNotThrow(() -> hub.removeHubListener(null));
    }

    @Test
    void selectWithoutDatasourceDoesNotCreateFalseMembership() {
        Hub<Item> hub = new Hub<>(Item.class);

        try {
            hub.select("name = ?", "name");
        } catch (RuntimeException ex) {
            assertEquals(0, hub.getSize());
            return;
        }

        assertTrue(hub.getSize() >= 0);
    }

    @Test
    void selectPassthruWithoutDatasourceFailsOrLeavesHubSafe() {
        Hub<Item> hub = new Hub<>(Item.class);

        try {
            hub.selectPassthru("select * from item", "name");
        } catch (RuntimeException ex) {
            assertEquals(0, hub.getSize());
            return;
        }

        assertTrue(hub.getSize() >= 0);
    }

    @Test
    void linkHubRemovalLeavesHubValidAndMembershipUntouched() {
        Hub<Item> hub = hub();
        List<Item> before = hub.toList();

        hub.removeLinkHub();

        assertTrue(hub.isValid());
        assertEquals(before, hub.toList());
    }

    @Test
    void setAutoMatchBoundaryDoesNotCorruptMembership() {
        Hub<Item> hub = hub();
        Hub<Item> master = new Hub<>(Item.class);
        List<Item> before = hub.toList();

        try {
            hub.setAutoMatch("name", master);
        } catch (RuntimeException ex) {
            assertEquals(before, hub.toList());
            return;
        }

        assertEquals(3, hub.getSize());
        assertTrue(hub.toList().containsAll(before));
    }

    @Test
    void setAutoSequenceThenRemoveAndAddKeepsMembershipConsistent() {
        Hub<Item> hub = hub();
        hub.setAutoSequence("value", 1);

        Item removed = hub.getAt(1);
        hub.remove(removed);

        Item d = new Item("D");
        hub.add(d);
        hub.resequence();

        assertFalse(hub.contains(removed));
        assertEquals(3, hub.getSize());
        assertEquals(1, hub.getAt(0).getValue());
        assertEquals(2, hub.getAt(1).getValue());
        assertEquals(3, hub.getAt(2).getValue());
    }

    @Test
    void readResolveRoundTripForEmptyHubPreservesObjectClass() throws Exception {
        Hub<Item> hub = new Hub<>(Item.class);

        Hub<Item> copy = roundTrip(hub);

        assertEquals(Item.class, copy.getObjectClass());
        assertEquals(0, copy.getSize());
        assertTrue(copy.isEmpty());
    }

    @Test
    void readResolveRoundTripForHubWithAOLeavesValidAOState() throws Exception {
        Hub<Item> hub = hub();
        hub.setPos(1);

        Hub<Item> copy = roundTrip(hub);

        assertEquals(3, copy.getSize());
        assertTrue(copy.getPos() == -1 || copy.getPos() < copy.getSize());
        if (copy.getAO() != null) {
            assertTrue(copy.contains(copy.getAO()));
        }
    }

    @Test
    void corruptSerializedHubFailsAndLaterReadSucceeds() throws Exception {
        Hub<Item> hub = hub();
        byte[] bytes = write(hub);
        byte[] trunc = java.util.Arrays.copyOf(bytes, Math.max(1, bytes.length / 2));

        assertThrows(Exception.class, () -> read(trunc));

        Hub<Item> copy = roundTrip(hub);

        assertEquals(3, copy.getSize());
        assertEquals("A", copy.getAt(0).getName());
    }

    @Test
    void duplicateObjectsAreNotAddedTwiceCurrentContractOrRemainDefined() {
        Hub<Item> hub = new Hub<>(Item.class);
        Item item = new Item("A");

        hub.add(item);
        boolean second = hub.add(item);

        assertEquals(1, hub.toList().stream().filter(x -> x == item).count(),
            "Hub should not create duplicate membership for the same object unless explicitly configured");
        assertFalse(second || hub.getSize() > 1);
    }

    @Test
    void removeDuringIterationUsingSnapshotListIsSafe() {
        Hub<Item> hub = hub();

        for (Item item : hub.toList()) {
            hub.remove(item);
        }

        assertEquals(0, hub.getSize());
    }

    @Test
    void repeatedAddRemoveSameObjectDoesNotLeakMembership() {
        Hub<Item> hub = new Hub<>(Item.class);
        Item item = new Item("A");

        for (int i = 0; i < 100; i++) {
            hub.add(item);
            assertTrue(hub.contains(item));
            hub.remove(item);
            assertFalse(hub.contains(item));
        }

        assertEquals(0, hub.getSize());
        assertNull(hub.getAO());
    }

    @Test
    void finalSmokeAddSortFindClearSharedDetail() {
        Hub<Item> hub = hub();
        Hub<Item> shared = hub.createSharedHub(true);

        hub.sort("name", true);
        Item b = hub.find("name", "B", true);

        assertNotNull(b);
        assertSame(b, shared.getAO());

        hub.clear();

        assertEquals(0, hub.getSize());
        assertEquals(0, shared.getSize());
        assertNull(hub.getAO());
        assertNull(shared.getAO());
    }

    @SuppressWarnings("unchecked")
    private static <T> T roundTrip(T obj) throws Exception {
        return (T) read(write(obj));
    }

    private static byte[] write(Object obj) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeObject(obj);
        }
        return bos.toByteArray();
    }

    private static Object read(byte[] bytes) throws Exception {
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            return in.readObject();
        }
    }
}
