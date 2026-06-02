package com.viaoa.select;

import static org.junit.jupiter.api.Assertions.*;

import com.viaoa.filter.OAFilter;
import com.viaoa.find.OAFinder;
import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OASelectConfigurationTest {

    public static class Item extends OAObject {
        private String name;
        public Item() { }
        public Item(String name) { this.name = name; }
        public String getName() { return name; }
    }

    @Test
    void constructorsAndCoreConfigurationRoundTrip() {
        OASelect<Item> sel = new OASelect<>(Item.class, "name = ?", new Object[] { "A" }, "name");

        assertEquals(Item.class, sel.getSelectClass());
        assertEquals("name = ?", sel.getWhere());
        assertArrayEquals(new Object[] { "A" }, sel.getParams());
        assertEquals("name", sel.getOrder());
        assertEquals("name", sel.getOrderBy());
        assertEquals("name", sel.getSortBy());
        assertFalse(sel.getPassthru());
        assertFalse(sel.getPassThru());
        assertEquals(OASelect.defaultFetchAmount, sel.getFetchAmount());
        assertEquals(0, sel.getMax());
        assertFalse(sel.getCountFirst());
        assertTrue(sel.getRewind());
        assertFalse(sel.getAppend());
    }

    @Test
    void passthruConstructorSetsPassthruWhereAndOrder() {
        OASelect<Item> sel = new OASelect<>(Item.class, true, "select * from item", "name");

        assertEquals(Item.class, sel.getSelectClass());
        assertTrue(sel.getPassthru());
        assertTrue(sel.getPassThru());
        assertEquals("select * from item", sel.getWhere());
        assertEquals("name", sel.getOrder());
    }

    @Test
    void aliasesUpdateSameUnderlyingFields() {
        OASelect<Item> sel = new OASelect<>(Item.class);

        sel.setOrder("a");
        assertEquals("a", sel.getOrderBy());
        assertEquals("a", sel.getSortBy());

        sel.setOrderBy("b");
        assertEquals("b", sel.getOrder());
        assertEquals("b", sel.getSortBy());

        sel.setSortBy("c");
        assertEquals("c", sel.getOrder());
        assertEquals("c", sel.getOrderBy());

        sel.setPassThru(true);
        assertTrue(sel.getPassthru());

        sel.setPassthru(false);
        assertFalse(sel.getPassThru());
    }

    @Test
    void booleanAndLimitFlagsRoundTrip() {
        OASelect<Item> sel = new OASelect<>(Item.class);

        sel.setAppend(true);
        sel.setRewind(false);
        sel.setCountFirst(true);
        sel.setMax(12);
        sel.setFetchAmount(7);
        sel.setDirty(true);

        assertTrue(sel.getAppend());
        assertFalse(sel.getRewind());
        assertTrue(sel.getCountFirst());
        assertEquals(12, sel.getMax());
        assertEquals(7, sel.getFetchAmount());
        assertTrue(sel.getDirty());

        sel.setFetchAmount(-10);
        assertEquals(0, sel.getFetchAmount(), "negative fetch amount clamps to zero");
    }

    @Test
    void whereAndParamsOverloadsRoundTripByReference() {
        OASelect<Item> sel = new OASelect<>(Item.class);
        Object[] params = { "A", 1 };

        sel.setWhere("name = ? and id = ?", params);

        assertEquals("name = ? and id = ?", sel.getWhere());
        assertSame(params, sel.getParams());

        sel.setWhere("name = ?", "B");
        assertEquals("name = ?", sel.getWhere());
        assertArrayEquals(new Object[] { "B" }, sel.getParams());

        Object[] params2 = { "C" };
        sel.setParams(params2);
        assertSame(params2, sel.getParams());
    }

    @Test
    void addConcatenatesWhereClausesAndAppendsParamsInOrder() {
        OASelect<Item> sel = new OASelect<>(Item.class);
        sel.setWhere("name = ?", new Object[] { "A" });

        sel.add("id > ?", new Object[] { 5 });
        sel.add("status = ?", new Object[] { "OPEN" });

        assertEquals("name = ? AND id > ? AND status = ?", sel.getWhere());
        assertArrayEquals(new Object[] { "A", 5, "OPEN" }, sel.getParams());
    }

    @Test
    void filtersAndFinderRoundTripByIdentity() {
        OASelect<Item> sel = new OASelect<>(Item.class);
        OAFilter<Item> filter = item -> true;
        OAFilter<Item> dsFilter = item -> true;
        OAFinder<?, Item> finder = new OAFinder<>("children");

        sel.setFilter(filter);
        sel.setDataSourceFilter(dsFilter);
        sel.setFinder(finder);

        assertSame(filter, sel.getFilter());
        assertSame(filter, sel.getHubFilter());
        assertSame(dsFilter, sel.getDataSourceFilter());
        assertSame(finder, sel.getFinder());

        OAFilter<Item> hubFilter = item -> false;
        sel.setHubFilter(hubFilter);
        assertSame(hubFilter, sel.getFilter());
    }

    @Test
    void whereObjectAndWhereHubFieldsSharePropertyPathContract() {
        OASelect<Item> sel = new OASelect<>(Item.class);
        Item item = new Item("A");
        Hub<Item> hub = new Hub<>(Item.class);

        sel.setWhereObject(item, "orders.items");
        assertSame(item, sel.getWhereObject());
        assertEquals("orders.items", sel.getWhereObjectPropertyPath());
        assertEquals("orders.items", sel.getPropertyFromWhereObject());

        sel.setPropertyFromWhereObject("items");
        assertEquals("items", sel.getWhereObjectPropertyPath());

        sel.setWhereHub(hub, "children");
        assertSame(hub, sel.getWhereHub());
        assertEquals("children", sel.getWhereHubPropertyPath());

        sel.setWhereHubPropertyPath("otherChildren");
        assertEquals("otherChildren", sel.getWhereObjectPropertyPath());
    }

    @Test
    void selectAllReflectsOnlyUnrestrictedConfiguration() {
        OASelect<Item> sel = new OASelect<>(Item.class);

        assertTrue(sel.isSelectAll());

        sel.setWhere("name = ?");
        assertFalse(sel.isSelectAll());

        sel.setWhere(null);
        assertTrue(sel.isSelectAll());

        sel.setMax(1);
        assertFalse(sel.isSelectAll());

        sel.setMax(0);
        sel.setFilter(item -> true);
        assertFalse(sel.isSelectAll());
    }
}
