package com.viaoa.select;

import static org.junit.jupiter.api.Assertions.*;

import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OASelectResetAndLifecycleFlagsTest {

    public static class Item extends OAObject {
        private String name;
        public Item() { }
        public Item(String name) { this.name = name; }
        public String getName() { return name; }
    }

    @Test
    void newSelectHasUnstartedNoProgressState() {
        OASelect<Item> sel = new OASelect<>(Item.class);

        assertFalse(sel.hasBeenStarted());
        assertFalse(sel.getHasBeenSelected());
        assertFalse(sel.isSelectingNow());
        assertFalse(sel.isCancelled());
        assertFalse(sel.hasNextCompleted());
        assertEquals(0, sel.getAmountRead());
        assertEquals(-1, sel.getCount(), "without datasource/count, count should use documented unknown/minimum fallback");
        assertEquals(0, sel.getLastReadTime());
        assertNull(sel.getDataSourceQuery());
        assertNull(sel.getDataSourceQuery2());
    }

    @Test
    void setHasBeenSelectedControlsStartedFlag() {
        OASelect<Item> sel = new OASelect<>(Item.class);

        sel.setHasBeenSelected(true);
        assertTrue(sel.getHasBeenSelected());
        assertTrue(sel.hasBeenStarted());

        sel.setHasBeenSelected(false);
        assertFalse(sel.getHasBeenSelected());
        assertFalse(sel.hasBeenStarted());
    }

    @Test
    void cancelBeforeStartMarksCancelledAndCompleted() {
        OASelect<Item> sel = new OASelect<>(Item.class);

        sel.cancel();

        assertTrue(sel.isCancelled());
        assertTrue(sel.hasNextCompleted());
        assertFalse(sel.hasMore());
        assertNull(sel.next());
    }

    @Test
    void closeBeforeStartMarksCancelledAndCompleted() {
        OASelect<Item> sel = new OASelect<>(Item.class);

        sel.close();

        assertTrue(sel.isCancelled());
        assertTrue(sel.hasNextCompleted());
        assertFalse(sel.hasNext());
        assertNull(sel.next());
    }

    @Test
    void resetClearsLifecycleButKeepsConfiguration() {
        OASelect<Item> sel = new OASelect<>(Item.class, "name = ?", new Object[] { "A" }, "name");
        sel.setMax(5);
        sel.setDirty(true);
        sel.cancel();

        assertTrue(sel.isCancelled());

        sel.reset();

        assertFalse(sel.isCancelled());
        assertFalse(sel.hasBeenStarted());
        assertFalse(sel.hasNextCompleted());
        assertEquals("name = ?", sel.getWhere());
        assertArrayEquals(new Object[] { "A" }, sel.getParams());
        assertEquals("name", sel.getOrder());
        assertEquals(5, sel.getMax());
        assertTrue(sel.getDirty());
        assertEquals(0, sel.getLastReadTime());
        assertEquals(0, sel.getAmountRead());
    }

    @Test
    void resetClearOutValuesClearsWhereOrderAndWhereObjectOnly() {
        OASelect<Item> sel = new OASelect<>(Item.class);
        Item item = new Item("A");

        sel.setWhere("name = ?", new Object[] { "A" });
        sel.setOrder("name");
        sel.setWhereObject(item, "items");
        sel.setMax(5);
        sel.setAppend(true);

        sel.reset(true);

        assertNull(sel.getWhere());
        assertNull(sel.getOrder());
        assertNull(sel.getWhereObject());
        assertEquals("items", sel.getWhereObjectPropertyPath(),
            "reset(true) currently clears whereObject but not the property path");
        assertEquals(5, sel.getMax());
        assertTrue(sel.getAppend());
    }

    @Test
    void resetAfterCloseMakesSelectReusableByLifecycleFlags() {
        OASelect<Item> sel = new OASelect<>(Item.class);

        sel.close();
        assertTrue(sel.isCancelled());
        assertTrue(sel.hasNextCompleted());

        sel.reset();

        assertFalse(sel.isCancelled());
        assertFalse(sel.hasNextCompleted());
        assertFalse(sel.hasBeenStarted());
    }

    @Test
    void missingDatasourceSelectHasDefinedCancelledNoResultState() {
        OASelect<Item> sel = new OASelect<>(Item.class);

        sel.select();

        assertTrue(sel.hasBeenStarted());
        assertTrue(sel.isCancelled(), "missing datasource path should not look like successful execution");
        assertFalse(sel.hasMore());
        assertNull(sel.next());
        assertTrue(sel.hasNextCompleted());
    }

    @Test
    void executeWithoutSelectClassOrDatasourceFailsVisibly() {
        OASelect<Item> noClass = new OASelect<>();
        assertThrows(RuntimeException.class, () -> noClass.execute("x"));

        OASelect<Item> noDatasource = new OASelect<>(Item.class);
        assertThrows(RuntimeException.class, () -> noDatasource.execute("x"));
    }
}
