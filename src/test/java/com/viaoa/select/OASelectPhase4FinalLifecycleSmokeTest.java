package com.viaoa.select;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import com.viaoa.find.OAFinder;
import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OASelectPhase4FinalLifecycleSmokeTest {

    public static class Root extends OAObject {
        private final Hub<Item> children = new Hub<>(Item.class);
        public Hub<Item> getChildren() { return children; }
    }

    public static class Item extends OAObject {
        private String name;

        public Item() { }

        public Item(String name) {
            this.name = name;
        }

        public String getName() { return name; }
    }

    private static Root root(String... names) {
        Root r = new Root();
        for (String name : names) {
            r.getChildren().add(new Item(name));
        }
        return r;
    }

    @Test
    void selectCanBeUsedInEnhancedForLoopExactlyOncePerLifecycle() {
        Root r = root("A", "B");
        OASelect<Item> sel = new OASelect<>(Item.class);
        sel.setFinder(new OAFinder<Root, Item>(r, "children"));

        List<String> names = new ArrayList<>();
        for (Item item : sel) {
            names.add(item.getName());
        }

        assertEquals(List.of("A", "B"), names);
        assertTrue(sel.hasNextCompleted());

        List<String> second = new ArrayList<>();
        for (Item item : sel) {
            second.add(item.getName());
        }

        assertTrue(second.isEmpty(), "completed lifecycle is forward-only until reset");
    }

    @Test
    void resetMakesEnhancedForLoopRepeatable() {
        Root r = root("A", "B");
        OASelect<Item> sel = new OASelect<>(Item.class);
        sel.setFinder(new OAFinder<Root, Item>(r, "children"));

        List<String> first = names(sel);
        sel.reset();
        List<String> second = names(sel);

        assertEquals(first, second);
        assertEquals(List.of("A", "B"), second);
    }

    @Test
    void closeBeforeIterationReturnsNoResults() {
        Root r = root("A", "B");
        OASelect<Item> sel = new OASelect<>(Item.class);
        sel.setFinder(new OAFinder<Root, Item>(r, "children"));

        sel.close();

        assertTrue(names(sel).isEmpty());
        assertTrue(sel.isCancelled());
    }

    @Test
    void cancelDuringIterationStopsRemainingResults() {
        Root r = root("A", "B", "C");
        OASelect<Item> sel = new OASelect<>(Item.class);
        sel.setFinder(new OAFinder<Root, Item>(r, "children"));

        assertEquals("A", sel.next().getName());

        sel.cancel();

        assertNull(sel.next());
        assertFalse(sel.hasMore());
        assertEquals(1, sel.getAmountRead());
    }

    @Test
    void idIsUniqueAcrossInstances() {
        OASelect<Item> a = new OASelect<>(Item.class);
        OASelect<Item> b = new OASelect<>(Item.class);
        OASelect<Item> c = new OASelect<>(Item.class);

        assertNotEquals(a.getId(), b.getId());
        assertNotEquals(b.getId(), c.getId());
        assertNotEquals(a.getId(), c.getId());
    }

    private static List<String> names(OASelect<Item> sel) {
        List<String> list = new ArrayList<>();
        for (Item item : sel) {
            list.add(item.getName());
        }
        return list;
    }
}
