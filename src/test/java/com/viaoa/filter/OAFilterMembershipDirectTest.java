package com.viaoa.filter;

import static org.junit.jupiter.api.Assertions.*;

import com.viaoa.hub.Hub;

import org.junit.jupiter.api.Test;

class OAFilterMembershipDirectTest {

    public static class Item {
        private String code;

        public Item(String code) {
            this.code = code;
        }

        public String getCode() {
            return code;
        }
    }

    @Test
    void inFilterWithHubAcceptsContainedObject() {
        Hub<Item> hub = new Hub<>(Item.class);
        Item a = new Item("A");
        Item b = new Item("B");
        hub.add(a);

        OAInFilter f = new OAInFilter(hub);

        assertTrue(f.isUsed(a));
        assertFalse(f.isUsed(b));
        assertFalse(f.isUsed(null));
    }

    @Test
    void inFilterWithEmptyHubRejectsCandidate() {
        Hub<Item> hub = new Hub<>(Item.class);
        OAInFilter f = new OAInFilter(hub);

        assertFalse(f.isUsed(new Item("A")));
        assertFalse(f.isUsed(null));
    }

    @Test
    void inFilterWithNullHubRejectsCandidatesCurrentContract() {
        OAInFilter f = new OAInFilter((Hub) null);

        assertFalse(f.isUsed(new Item("A")));
        assertFalse(f.isUsed(null));
    }
}
