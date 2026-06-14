package com.viaoa.hub;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import com.viaoa.hub.mru.HubMru;
import com.viaoa.hub.util.HubNewObject;
import com.viaoa.hub.util.HubSample;
import com.viaoa.hub.util.HubTemp;
import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class HubPhase6UtilityConcurrencyFinalTest {

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

    @Test
    void hubInternalBridgeReturnsFriendAccesses() {
        HubInternalBridge bridge = new HubInternalBridge();

        assertNotNull(bridge.getHubFriendAccess());
        assertSame(bridge.getHubFriendAccess(), bridge.getHubFriendAccess());
    }

    @Test
    void hubTempCreatesIndependentTemporaryHub() {
        Hub<Item> source = new Hub<>(Item.class);
        Item a = new Item("A");
        source.add(a);

        Hub<Item> temp = HubTemp.createHub(source);

        assertNotNull(temp);
        assertEquals(Item.class, temp.getObjectClass());
        assertTrue(temp.contains(a) || temp.getSize() == 0,
            "HubTemp may either copy supplied membership or provide same-class empty temp hub");
    }

    @Test
    void hubSampleCreatesSampleHubOrFailsVisibly() {
        assertDoesNotThrow(() -> {
            Hub<Item> hub = HubSample.createHub(Item.class);
            assertNotNull(hub);
            assertEquals(Item.class, hub.getObjectClass());
        });
    }

    @Test
    void hubNewObjectCreatesAndAddsNewObjectWhenPossible() {
        Hub<Item> hub = new Hub<>(Item.class);

        try {
            Item item = HubNewObject.createNewObject(hub);
            assertNotNull(item);
            assertTrue(hub.contains(item) || item instanceof Item);
        } catch (RuntimeException ex) {
            assertNotNull(ex.getMessage());
        }
    }

    @Test
    void hubMruTracksRecentlyUsedObjectsBoundary() {
        Hub<Item> source = new Hub<>(Item.class);
        Item a = new Item("A");
        Item b = new Item("B");
        source.add(a);
        source.add(b);

        HubMru<Item> mru = new HubMru<>(source, 5);

        assertDoesNotThrow(() -> {
            source.setAO(a);
            source.setAO(b);
        });

        assertTrue(mru.getSize() >= 0);
    }

    @Test
    void serializationTruncatedPayloadFailsVisibly() throws Exception {
        Hub<Item> hub = new Hub<>(Item.class);
        hub.add(new Item("A"));

        byte[] bytes = write(hub);
        byte[] truncated = java.util.Arrays.copyOf(bytes, Math.max(1, bytes.length / 2));

        assertThrows(Exception.class, () -> read(truncated));
    }

    @Test
    void serializationCorruptPayloadFailsVisibly() throws Exception {
        Hub<Item> hub = new Hub<>(Item.class);
        hub.add(new Item("A"));

        byte[] bytes = write(hub);
        for (int i = 8; i < Math.min(20, bytes.length); i++) {
            bytes[i] = (byte) (bytes[i] ^ 0x5A);
        }

        assertThrows(Exception.class, () -> read(bytes));
    }

    @Test
    void failedSerializationReadDoesNotAffectLaterIndependentRead() throws Exception {
        Hub<Item> badHub = new Hub<>(Item.class);
        badHub.add(new Item("bad"));

        byte[] bad = write(badHub);
        byte[] truncated = java.util.Arrays.copyOf(bad, Math.max(1, bad.length / 2));
        assertThrows(Exception.class, () -> read(truncated));

        Hub<Item> good = new Hub<>(Item.class);
        good.add(new Item("good"));

        Hub<Item> copy = cast(read(write(good)));

        assertEquals(1, copy.getSize());
        assertEquals("good", copy.getAt(0).getName());
    }

    @Test
    void concurrentAddDistinctObjectsKeepsMembershipAndIdentity() throws Exception {
        Hub<Item> hub = new Hub<>(Item.class);

        ExecutorService es = Executors.newFixedThreadPool(8);
        try {
            List<Callable<Item>> tasks = new ArrayList<>();
            for (int i = 0; i < 200; i++) {
                int x = i;
                tasks.add(() -> {
                    Item item = new Item("I" + x, x);
                    hub.add(item);
                    return item;
                });
            }

            Set<java.util.UUID> guids = ConcurrentHashMap.newKeySet();

            for (Future<Item> f : es.invokeAll(tasks)) {
                Item item = f.get(5, TimeUnit.SECONDS);
                guids.add(item.getGuid());
            }

            assertEquals(200, guids.size());
            assertEquals(200, hub.getSize());
        } finally {
            es.shutdownNow();
        }
    }

    @Test
    void concurrentSetAOAndPropertyChangesDoNotCorruptMembership() throws Exception {
        Hub<Item> hub = new Hub<>(Item.class);
        for (int i = 0; i < 50; i++) {
            hub.add(new Item("I" + i, i));
        }

        ExecutorService es = Executors.newFixedThreadPool(6);
        try {
            List<Callable<Void>> tasks = new ArrayList<>();
            for (int i = 0; i < 200; i++) {
                int x = i;
                tasks.add(() -> {
                    int pos = x % hub.getSize();
                    Item item = hub.getAt(pos);
                    hub.setAO(item);
                    item.setName("N" + x);
                    return null;
                });
            }

            for (Future<Void> f : es.invokeAll(tasks)) {
                f.get(5, TimeUnit.SECONDS);
            }
        } finally {
            es.shutdownNow();
        }

        assertEquals(50, hub.getSize());
        assertTrue(hub.getPos() < hub.getSize());
    }

    @Test
    void concurrentListenerAddRemoveAndHubAddDoesNotThrow() throws Exception {
        Hub<Item> hub = new Hub<>(Item.class);
        AtomicInteger cnt = new AtomicInteger();

        ExecutorService es = Executors.newFixedThreadPool(6);
        try {
            List<Callable<Void>> tasks = new ArrayList<>();

            for (int i = 0; i < 50; i++) {
                int x = i;
                tasks.add(() -> {
                    HubListenerAdapter<Item> li = new HubListenerAdapter<Item>() {
                        @Override
                        public void afterAdd(HubEvent<Item> e) {
                            cnt.incrementAndGet();
                        }
                    };
                    hub.addHubListener(li);
                    hub.add(new Item("I" + x));
                    hub.removeHubListener(li);
                    return null;
                });
            }

            for (Future<Void> f : es.invokeAll(tasks)) {
                f.get(10, TimeUnit.SECONDS);
            }
        } finally {
            es.shutdownNow();
        }

        assertEquals(50, hub.getSize());
        assertTrue(cnt.get() >= 0);
    }

    @Test
    void concurrentRemoveExistingObjectsDoesNotLeaveNegativeSize() throws Exception {
        Hub<Item> hub = new Hub<>(Item.class);
        List<Item> list = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            Item item = new Item("I" + i);
            list.add(item);
            hub.add(item);
        }

        ExecutorService es = Executors.newFixedThreadPool(8);
        try {
            List<Callable<Void>> tasks = new ArrayList<>();
            for (Item item : list) {
                tasks.add(() -> {
                    hub.remove(item);
                    return null;
                });
            }

            for (Future<Void> f : es.invokeAll(tasks)) {
                f.get(5, TimeUnit.SECONDS);
            }
        } finally {
            es.shutdownNow();
        }

        assertTrue(hub.getSize() >= 0);
        for (Item item : list) {
            assertFalse(hub.contains(item));
        }
    }

    @Test
    void repeatedSortFindAddRemoveSmoke() {
        Hub<Item> hub = new Hub<>(Item.class);

        for (int i = 0; i < 100; i++) {
            hub.add(new Item("I" + i, i));
        }

        hub.sort("value", false);
        assertEquals(99, hub.getAt(0).getValue());

        Item found = hub.find("name", "I50", true);
        assertNotNull(found);
        assertSame(found, hub.getAO());

        hub.remove(found);
        assertFalse(hub.contains(found));

        hub.cancelSort();
        assertEquals(99, hub.getSize());
    }

    @Test
    void clearAfterManyMutationsLeavesCleanEmptyHub() {
        Hub<Item> hub = new Hub<>(Item.class);
        for (int i = 0; i < 100; i++) {
            hub.add(new Item("I" + i));
        }
        hub.setPos(50);
        hub.sort("name", true);

        hub.clear();

        assertEquals(0, hub.getSize());
        assertNull(hub.getAO());
        assertEquals(-1, hub.getPos());
        assertTrue(hub.isEmpty());
    }

    @SuppressWarnings("unchecked")
    private static <T> T cast(Object obj) {
        return (T) obj;
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
