package com.viaoa.filter;

import static org.junit.jupiter.api.Assertions.*;

import com.viaoa.hub.Hub;

import org.junit.jupiter.api.Test;

class OAFilterHubDirectTest {

    public static class Item {
        private String code;

        public Item(String code) {
            this.code = code;
        }

        public String getCode() {
            return code;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Item)) return false;
            return java.util.Objects.equals(code, ((Item) obj).code);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hashCode(code);
        }
    }

    @Test
    void equalFilterWithHubCandidateChecksMembershipForMatchObject() {
        Hub<Item> hub = new Hub<>(Item.class);
        Item a = new Item("A");
        Item b = new Item("B");
        hub.add(a);

        assertTrue(new OAEqualFilter(a).isUsed(hub));
        assertFalse(new OAEqualFilter(b).isUsed(hub));
    }

    @Test
    void notEqualFilterWithHubCandidateNegatesMembershipForMatchObject() {
        Hub<Item> hub = new Hub<>(Item.class);
        Item a = new Item("A");
        Item b = new Item("B");
        hub.add(a);

        assertFalse(new OANotEqualFilter(a).isUsed(hub));
        assertTrue(new OANotEqualFilter(b).isUsed(hub));
    }

    @Test
    void emptyAndNotEmptyFiltersRecognizeHubSize() {
        Hub<Item> hub = new Hub<>(Item.class);

        assertTrue(new OAEmptyFilter().isUsed(hub));
        assertFalse(new OANotEmptyFilter().isUsed(hub));

        hub.add(new Item("A"));

        assertFalse(new OAEmptyFilter().isUsed(hub));
        assertTrue(new OANotEmptyFilter().isUsed(hub));
    }

    @Test
    void inFilterWithHubUsesHubContainsContract() {
        Hub<Item> hub = new Hub<>(Item.class);
        Item a = new Item("A");
        Item equivalentA = new Item("A");
        hub.add(a);

        OAInFilter f = new OAInFilter(hub);

        assertTrue(f.isUsed(a));

        // Hub.contains current semantics determine whether equals-equivalent
        // objects are considered members. This documents current behavior.
        assertEquals(hub.contains(equivalentA), f.isUsed(equivalentA));
    }
}
