package com.viaoa.select;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import com.viaoa.find.OAFinder;
import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OASelectPhase3ConfigurationSnapshotTest {

    public static class Root extends OAObject {
        private final Hub<Item> children = new Hub<>(Item.class);
        public Hub<Item> getChildren() { return children; }
    }

    public static class Item extends OAObject {
        private String name;
        private int amount;

        public Item() {
        }

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
        return r;
    }

    @Test
    void changingFilterAfterSelectDoesNotCorruptExistingFinderResultSnapshot() {
        Root r = root();
        OASelect<Item> sel = new OASelect<>(Item.class);
        sel.setFinder(new OAFinder<Root, Item>(r, "children"));
        sel.setFilter(item -> item.getAmount() >= 10);

        sel.select();

        sel.setFilter(item -> false);

        assertEquals(List.of("A", "B", "C"), readNames(sel),
            "finder branch builds a result snapshot at select time");
    }

    @Test
    void changingOrderAfterSelectDoesNotResortExistingFinderResultSnapshot() {
        Root r = root();
        OASelect<Item> sel = new OASelect<>(Item.class);
        sel.setFinder(new OAFinder<Root, Item>(r, "children"));
        sel.setOrder("name");

        sel.select();

        sel.setOrder("amount desc");

        assertEquals(List.of("A", "B", "C"), readNames(sel));
    }

    @Test
    void changingMaxAfterSelectShouldNotRetroactivelyExpandCurrentLifecycleDesiredContract() {
        Root r = root();
        OASelect<Item> sel = new OASelect<>(Item.class);
        sel.setFinder(new OAFinder<Root, Item>(r, "children"));
        sel.setMax(1);

        sel.select();

        sel.setMax(3);

        assertEquals(List.of("A"), readNames(sel),
            "max should be part of a coherent configuration snapshot for the opened lifecycle");
    }

    @Test
    void resetAfterConfigChangeUsesNewConfiguration() {
        Root r = root();
        OASelect<Item> sel = new OASelect<>(Item.class);
        sel.setFinder(new OAFinder<Root, Item>(r, "children"));
        sel.setFilter(item -> item.getAmount() >= 20);

        assertEquals(List.of("B", "C"), readNames(sel));

        sel.reset();
        sel.setFilter(item -> item.getAmount() >= 30);

        assertEquals(List.of("C"), readNames(sel));
    }

    private static List<String> readNames(OASelect<Item> sel) {
        List<String> list = new ArrayList<>();
        Item item;
        while ((item = sel.next()) != null) {
            list.add(item.getName());
        }
        return list;
    }
}
