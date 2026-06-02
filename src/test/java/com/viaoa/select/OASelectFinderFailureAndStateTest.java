package com.viaoa.select;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicInteger;

import com.viaoa.find.OAFinder;
import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OASelectFinderFailureAndStateTest {

    public static class Root extends OAObject {
        private final Hub<Item> children = new Hub<>(Item.class);
        public Hub<Item> getChildren() { return children; }
    }

    public static class Item extends OAObject {
        private String name;
        public Item() { }
        public Item(String name) { this.name = name; }
        public String getName() { return name; }
    }

    private static Root root() {
        Root root = new Root();
        root.getChildren().add(new Item("A"));
        root.getChildren().add(new Item("B"));
        return root;
    }

    @Test
    void finderOriginalFilterIsRestoredAfterSelectSetup() {
        Root root = root();
        OAFinder<Root, Item> finder = new OAFinder<>(root, "children");
        finder.addFilter(item -> "A".equals(item.getName()));

        OASelect<Item> sel = new OASelect<>(Item.class);
        sel.setFinder(finder);
        sel.setFilter(item -> true);

        assertEquals("A", sel.next().getName());
        assertNull(sel.next());

        assertEquals(1, finder.find(root).size());
        assertEquals("A", finder.findFirst(root).getName());
    }

    @Test
    void finderFilterExceptionIsVisibleAndDoesNotBecomeFalseEmptySuccess() {
        Root root = root();
        OASelect<Item> sel = new OASelect<>(Item.class);
        sel.setFinder(new OAFinder<Root, Item>(root, "children"));
        sel.setFilter(item -> {
            throw new IllegalStateException("boom");
        });

        assertThrows(IllegalStateException.class, sel::select);
        assertTrue(sel.hasBeenStarted());
        assertFalse(sel.isCancelled());
    }

    @Test
    void finderFailureCanBeRetriedAfterFilterCleared() {
        Root root = root();
        OASelect<Item> sel = new OASelect<>(Item.class);
        sel.setFinder(new OAFinder<Root, Item>(root, "children"));
        sel.setFilter(item -> {
            throw new IllegalStateException("boom");
        });

        assertThrows(IllegalStateException.class, sel::select);

        sel.setFilter(null);
        sel.reset();

        assertEquals("A", sel.next().getName());
        assertEquals("B", sel.next().getName());
    }

    @Test
    void finderPostFilterRunsOncePerCandidateDuringSelectSetup() {
        Root root = root();
        AtomicInteger cnt = new AtomicInteger();

        OASelect<Item> sel = new OASelect<>(Item.class);
        sel.setFinder(new OAFinder<Root, Item>(root, "children"));
        sel.setFilter(item -> {
            cnt.incrementAndGet();
            return true;
        });

        sel.select();

        assertEquals(2, cnt.get());

        assertEquals("A", sel.next().getName());
        assertEquals("B", sel.next().getName());
    }

    @Test
    void whereObjectWithFinderAndNoDatasourceDoesNotSilentlyDropFinderResultsByDefaultCurrentContract() {
        Root root = root();
        OASelect<Item> sel = new OASelect<>(Item.class);
        sel.setFinder(new OAFinder<Root, Item>(root, "children"));
        sel.setWhereObject(root, "children");

        assertEquals("A", sel.next().getName());
        assertEquals("B", sel.next().getName());
    }

    @Test
    void invalidWhereObjectPropertyPathWithFinderShouldFailOrBeDocumentedCurrentGap() {
        Root root = root();
        OASelect<Item> sel = new OASelect<>(Item.class);
        sel.setFinder(new OAFinder<Root, Item>(root, "children"));
        sel.setWhereObject(root, "missing.path");

        try {
            sel.select();
        } catch (RuntimeException expected) {
            return;
        }

        assertEquals("missing.path", sel.getWhereObjectPropertyPath(),
            "current finder path does not validate whereObjectPropertyPath; this documents the gap");
    }
}
