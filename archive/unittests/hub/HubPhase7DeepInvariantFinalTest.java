package com.viaoa.hub;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class HubPhase7DeepInvariantFinalTest {

    public static class Item extends OAObject implements Serializable {
        private static final long serialVersionUID = 1L;
        private String name;
        private int seq;
        private Group group;

        public Item() {
        }

        public Item(String name) {
            this.name = name;
        }

        public Item(String name, int seq) {
            this.name = name;
            this.seq = seq;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            String old = this.name;
            this.name = name;
            firePropertyChange("name", old, name);
        }

        public int getSeq() {
            return seq;
        }

        public void setSeq(int seq) {
            int old = this.seq;
            this.seq = seq;
            firePropertyChange("seq", old, seq);
        }

        public Group getGroup() {
            return group;
        }

        public void setGroup(Group group) {
            Group old = this.group;
            this.group = group;
            firePropertyChange("group", old, group);
        }
    }

    public static class Group extends OAObject implements Serializable {
        private static final long serialVersionUID = 1L;
        private String name;
        private final Hub<Item> items = new Hub<>(Item.class);

        public Group() {
        }

        public Group(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public Hub<Item> getItems() {
            return items;
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
    void beforeAddFailureLeavesNoMembershipFalseSuccess() {
        Hub<Item> hub = new Hub<>(Item.class);
        Item item = new Item("A");

        hub.addHubListener(new HubListenerAdapter<Item>() {
            @Override
            public void beforeAdd(HubEvent<Item> e) {
                throw new RuntimeException("before add");
            }
        });

        RuntimeException ex = assertThrows(RuntimeException.class, () -> hub.add(item));

        assertEquals("before add", ex.getMessage());
        assertFalse(hub.contains(item));
        assertEquals(0, hub.getSize());
    }

    @Test
    void afterAddFailureLeavesCompletedMembershipVisible() {
        Hub<Item> hub = new Hub<>(Item.class);
        Item item = new Item("A");

        hub.addHubListener(new HubListenerAdapter<Item>() {
            @Override
            public void afterAdd(HubEvent<Item> e) {
                throw new RuntimeException("after add");
            }
        });

        RuntimeException ex = assertThrows(RuntimeException.class, () -> hub.add(item));

        assertEquals("after add", ex.getMessage());
        assertTrue(hub.contains(item));
        assertEquals(1, hub.getSize());
    }

    @Test
    void beforeRemoveFailureLeavesMembershipIntact() {
        Hub<Item> hub = hub();
        Item item = hub.getAt(0);

        hub.addHubListener(new HubListenerAdapter<Item>() {
            @Override
            public void beforeRemove(HubEvent<Item> e) {
                throw new RuntimeException("before remove");
            }
        });

        RuntimeException ex = assertThrows(RuntimeException.class, () -> hub.remove(item));

        assertEquals("before remove", ex.getMessage());
        assertTrue(hub.contains(item));
        assertEquals(3, hub.getSize());
    }

    @Test
    void afterRemoveFailureLeavesRemovalCompletedVisible() {
        Hub<Item> hub = hub();
        Item item = hub.getAt(0);

        hub.addHubListener(new HubListenerAdapter<Item>() {
            @Override
            public void afterRemove(HubEvent<Item> e) {
                throw new RuntimeException("after remove");
            }
        });

        RuntimeException ex = assertThrows(RuntimeException.class, () -> hub.remove(item));

        assertEquals("after remove", ex.getMessage());
        assertFalse(hub.contains(item));
        assertEquals(2, hub.getSize());
    }

    @Test
    void beforeInsertFailureLeavesOrderUnchanged() {
        Hub<Item> hub = hub();
        List<Item> before = hub.toList();
        Item x = new Item("X");

        hub.addHubListener(new HubListenerAdapter<Item>() {
            @Override
            public void beforeInsert(HubEvent<Item> e) {
                throw new RuntimeException("before insert");
            }
        });

        RuntimeException ex = assertThrows(RuntimeException.class, () -> hub.insert(x, 1));

        assertEquals("before insert", ex.getMessage());
        assertEquals(before, hub.toList());
        assertFalse(hub.contains(x));
    }

    @Test
    void afterInsertFailureLeavesInsertCompletedVisible() {
        Hub<Item> hub = hub();
        Item x = new Item("X");

        hub.addHubListener(new HubListenerAdapter<Item>() {
            @Override
            public void afterInsert(HubEvent<Item> e) {
                throw new RuntimeException("after insert");
            }
        });

        RuntimeException ex = assertThrows(RuntimeException.class, () -> hub.insert(x, 1));

        assertEquals("after insert", ex.getMessage());
        assertSame(x, hub.getAt(1));
        assertTrue(hub.contains(x));
    }

    @Test
    void beforeReplaceFailureLeavesOldObjectAtPosition() {
        Hub<Item> hub = hub();
        Item old = hub.getAt(1);
        Item x = new Item("X");

        hub.addHubListener(new HubListenerAdapter<Item>() {
            @Override
            public void beforeReplace(HubEvent<Item> e) {
                throw new RuntimeException("before replace");
            }
        });

        RuntimeException ex = assertThrows(RuntimeException.class, () -> hub.replace(1, x));

        assertEquals("before replace", ex.getMessage());
        assertSame(old, hub.getAt(1));
        assertFalse(hub.contains(x));
    }

    @Test
    void afterReplaceFailureLeavesReplacementCompletedVisible() {
        Hub<Item> hub = hub();
        Item old = hub.getAt(1);
        Item x = new Item("X");

        hub.addHubListener(new HubListenerAdapter<Item>() {
            @Override
            public void afterReplace(HubEvent<Item> e) {
                throw new RuntimeException("after replace");
            }
        });

        RuntimeException ex = assertThrows(RuntimeException.class, () -> hub.replace(1, x));

        assertEquals("after replace", ex.getMessage());
        assertSame(x, hub.getAt(1));
        assertFalse(hub.contains(old));
    }

    @Test
    void invalidMoveDoesNotCorruptOrder() {
        Hub<Item> hub = hub();
        List<Item> before = hub.toList();

        try {
            hub.move(-1, 2);
        } catch (RuntimeException ex) {
            assertEquals(before, hub.toList());
            return;
        }

        assertEquals(before, hub.toList());
    }

    @Test
    void invalidSwapDoesNotCorruptOrder() {
        Hub<Item> hub = hub();
        List<Item> before = hub.toList();

        try {
            hub.swap(0, 99);
        } catch (RuntimeException ex) {
            assertEquals(before, hub.toList());
            return;
        }

        assertEquals(before, hub.toList());
    }

    @Test
    void failedSortInvalidPropertyDoesNotLoseMembership() {
        Hub<Item> hub = hub();
        List<Item> before = hub.toList();

        try {
            hub.sort("missing.property", true);
        } catch (RuntimeException ex) {
            assertEquals(3, hub.getSize());
            assertTrue(hub.toList().containsAll(before));
            return;
        }

        assertEquals(3, hub.getSize());
        assertTrue(hub.toList().containsAll(before));
    }

    @Test
    void detailHubAfterMasterClearHasNoStaleRows() {
        Hub<Group> groups = new Hub<>(Group.class);
        Group g = new Group("G");
        Item a = new Item("A");
        a.setGroup(g);
        g.getItems().add(a);
        groups.add(g);

        Hub<Item> detail = groups.getDetailHub("items");
        groups.setAO(g);

        assertEquals(1, detail.getSize());

        groups.clear();

        assertEquals(0, detail.getSize());
        assertNull(detail.getAO());
    }

    @Test
    void detailHubAfterMasterAOChangeDoesNotRetainOldChildAO() {
        Hub<Group> groups = new Hub<>(Group.class);

        Group g1 = new Group("G1");
        Item a = new Item("A");
        g1.getItems().add(a);

        Group g2 = new Group("G2");
        Item b = new Item("B");
        g2.getItems().add(b);

        groups.add(g1);
        groups.add(g2);

        Hub<Item> detail = groups.getDetailHub("items");

        groups.setAO(g1);
        detail.setAO(a);
        assertSame(a, detail.getAO());

        groups.setAO(g2);

        assertTrue(detail.getAO() == null || detail.getAO() == b);
        assertFalse(detail.contains(a));
    }

    @Test
    void sharedHubListenerExceptionDoesNotCorruptMasterMembership() {
        Hub<Item> master = hub();
        Hub<Item> shared = master.createSharedHub();
        Item x = new Item("X");

        shared.addHubListener(new HubListenerAdapter<Item>() {
            @Override
            public void afterAdd(HubEvent<Item> e) {
                throw new RuntimeException("shared after add");
            }
        });

        RuntimeException ex = assertThrows(RuntimeException.class, () -> master.add(x));

        assertEquals("shared after add", ex.getMessage());
        assertTrue(master.contains(x));
        assertTrue(shared.contains(x));
    }

    @Test
    void serializationRoundTripAfterSortAndAOIsConsistent() throws Exception {
        Hub<Item> hub = hub();
        hub.sort("name", false);
        hub.setAO(hub.find("name", "B"));

        Hub<Item> copy = roundTrip(hub);

        assertEquals(3, copy.getSize());
        assertEquals(List.of("C", "B", "A"), copy.toList().stream().map(Item::getName).toList());

        if (copy.getAO() != null) {
            assertEquals("B", copy.getAO().getName());
        }
    }

    @Test
    void serializationRoundTripForSharedHubPreservesReadableMembership() throws Exception {
        Hub<Item> master = hub();
        Hub<Item> shared = master.createSharedHub(true);
        master.setPos(1);

        Hub<Item> copy = roundTrip(shared);

        assertEquals(shared.getSize(), copy.getSize());
        assertEquals(shared.getAt(0).getName(), copy.getAt(0).getName());
    }

    @Test
    void concurrentAOChangesAndRemovesLeaveValidPosition() throws Exception {
        Hub<Item> hub = new Hub<>(Item.class);
        List<Item> items = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            Item item = new Item("I" + i);
            items.add(item);
            hub.add(item);
        }

        ExecutorService es = Executors.newFixedThreadPool(6);
        try {
            List<Callable<Void>> tasks = new ArrayList<>();

            for (int i = 0; i < 100; i++) {
                int x = i;
                tasks.add(() -> {
                    Item item = items.get(x);
                    hub.setAO(item);
                    if ((x % 2) == 0) hub.remove(item);
                    return null;
                });
            }

            for (Future<Void> f : es.invokeAll(tasks)) {
                f.get(10, TimeUnit.SECONDS);
            }
        } finally {
            es.shutdownNow();
        }

        assertTrue(hub.getSize() >= 0);
        assertTrue(hub.getPos() < hub.getSize() || hub.getPos() == -1);
        if (hub.getAO() != null) {
            assertTrue(hub.contains(hub.getAO()));
        }
    }

    @Test
    void repeatedClearAddSortFindSmokeDoesNotCorruptHub() {
        Hub<Item> hub = new Hub<>(Item.class);

        for (int round = 0; round < 20; round++) {
            hub.clear();

            for (int i = 0; i < 20; i++) {
                hub.add(new Item("I" + i, i));
            }

            hub.sort("seq", false);

            assertEquals(20, hub.getSize());
            assertEquals(19, hub.getAt(0).getSeq());

            Item found = hub.find("name", "I10", true);

            assertNotNull(found);
            assertSame(found, hub.getAO());
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T roundTrip(T obj) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeObject(obj);
        }
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()))) {
            return (T) in.readObject();
        }
    }
}
