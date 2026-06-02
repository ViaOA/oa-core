package com.viaoa.select;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import com.viaoa.find.OAFinder;
import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OASelectPhase4MaxFilterCountContractTest {

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
        Root r = new Root();
        r.getChildren().add(new Item("A", 10));
        r.getChildren().add(new Item("B", 20));
        r.getChildren().add(new Item("C", 30));
        r.getChildren().add(new Item("D", 40));
        return r;
    }

    @Test
    void maxZeroMeansUnlimitedForFinderResults() {
        Root r = root();
        OASelect<Item> sel = new OASelect<>(Item.class);
        sel.setFinder(new OAFinder<Root, Item>(r, "children"));
        sel.setMax(0);

        assertEquals(List.of("A", "B", "C", "D"), names(sel));
    }

    @Test
    void maxLimitsReturnedResultsAfterFinderFilterDesiredContract() {
        Root r = root();
        OASelect<Item> sel = new OASelect<>(Item.class);
        sel.setFinder(new OAFinder<Root, Item>(r, "children"));
        sel.setFilter(item -> item.getAmount() >= 20);
        sel.setMax(2);

        assertEquals(List.of("B", "C"), names(sel));
        assertEquals(2, sel.getAmountRead());
    }

    @Test
    void countForFinderSelectionReflectsSemanticFilteredResultsDesiredContract() {
        Root r = root();
        OASelect<Item> sel = new OASelect<>(Item.class);
        sel.setFinder(new OAFinder<Root, Item>(r, "children"));
        sel.setFilter(item -> item.getAmount() >= 20);

        sel.select();

        assertEquals(3, sel.getCount(),
            "count should represent the same semantic selection scope as iteration");
    }

    @Test
    void countUnavailableIsDistinguishableFromValidZero() {
        OASelect<Item> sel = new OASelect<>(Item.class);

        assertEquals(-1, sel.getCount(),
            "unavailable count should not be reported as valid zero");
    }

    @Test
    void validZeroFinderCountIsZeroNotUnavailable() {
        Root r = root();
        OASelect<Item> sel = new OASelect<>(Item.class);
        sel.setFinder(new OAFinder<Root, Item>(r, "children"));
        sel.setFilter(item -> false);

        sel.select();

        assertEquals(0, sel.getCount());
        assertNull(sel.next());
        assertTrue(sel.hasNextCompleted());
    }

    @Test
    void dataSourceFilterAndPostFilterBothRestrictFinderResultSet() {
        Root r = root();
        OASelect<Item> sel = new OASelect<>(Item.class);
        sel.setFinder(new OAFinder<Root, Item>(r, "children"));
        sel.setDataSourceFilter(item -> item.getAmount() >= 20);
        sel.setFilter(item -> item.getAmount() <= 30);

        assertEquals(List.of("B", "C"), names(sel));
    }

    private static List<String> names(OASelect<Item> sel) {
        List<String> list = new ArrayList<>();
        Item item;
        while ((item = sel.next()) != null) {
            list.add(item.getName());
        }
        return list;
    }
}
