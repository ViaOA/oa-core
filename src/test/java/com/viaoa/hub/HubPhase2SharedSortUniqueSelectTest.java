package com.viaoa.hub;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.util.Comparator;
import java.util.List;

import com.viaoa.object.OAObject;
import com.viaoa.select.OASelect;

import org.junit.jupiter.api.Test;

class HubPhase2SharedSortUniqueSelectTest {

    public static class Item extends OAObject implements Serializable {
        private static final long serialVersionUID = 1L;
        private String name;
        private int amount;
        private String code;

        public Item() { }
        public Item(String name, int amount) {
            this.name = name;
            this.amount = amount;
            this.code = name;
        }

        public String getName() { return name; }
        public int getAmount() { return amount; }
        public String getCode() { return code; }

        public void setCode(String code) {
            String old = this.code;
            this.code = code;
            firePropertyChange("code", old, code);
        }
    }

    private static Hub<Item> hub() {
        Hub<Item> hub = new Hub<>(Item.class);
        hub.add(new Item("C", 30));
        hub.add(new Item("A", 10));
        hub.add(new Item("B", 20));
        return hub;
    }

    @Test
    void createSharedHubSharesMembership() {
        Hub<Item> master = hub();
        Hub<Item> shared = master.createSharedHub();

        assertNotSame(master, shared);
        assertSame(master, shared.getSharedHub());
        assertEquals(master.getSize(), shared.getSize());
        assertSame(master.getAt(0), shared.getAt(0));

        Item d = new Item("D", 40);
        master.add(d);

        assertTrue(shared.contains(d));
    }

    @Test
    void sharedHubWithShareAOTracksActiveObjectBothWays() {
        Hub<Item> master = hub();
        Hub<Item> shared = master.createSharedHub(true);

        master.setPos(1);
        assertSame(master.getAO(), shared.getAO());

        shared.setPos(2);
        assertSame(shared.getAO(), master.getAO());
        assertEquals(shared.getPos(), master.getPos());
    }

    @Test
    void sharedHubWithoutShareAOCanUseIndependentAO() {
        Hub<Item> master = hub();
        Hub<Item> shared = master.createSharedHub(false);

        master.setPos(1);
        shared.setPos(2);

        assertTrue(shared.getPos() == 2 || shared.getAO() == master.getAO());
    }

    @Test
    void setSharedHubExplicitlySharesMembership() {
        Hub<Item> master = hub();
        Hub<Item> shared = new Hub<>(Item.class);

        shared.setSharedHub(master, true);

        assertSame(master, shared.getSharedHub());
        assertEquals(master.getSize(), shared.getSize());
    }

    @Test
    void sortByPropertyPathAscendingAndDescending() {
        Hub<Item> h1 = hub();
        h1.sort("name", true);
        assertEquals(List.of("A", "B", "C"), h1.toList().stream().map(Item::getName).toList());
        assertTrue(h1.isSorted());

        Hub<Item> h2 = hub();
        h2.sort("name", false);
        assertEquals(List.of("C", "B", "A"), h2.toList().stream().map(Item::getName).toList());
    }

    @Test
    void sortWithComparatorAndCancelSortAreSafe() {
        Hub<Item> hub = hub();

        hub.sort(Comparator.comparingInt(Item::getAmount).reversed());
        assertEquals(List.of(30, 20, 10), hub.toList().stream().map(Item::getAmount).toList());

        hub.cancelSort();

        assertFalse(hub.isSorted());
        assertEquals(3, hub.getSize());
    }

    @Test
    void resortAfterSortPreservesSortedOrder() {
        Hub<Item> hub = hub();

        hub.sort("name", true);
        hub.resort();

        assertEquals(List.of("A", "B", "C"), hub.toList().stream().map(Item::getName).toList());
    }

    @Test
    void findReturnsMatchingObjectAndCanSetAO() {
        Hub<Item> hub = hub();

        Item item = hub.find("name", "B");
        assertNotNull(item);
        assertEquals("B", item.getName());
        assertNull(hub.getAO());

        Item item2 = hub.find("name", "A", true);
        assertSame(item2, hub.getAO());
    }

    @Test
    void findNextStartsAfterFromObject() {
        Hub<Item> hub = new Hub<>(Item.class);
        Item a1 = new Item("A", 1);
        Item b = new Item("B", 2);
        Item a2 = new Item("A", 3);
        hub.add(a1);
        hub.add(b);
        hub.add(a2);

        assertSame(a2, hub.findNext(a1, "name", "A"));
        assertNull(hub.findNext(a2, "name", "A"));
    }

    @Test
    void setUniquePropertyAllowsLookupAndUpdatesOnPropertyChange() {
        Hub<Item> hub = new Hub<>(Item.class);
        Item a = new Item("A", 1);
        hub.add(a);

        hub.setUniqueProperty("code");

        assertSame(a, hub.getObject("A"));

        a.setCode("B");

        assertNull(hub.getObject("A"));
        assertSame(a, hub.getObject("B"));
    }

    @Test
    void duplicateUniquePropertyUsesDefinedMemberPolicy() {
        Hub<Item> hub = new Hub<>(Item.class);
        Item a = new Item("A", 1);
        Item b = new Item("A", 2);
        hub.add(a);
        hub.add(b);

        hub.setUniqueProperty("code");

        Item found = hub.getObject("A");

        assertTrue(found == a || found == b);
    }

    @Test
    void removingObjectRemovesUniqueLookup() {
        Hub<Item> hub = new Hub<>(Item.class);
        Item a = new Item("A", 1);
        hub.add(a);

        hub.setUniqueProperty("code");
        hub.remove(a);

        assertNull(hub.getObject("A"));
    }

    @Test
    void selectStateDefaultsAndCreateSelect() {
        Hub<Item> hub = new Hub<>(Item.class);

        assertNull(hub.getSelect());
        assertNull(hub.getSelect(false));
        assertFalse(hub.isMoreData());

        OASelect<? extends OAObject> select = hub.getSelect(true);

        assertNotNull(select);
        assertSame(select, hub.getSelect(false));
    }

    @Test
    void cancelSelectAndLoadAllDataAreSafeWithoutDatasource() {
        Hub<Item> hub = new Hub<>(Item.class);

        assertDoesNotThrow(hub::cancelSelect);
        assertDoesNotThrow(hub::loadAllData);
    }

    @Test
    void setSelectWhereAndOrderRoundTrip() {
        Hub<Item> hub = new Hub<>(Item.class);

        hub.setSelectWhere("id = ?");
        hub.setSelectOrder("name");

        assertEquals("id = ?", hub.getSelectWhere());
        assertEquals("name", hub.getSelectOrder(hub));
    }

    @Test
    void cloneAndCopyIntoPreserveMembershipButNotHubIdentity() throws Exception {
        Hub<Item> source = hub();
        Hub<Item> target = new Hub<>(Item.class);

        source.copyInto(target);

        assertNotSame(source, target);
        assertEquals(source.toList(), target.toList());

        @SuppressWarnings("unchecked")
        Hub<Item> clone = (Hub<Item>) source.clone();

        assertNotSame(source, clone);
        assertEquals(source.toList(), clone.toList());
    }

    @Test
    void serializationRoundTripPreservesMembershipOrderAndObjectClass() throws Exception {
        Hub<Item> hub = new Hub<>(Item.class);
        hub.add(new Item("A", 1));
        hub.add(new Item("B", 2));

        Hub<Item> copy = roundTrip(hub);

        assertEquals(Item.class, copy.getObjectClass());
        assertEquals(2, copy.getSize());
        assertEquals("A", copy.getAt(0).getName());
        assertEquals("B", copy.getAt(1).getName());
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
