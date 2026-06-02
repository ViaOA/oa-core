package com.viaoa.select;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.viaoa.find.OAFinder;
import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OASelectFinderIterationTest {

    public static class Root extends OAObject {
        private final Hub<Item> children = new Hub<>(Item.class);
        public Hub<Item> getChildren() { return children; }
    }

    public static class Item extends OAObject {
        private String name;
        private int amount;

        public Item() { }
        public Item(String name, int amount) {
            this.name = name;
            this.amount = amount;
        }

        public String getName() { return name; }
        public int getAmount() { return amount; }
    }

    private static Root root() {
        Root root = new Root();
        root.getChildren().add(new Item("B", 20));
        root.getChildren().add(new Item("A", 10));
        root.getChildren().add(new Item("C", 30));
        return root;
    }

    @Test
    void finderSelectStartsLazilyAndIteratesForwardOnly() {
        Root root = root();
        OASelect<Item> sel = new OASelect<>(Item.class);
        sel.setFinder(new OAFinder<Root, Item>(root, "children"));

        assertFalse(sel.hasBeenStarted());

        assertTrue(sel.hasMore());
        assertTrue(sel.hasBeenStarted());

        assertEquals("B", sel.next().getName());
        assertEquals("A", sel.next().getName());
        assertEquals("C", sel.next().getName());
        assertNull(sel.next());
        assertTrue(sel.hasNextCompleted());
        assertEquals(3, sel.getAmountRead());
        assertFalse(sel.hasMore());
    }

    @Test
    void hasMoreDoesNotSkipFinderResult() {
        Root root = root();
        OASelect<Item> sel = new OASelect<>(Item.class);
        sel.setFinder(new OAFinder<Root, Item>(root, "children"));

        assertTrue(sel.hasMore());
        assertTrue(sel.hasMore());

        assertEquals("B", sel.next().getName());
        assertEquals("A", sel.next().getName());
    }

    @Test
    void iteratorDelegatesToSelectSemantics() {
        Root root = root();
        OASelect<Item> sel = new OASelect<>(Item.class);
        sel.setFinder(new OAFinder<Root, Item>(root, "children"));

        List<String> names = new ArrayList<>();
        for (Item item : sel) {
            names.add(item.getName());
        }

        assertEquals(List.of("B", "A", "C"), names);
        assertTrue(sel.hasNextCompleted());
    }

    @Test
    void iteratorRemoveIsNoop() {
        Root root = root();
        OASelect<Item> sel = new OASelect<>(Item.class);
        sel.setFinder(new OAFinder<Root, Item>(root, "children"));

        Iterator<Item> it = sel.iterator();
        assertDoesNotThrow(it::remove);
        assertTrue(it.hasNext());
        assertEquals("B", it.next().getName());
    }

    @Test
    void maxLimitsReturnedFinderResults() {
        Root root = root();
        OASelect<Item> sel = new OASelect<>(Item.class);
        sel.setFinder(new OAFinder<Root, Item>(root, "children"));
        sel.setMax(2);

        assertEquals("B", sel.next().getName());
        assertEquals("A", sel.next().getName());
        assertNull(sel.next());
        assertEquals(2, sel.getAmountRead());
        assertTrue(sel.hasNextCompleted());
    }

    @Test
    void filterRestrictsFinderResultsAndDoesNotBroadenScope() {
        Root root = root();
        OASelect<Item> sel = new OASelect<>(Item.class);
        sel.setFinder(new OAFinder<Root, Item>(root, "children"));
        sel.setFilter(item -> item.getAmount() >= 20);

        assertEquals("B", sel.next().getName());
        assertEquals("C", sel.next().getName());
        assertNull(sel.next());
        assertEquals(2, sel.getAmountRead());
    }

    @Test
    void dataSourceFilterAndPostFilterBothRestrictFinderResults() {
        Root root = root();
        OASelect<Item> sel = new OASelect<>(Item.class);
        sel.setFinder(new OAFinder<Root, Item>(root, "children"));
        sel.setDataSourceFilter(item -> item.getAmount() >= 10);
        sel.setFilter(item -> item.getAmount() <= 20);

        assertEquals("B", sel.next().getName());
        assertEquals("A", sel.next().getName());
        assertNull(sel.next());
    }

    @Test
    void finderSelectAppliesOrderByUsingComparator() {
        Root root = root();
        OASelect<Item> sel = new OASelect<>(Item.class);
        sel.setFinder(new OAFinder<Root, Item>(root, "children"));
        sel.setOrder("name");

        assertEquals("A", sel.next().getName());
        assertEquals("B", sel.next().getName());
        assertEquals("C", sel.next().getName());
    }

    @Test
    void resetAfterFinderExhaustionAllowsFreshFinderSelection() {
        Root root = root();
        OASelect<Item> sel = new OASelect<>(Item.class);
        sel.setFinder(new OAFinder<Root, Item>(root, "children"));

        assertEquals("B", sel.next().getName());
        assertEquals("A", sel.next().getName());
        assertEquals("C", sel.next().getName());
        assertNull(sel.next());

        sel.reset();

        assertEquals("B", sel.next().getName());
        assertEquals("A", sel.next().getName());
        assertEquals("C", sel.next().getName());
    }

    @Test
    void closeDuringFinderIterationStopsFutureResults() {
        Root root = root();
        OASelect<Item> sel = new OASelect<>(Item.class);
        sel.setFinder(new OAFinder<Root, Item>(root, "children"));

        assertEquals("B", sel.next().getName());

        sel.close();

        assertTrue(sel.isCancelled());
        assertTrue(sel.hasNextCompleted());
        assertNull(sel.next());
        assertFalse(sel.hasMore());
    }
}
