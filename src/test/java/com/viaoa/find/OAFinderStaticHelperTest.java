package com.viaoa.find;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OAFinderStaticHelperTest {

    public static class Item extends OAObject {
        private String code;
        private Integer amount;

        public Item() {
        }

        public Item(String code, Integer amount) {
            this.code = code;
            this.amount = amount;
        }

        public String getCode() {
            return code;
        }

        public Integer getAmount() {
            return amount;
        }
    }

    private static Hub<Item> hub() {
        Hub<Item> hub = new Hub<>(Item.class);
        hub.add(new Item("A", 10));
        hub.add(new Item("B", 30));
        hub.add(new Item("B", 20));
        hub.add(new Item(null, 40));
        return hub;
    }

    @Test
    void staticFindLargestUsesPropertyPathValue() {
        Hub<Item> hub = hub();

        Item item = OAFinder.findLargest(hub, "amount");

        assertNotNull(item);
        assertEquals(40, item.getAmount());
    }

    @Test
    void staticFindSmallestUsesPropertyPathValue() {
        Hub<Item> hub = hub();

        Item item = OAFinder.findSmallest(hub, "amount");

        assertNotNull(item);
        assertEquals(10, item.getAmount());
    }

    @Test
    void staticFindDuplicatesFindsDuplicateNonNullValues() {
        Hub<Item> hub = hub();

        List<Item> result = OAFinder.findDuplicates(hub, "code");

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(item -> "B".equals(item.getCode())));
    }

    @Test
    void staticFindDuplicatesIgnoresNullDuplicateValues() {
        Hub<Item> hub = new Hub<>(Item.class);
        hub.add(new Item(null, 1));
        hub.add(new Item(null, 2));

        assertTrue(OAFinder.findDuplicates(hub, "code").isEmpty());
    }

    @Test
    void staticHelpersReturnNullOrEmptyForEmptyHub() {
        Hub<Item> hub = new Hub<>(Item.class);

        assertNull(OAFinder.findLargest(hub, "amount"));
        assertNull(OAFinder.findSmallest(hub, "amount"));
        assertTrue(OAFinder.findDuplicates(hub, "code").isEmpty());
    }
}
