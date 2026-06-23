package com.viaoa.select;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.test.pos.model.oa.Invoice;
import com.test.pos.model.oa.InvoiceBasket;
import com.test.pos.model.oa.Item;
import com.test.pos.model.oa.LineItem;
import com.test.pos.model.oa.Product;
import com.test.pos.model.oa.Register;
import com.test.pos.model.oa.RegisterSession;
import com.test.pos.model.oa.Store;
import com.test.pos.model.oa.propertypath.StorePP;
import com.viaoa.filter.OAFilter;
import com.viaoa.find.OAFinder;
import com.viaoa.graph.api.internal.OAGraphInternal;
import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;
import com.viaoa.runtime.OARuntime;
import com.viaoa.text.OATextUtil;

class OASelectTest {
    private static final AtomicInteger NEXT = new AtomicInteger(7000);

    private static final String RAW_STORE_TO_ITEM_PATH = "registers.registerSessions.invoices.invoiceBaskets.lineItems.product.item";
    private static final String GENERATED_STORE_TO_ITEM_PATH = StorePP.registers().registerSessions().invoices()
            .invoiceBaskets().lineItems().product().item().pp();
    private static final String TEXT_UTIL_STORE_TO_ITEM_PATH = OATextUtil.createPropertyPath(Store.P_Registers,
            Register.P_RegisterSessions, RegisterSession.P_Invoices, Invoice.P_InvoiceBaskets,
            InvoiceBasket.P_LineItems, LineItem.P_Product, Product.P_Item);
    private static final String P_CONSTANT_STORE_TO_ITEM_PATH = Store.P_Registers + "." + Register.P_RegisterSessions
            + "." + RegisterSession.P_Invoices + "." + Invoice.P_InvoiceBaskets + "."
            + InvoiceBasket.P_LineItems + "." + LineItem.P_Product + "." + Product.P_Item;

    @BeforeEach
    void beforeEach() {
        OAGraphInternal og = (OAGraphInternal) OARuntime.graph(Register.class);
    }
    @AfterEach
    void afterEach() {
        OARuntime.graph(Register.class).close();
    }

    @Test
    void constructorsAssignClassWhereParamsOrderPassthruAndWhereObject() {
        OASelect<Item> empty = new OASelect<>();
        assertNull(empty.getSelectClass());
        assertTrue(empty.getId() > 0);

        OASelect<Item> byClass = new OASelect<>(Item.class);
        assertSame(Item.class, byClass.getSelectClass());

        OASelect<Item> passthru = new OASelect<>(Item.class, true, "select * from item", Item.P_Name);
        assertSame(Item.class, passthru.getSelectClass());
        assertTrue(passthru.getPassthru());
        assertTrue(passthru.getPassThru());
        assertEquals("select * from item", passthru.getWhere());
        assertEquals(Item.P_Name, passthru.getOrder());

        OASelect<Item> whereOrder = new OASelect<>(Item.class, Item.P_Code + " = 'BP1'", Item.P_Code);
        assertEquals(Item.P_Code + " = 'BP1'", whereOrder.getWhere());
        assertEquals(Item.P_Code, whereOrder.getOrderBy());

        Object[] params = { "BP1", Boolean.TRUE };
        OASelect<Item> withParams = new OASelect<>(Item.class, Item.P_Code + " = ? AND " + Item.P_Stocking + " = ?",
                params, Item.P_Name);
        assertSame(params, withParams.getParams());
        assertEquals(Item.P_Name, withParams.getSortBy());

        Store store = new Store(NEXT.incrementAndGet());
        OASelect<Item> whereObject = new OASelect<>(Item.class, store, Item.P_Name);
        assertSame(store, whereObject.getWhereObject());
        assertEquals(Item.P_Name, whereObject.getOrder());
    }

    @Test
    void getIdIsUniqueForEachSelect() {
        OASelect<Item> first = new OASelect<>(Item.class);
        OASelect<Item> second = new OASelect<>(Item.class);

        assertNotEquals(first.getId(), second.getId());
    }

    @Test
    void setParamsAndAddAppendWhereAndParameters() {
        OASelect<Item> select = new OASelect<>(Item.class);
        Object[] params = { "BP1" };

        select.setWhere(Item.P_Code + " = ?");
        select.setParams(params);
        assertSame(params, select.getParams());

        select.add(Item.P_Stocking + " = ?", new Object[] { Boolean.TRUE });
        select.add(Item.P_Name + " like ?", null);

        assertEquals(Item.P_Code + " = ? AND " + Item.P_Stocking + " = ? AND " + Item.P_Name + " like ?",
                select.getWhere());
        assertArrayEquals(new Object[] { "BP1", Boolean.TRUE }, select.getParams());
    }

    @Test
    void setSearchHubStoresInMemorySearchDomain() {
        Hub<Item> hub = hub(item("BP1", "Brake Pad", true));
        OASelect<Item> select = new OASelect<>(Item.class);

        select.setSearchHub(hub);

        assertSame(hub, select.getSearchHub());
    }

    @Test
    void resetKeepsOrClearsConfigurationAndLifecycleState() {
        OASelect<Item> select = finderSelect(item("BP2", "Brake Pad", true));
        select.setWhere(Item.P_Name + " like 'Brake%'");
        select.setOrder(Item.P_Name);
        select.select();
        assertTrue(select.hasBeenStarted());

        select.reset();
        assertFalse(select.hasBeenStarted());
        assertFalse(select.hasNextCompleted());
        assertEquals(Item.P_Name + " like 'Brake%'", select.getWhere());
        assertEquals(Item.P_Name, select.getOrder());
        assertEquals(0, select.getAmountRead());

        select.reset(true);
        assertNull(select.getWhere());
        assertNull(select.getOrder());
        assertNull(select.getWhereObject());
    }

    @Test
    void whereObjectAndWhereObjectPropertyPathAccessorsRoundTrip() {
        Store store = new Store(NEXT.incrementAndGet());
        OASelect<Item> select = new OASelect<>(Item.class);

        select.setWhereObject(store, GENERATED_STORE_TO_ITEM_PATH);
        assertSame(store, select.getWhereObject());
        assertEquals(GENERATED_STORE_TO_ITEM_PATH, select.getWhereObjectPropertyPath());

        select.setPropertyFromWhereObject(TEXT_UTIL_STORE_TO_ITEM_PATH);
        assertEquals(TEXT_UTIL_STORE_TO_ITEM_PATH, select.getPropertyFromWhereObject());

        select.setWhereObjectPropertyPath(P_CONSTANT_STORE_TO_ITEM_PATH);
        assertEquals(P_CONSTANT_STORE_TO_ITEM_PATH, select.getWhereObjectPropertyPath());
    }

    @Test
    void getDataSourceReturnsNullWithoutSelectClassOrRegisteredDatasource() {
        assertNull(new OASelect<Item>().getDataSource());
        assertNull(new OASelect<>(Item.class).getDataSource());
    }

    @Test
    void selectClassWhereAndSelectionStateAccessorsRoundTrip() {
        OASelect<Item> select = new OASelect<>();

        select.setSelectClass(Item.class);
        select.setWhere(Item.P_Code + " = ?", "BP1");
        select.setHasBeenSelected(true);

        assertSame(Item.class, select.getSelectClass());
        assertEquals(Item.P_Code + " = ?", select.getWhere());
        assertArrayEquals(new Object[] { "BP1" }, select.getParams());
        assertTrue(select.getHasBeenSelected());

        select.setWhere(Item.P_Name + " = ?", new Object[] { "Brake Pad" });
        assertEquals(Item.P_Name + " = ?", select.getWhere());
        assertArrayEquals(new Object[] { "Brake Pad" }, select.getParams());
    }

    @Test
    void filtersFinderAndOrderingAccessorsRoundTrip() {
        OASelect<Item> select = new OASelect<>(Item.class);
        OAFilter<Item> filter = item -> item.getStocking();
        OAFilter<Item> dsFilter = item -> item.getCode().startsWith("B");
        ListFinder finder = new ListFinder(item("BP3", "Brake Pad", true));

        select.setHubFilter(filter);
        assertSame(filter, select.getHubFilter());
        assertSame(filter, select.getFilter());

        select.setFilter(dsFilter);
        assertSame(dsFilter, select.getFilter());

        select.setDataSourceFilter(filter);
        assertSame(filter, select.getDataSourceFilter());

        select.setFinder(finder);
        assertSame(finder, select.getFinder());

        select.setOrder(Item.P_Name);
        assertEquals(Item.P_Name, select.getOrder());
        select.setOrderBy(Item.P_Code);
        assertEquals(Item.P_Code, select.getOrderBy());
        select.setSortBy(StorePP.registers().registerSessions().invoices().invoiceBaskets().lineItems().product()
                .item().name());
        assertEquals(Store.P_Registers + "." + Register.P_RegisterSessions + "." + RegisterSession.P_Invoices + "."
                + Invoice.P_InvoiceBaskets + "." + InvoiceBasket.P_LineItems + "." + LineItem.P_Product + "."
                + Product.P_Item + "." + Item.P_Name, select.getSortBy());
    }

    @Test
    void passthruAppendRewindCountMaxFetchAndDirtyAccessorsRoundTrip() {
        OASelect<Item> select = new OASelect<>(Item.class);

        select.setPassThru(true);
        assertTrue(select.getPassThru());
        assertTrue(select.getPassthru());

        select.setPassthru(false);
        assertFalse(select.getPassThru());

        select.setAppend(true);
        assertTrue(select.getAppend());

        assertTrue(select.getRewind());
        select.setRewind(false);
        assertFalse(select.getRewind());

        select.setCountFirst(true);
        assertTrue(select.getCountFirst());

        select.setMax(3);
        assertEquals(3, select.getMax());

        assertEquals(OASelect.defaultFetchAmount, select.getFetchAmount());
        select.setFetchAmount(12);
        assertEquals(12, select.getFetchAmount());
        select.setFetchAmount(-1);
        assertEquals(0, select.getFetchAmount());

        assertFalse(select.getDirty());
        select.setDirty(true);
        assertTrue(select.getDirty());
    }

    @Test
    void getCountIsCountedAndAmountReadUseFinderResults() {
        Item first = item("BP4", "Brake Pad", true);
        Item second = item("OF4", "Oil Filter", true);
        OASelect<Item> select = finderSelect(first, second);

        assertFalse(select.isCounted());
        select.select();
        assertTrue(select.hasMore());
        assertEquals(2, select.getCount());
        assertEquals(0, select.getAmountRead());

        assertSame(first, select.next());
        assertEquals(1, select.getAmountRead());
        assertSame(second, select.next());
        assertEquals(2, select.getAmountRead());
        assertFalse(select.hasMore());
        assertTrue(select.isCounted());
    }

    @Test
    void executeRequiresSelectClassAndDatasource() {
        RuntimeException noClass = assertThrows(RuntimeException.class, () -> new OASelect<Item>().execute("noop"));
        assertTrue(noClass.getMessage().contains("needs selectClass"));

        RuntimeException noDatasource = assertThrows(RuntimeException.class,
                () -> new OASelect<>(Item.class).execute("noop"));
        assertTrue(noDatasource.getMessage().contains("cant find datasource"));
    }

    @Test
    void selectOverloadsSetConfigurationAndUseFinderResults() {
        OASelect<Item> select = finderSelect(item("BP5", "Brake Pad", true));
        Object[] params = { "Brake Pad" };

        select.select(Item.P_Name + " = ?", params, Item.P_Name);
        assertEquals(Item.P_Name + " = ?", select.getWhere());
        assertSame(params, select.getParams());
        assertEquals(Item.P_Name, select.getOrder());
        assertTrue(select.hasBeenStarted());
        assertEquals("Brake Pad", select.next().getName());

        select.reset();
        select.select(Item.P_Code + " = 'BP5'", Item.P_Code);
        assertEquals(Item.P_Code + " = 'BP5'", select.getWhere());
        assertEquals(Item.P_Code, select.getOrder());

        select.reset();
        select.select(Item.P_Name + " = 'Brake Pad'");
        assertEquals(Item.P_Name + " = 'Brake Pad'", select.getWhere());

        select.reset();
        select.select(Item.P_Code + " = ?", new Object[] { "BP5" });
        assertArrayEquals(new Object[] { "BP5" }, select.getParams());
    }

    @Test
    void selectFinderModeAppliesQueryFilterDatasourceFilterHubFilterSearchHubAndSort() {
        Item brake = item("BP6", "Brake Pad", true);
        Item oil = item("OF6", "Oil Filter", true);
        Item hidden = item("XX6", "Hidden", true);
        OASelect<Item> select = finderSelect(oil, hidden, brake);
        Hub<Item> searchHub = hub(brake, oil);

        select.setSearchHub(searchHub);
        select.setWhere(Item.P_Name + " like ?");
        select.setParams(new Object[] { "*Filter" });
        select.setDataSourceFilter(item -> item.getStocking());
        select.setFilter(item -> !"XX6".equals(item.getCode()));
        select.setSortBy(Item.P_Name);
        select.select();

        assertSame(oil, select.next());
        assertNull(select.next());
        assertFalse(select.hasMore());
    }

    @Test
    void nextHasMoreIteratorAndMaxUseFinderResults() {
        Item first = item("A1", "A Item", true);
        Item second = item("B1", "B Item", true);
        Item third = item("C1", "C Item", true);
        OASelect<Item> select = finderSelect(first, second, third);
        select.setMax(2);

        assertTrue(select.hasNext());
        assertTrue(select.hasMore());
        Iterator<Item> iterator = select.iterator();
        assertTrue(iterator.hasNext());
        assertSame(first, iterator.next());
        assertSame(second, select.next());
        assertFalse(select.hasMore());
        assertTrue(select.hasNextCompleted());
        assertNull(select.next());
        iterator.remove();
    }

    @Test
    void cancelCloseAndCompletionFlagsAreIdempotent() {
        OASelect<Item> neverStarted = new OASelect<>(Item.class);
        assertFalse(neverStarted.isCancelled());
        neverStarted.cancel();
        assertTrue(neverStarted.isCancelled());
        neverStarted.close();
        assertTrue(neverStarted.hasNextCompleted());

        OASelect<Item> started = finderSelect(item("BP7", "Brake Pad", true));
        started.select();
        assertFalse(started.isSelectingNow());
        assertTrue(started.hasBeenStarted());
        started.close();
        assertTrue(started.hasNextCompleted());
        assertFalse(started.hasMore());
    }

    @Test
    void datasourceQueryAccessorsReturnNullWithoutActiveDatasourceIterator() {
        OASelect<Item> select = finderSelect(item("BP8", "Brake Pad", true));

        assertNull(select.getDataSourceQuery());
        assertNull(select.getDataSourceQuery2());
        select.select();
        assertNull(select.getDataSourceQuery());
        assertNull(select.getDataSourceQuery2());
    }

    @Test
    void isSelectAllReflectsConfiguration() {
        OASelect<Item> select = new OASelect<>(Item.class);
        assertTrue(select.isSelectAll());

        select.setWhere(Item.P_Name + " = 'Brake Pad'");
        assertFalse(select.isSelectAll());
        select.setWhere(null);

        select.setFilter(item -> true);
        assertFalse(select.isSelectAll());
        select.setFilter(null);

        select.setMax(1);
        assertFalse(select.isSelectAll());
        select.setMax(0);

        select.setFinder(new ListFinder());
        assertFalse(select.isSelectAll());
    }

    @Test
    void whereHubAccessorsRoundTrip() {
        Hub<Store> hub = hub(new Store(NEXT.incrementAndGet()));
        OASelect<Item> select = new OASelect<>(Item.class);

        select.setWhereHub(hub, RAW_STORE_TO_ITEM_PATH);
        assertSame(hub, select.getWhereHub());
        assertEquals(RAW_STORE_TO_ITEM_PATH, select.getWhereHubPropertyPath());

        select.setWhereHubPropertyPath(TEXT_UTIL_STORE_TO_ITEM_PATH);
        assertEquals(TEXT_UTIL_STORE_TO_ITEM_PATH, select.getWhereHubPropertyPath());

        select.setWhereHub(hub);
        assertSame(hub, select.getWhereHub());
    }

    private static OASelect<Item> finderSelect(Item... items) {
        OASelect<Item> select = new OASelect<>(Item.class);
        select.setDirty(false);
        select.setFinder(new ListFinder(items));
        return select;
    }

    private static Item item(String code, String name, boolean stocking) {
        Item item = new Item(NEXT.incrementAndGet());
        item.setCode(code);
        item.setName(name);
        item.setStocking(stocking);
        return item;
    }

    @SafeVarargs
    private static <T extends OAObject> Hub<T> hub(T... objects) {
        Hub<T> hub = new Hub<>();
        for (T object : objects) {
            hub.add(object);
        }
        return hub;
    }

    private static class ListFinder extends OAFinder<OAObject, Item> {
        private final List<Item> items;
        private OAFilter<Item> filter;

        ListFinder(Item... items) {
            this.items = new ArrayList<>(List.of(items));
        }

        @Override
        public void addFilter(OAFilter<Item> filter) {
            if (this.filter == null) {
                this.filter = filter;
            } else {
                OAFilter<Item> existing = this.filter;
                this.filter = item -> existing.isUsed(item) && filter.isUsed(item);
            }
        }

        @Override
        public OAFilter<Item> getFilter() {
            return filter;
        }

        @Override
        public void setFilter(OAFilter<Item> filter) {
            this.filter = filter;
        }

        @Override
        public List<Item> find() {
            List<Item> list = new ArrayList<>();
            for (Item item : items) {
                if (filter == null || filter.isUsed(item)) {
                    list.add(item);
                }
            }
            return list;
        }
    }
}
