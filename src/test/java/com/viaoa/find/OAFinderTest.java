package com.viaoa.find;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

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
import com.viaoa.cascade.OACascade;
import com.viaoa.filter.OAEqualFilter;
import com.viaoa.filter.OAFilter;
import com.viaoa.hub.Hub;
import com.viaoa.text.OATextUtil;

class OAFinderTest {
    private static final AtomicInteger NEXT = new AtomicInteger(2000);

    private static final String RAW_STORE_TO_ITEM_PATH = "registers.registerSessions.invoices.invoiceBaskets.lineItems.product.item";
    private static final String GENERATED_STORE_TO_ITEM_PATH = StorePP.registers().registerSessions().invoices()
            .invoiceBaskets().lineItems().product().item().pp();
    private static final String TEXT_UTIL_STORE_TO_ITEM_PATH = OATextUtil.createPath(Store.P_Registers,
            Register.P_RegisterSessions, RegisterSession.P_Invoices, Invoice.P_InvoiceBaskets,
            InvoiceBasket.P_LineItems, LineItem.P_Product, Product.P_Item);
    private static final String P_CONSTANT_STORE_TO_ITEM_PATH = Store.P_Registers + "." + Register.P_RegisterSessions
            + "." + RegisterSession.P_Invoices + "." + Invoice.P_InvoiceBaskets + "."
            + InvoiceBasket.P_LineItems + "." + LineItem.P_Product + "." + Product.P_Item;

    @Test
    void constructorsStoreRootsAndPropertyPathsForLaterFind() {
        assertNull(new OAFinder<Store, Item>().find());

        Graph graph = graph("Brake Pad", 12.50, true);
        assertSame(graph.item, new OAFinder<Store, Item>(GENERATED_STORE_TO_ITEM_PATH).findFirst(graph.store));
        assertSame(graph.item, new OAFinder<Store, Item>(graph.store, GENERATED_STORE_TO_ITEM_PATH).findFirst());

        Hub<Store> hub = hub(graph.store);
        assertSame(graph.item, new OAFinder<Store, Item>(hub, GENERATED_STORE_TO_ITEM_PATH).findFirst());
        assertSame(graph.item, new OAFinder<Store, Item>(hub, GENERATED_STORE_TO_ITEM_PATH, false).findFirst());
    }

    @Test
    void setAllowRecursiveRootAndGetterRoundTrip() {
        OAFinder<Store, Item> finder = new OAFinder<>(GENERATED_STORE_TO_ITEM_PATH);

        assertFalse(finder.getAllowRecursiveRoot());
        finder.setAllowRecursiveRoot(true);
        assertTrue(finder.getAllowRecursiveRoot());
        finder.setAllowRecursiveRoot(false);
        assertFalse(finder.getAllowRecursiveRoot());
    }

    @Test
    void protectedHooksOnFoundAndOnDataNotFoundAreInvoked() {
        HookFinder finder = new HookFinder(GENERATED_STORE_TO_ITEM_PATH);
        Graph graph = graph("Brake Pad", 12.50, true);

        assertEquals(List.of(graph.item), finder.find(graph.store));
        assertEquals(List.of(graph.item), finder.onFoundItems);

        finder.callOnDataNotFound();
        assertEquals(1, finder.dataNotFoundCount);
    }

    @Test
    void stopAndGetStopAllowEarlyTermination() {
        OAFinder<Store, Item> finder = new OAFinder<>(GENERATED_STORE_TO_ITEM_PATH);

        assertFalse(finder.getStop());
        finder.stop();
        assertTrue(finder.getStop());
    }

    @Test
    void useOnlyLoadedDataGetterSetterRoundTrip() {
        OAFinder<Store, Item> finder = new OAFinder<>(GENERATED_STORE_TO_ITEM_PATH);

        assertFalse(finder.getUseOnlyLoadedData());
        finder.setUseOnlyLoadedData(true);
        assertTrue(finder.getUseOnlyLoadedData());
    }

    @Test
    void maxFoundLimitsResultsAndGetterReturnsConfiguredValue() {
        MultiGraph graph = multiGraph();
        OAFinder<Store, Item> finder = new OAFinder<>(GENERATED_STORE_TO_ITEM_PATH);
        finder.setMaxFound(1);

        List<Item> found = finder.find(graph.store);

        assertEquals(1, finder.getMaxFound());
        assertEquals(List.of(graph.firstItem), found);
    }

    @Test
    void findUsesConfiguredObjectOrHubRootAndNullWhenNoRoot() {
        MultiGraph graph = multiGraph();
        OAFinder<Store, Item> finder = new OAFinder<>(GENERATED_STORE_TO_ITEM_PATH);
        assertNull(finder.find());

        finder.setRoot(graph.store);
        assertEquals(List.of(graph.firstItem, graph.secondItem), finder.find());

        finder.setRoot(hub(graph.store));
        assertEquals(List.of(graph.firstItem, graph.secondItem), finder.find());
    }

    @Test
    void findHubListAndLastUsedVariantsReturnExpectedObjectsAndRootPosition() {
        Graph first = graph("Brake Pad", 12.50, true);
        Graph second = graph("Oil Filter", 8.75, false);
        OAFinder<Store, Item> finder = new OAFinder<>(GENERATED_STORE_TO_ITEM_PATH);

        Hub<Store> hub = hub(first.store, second.store);
        assertEquals(List.of(first.item, second.item), finder.find(hub));
        assertEquals(List.of(second.item), finder.find(hub, first.store));
        assertEquals(2, finder.getRootHubPos());

        List<Store> stores = List.of(first.store, second.store);
        assertEquals(List.of(first.item, second.item), finder.find(stores));
        assertEquals(List.of(second.item), finder.find(stores, first.store));
        assertEquals(List.of(), finder.find((List<Store>) null));
        assertEquals(List.of(), finder.find(List.of()));
    }

    @Test
    void protectedFindHubVariantCanBeCalledBySubclass() {
        Graph graph = graph("Brake Pad", 12.50, true);
        HookFinder finder = new HookFinder(GENERATED_STORE_TO_ITEM_PATH);

        assertEquals(List.of(graph.item), finder.callProtectedFind(hub(graph.store), null));
    }

    @Test
    void clearAddGetAndSetFilterControlMatching() {
        Graph graph = graph("Brake Pad", 12.50, true);
        OAFinder<Store, Item> finder = new OAFinder<>(GENERATED_STORE_TO_ITEM_PATH);
        OAFilter<Item> filter = item -> "Brake Pad".equals(item.getName());

        finder.setFilter(filter);
        assertSame(filter, finder.getFilter());
        assertEquals(List.of(graph.item), finder.find(graph.store));

        finder.clearFilters();
        assertNull(finder.getFilter());
        finder.addFilter(item -> false);
        assertEquals(List.of(), finder.find(graph.store));
    }

    @Test
    void canFindFirstAndFindFirstVariantsReturnFirstMatchAndRestoreMaxFound() {
        MultiGraph graph = multiGraph();
        OAFinder<Store, Item> finder = new OAFinder<>(GENERATED_STORE_TO_ITEM_PATH);
        finder.setMaxFound(2);

        assertTrue(finder.canFindFirst(graph.store));
        assertEquals(2, finder.getMaxFound());
        assertSame(graph.firstItem, finder.findFirst(graph.store));
        assertEquals(2, finder.getMaxFound());
        assertSame(graph.firstItem, finder.findFirst(hub(graph.store)));

        finder.setRoot(graph.store);
        assertSame(graph.firstItem, finder.findFirst());
        assertNull(finder.findFirst((Store) null));
    }

    @Test
    void findNextAndFindLastVariantsUseSearchOrder() {
        Graph first = graph("Brake Pad", 12.50, true);
        Graph second = graph("Oil Filter", 8.75, false);
        Hub<Store> hub = hub(first.store, second.store);
        OAFinder<Store, Item> finder = new OAFinder<>(GENERATED_STORE_TO_ITEM_PATH);

        assertSame(second.item, finder.findNext(hub, first.store));
        assertSame(second.item, finder.findLast(hub));
        assertSame(first.item, finder.findLast(first.store));

        finder.setRoot(hub);
        Item item = finder.findLast();
        assertSame(second.item, item);
    }

    @Test
    void findLargestAndSmallestPreserveExistingFilterAndUseNumericProperty() {
        MultiGraph graph = multiGraph();
        OAFinder<Store, LineItem> finder = new OAFinder<>(StorePP.registers().registerSessions().invoices()
                .invoiceBaskets().lineItems().pp());
        OAFilter<LineItem> all = line -> true;
        finder.setRoot(graph.store);
        finder.setFilter(all);

        assertSame(graph.expensiveLineItem, finder.findLargest(LineItem.P_PriceEach));
        assertSame(graph.cheapLineItem, finder.findSmallest(LineItem.P_PriceEach));
        assertSame(all, finder.getFilter());
        assertSame(graph.expensiveLineItem, finder.findLargest(graph.store, LineItem.P_PriceEach));
        assertSame(graph.cheapLineItem, finder.findSmallest(graph.store, LineItem.P_PriceEach));
        assertSame(graph.expensiveLineItem, finder.findLargest(hub(graph.store), LineItem.P_PriceEach));
        assertSame(graph.cheapLineItem, finder.findSmallest(hub(graph.store), LineItem.P_PriceEach));
    }

    @Test
    void findDuplicatesReturnsDuplicateAndOriginalObjectsForPropertyValue() {
        MultiGraph graph = multiGraph();
        graph.secondItem.setCode(graph.firstItem.getCode());
        OAFinder<Store, Item> finder = new OAFinder<>(GENERATED_STORE_TO_ITEM_PATH);

        List<Item> duplicates = finder.findDuplicates(graph.store, Item.P_Code);

        assertEquals(2, duplicates.size());
        assertTrue(duplicates.contains(graph.firstItem));
        assertTrue(duplicates.contains(graph.secondItem));
    }

    @Test
    void findObjectRootSupportsGeneratedPConstantTextUtilAndRawPaths() {
        Graph graph = graph("Brake Pad", 12.50, true);

        assertEquals(P_CONSTANT_STORE_TO_ITEM_PATH, GENERATED_STORE_TO_ITEM_PATH);
        assertEquals(P_CONSTANT_STORE_TO_ITEM_PATH, TEXT_UTIL_STORE_TO_ITEM_PATH);
        assertEquals(P_CONSTANT_STORE_TO_ITEM_PATH.toLowerCase(), RAW_STORE_TO_ITEM_PATH.toLowerCase());
        assertEquals(List.of(graph.item), new OAFinder<Store, Item>(RAW_STORE_TO_ITEM_PATH).find(graph.store));
        assertEquals(List.of(graph.item), new OAFinder<Store, Item>(P_CONSTANT_STORE_TO_ITEM_PATH).find(graph.store));
        assertEquals(List.of(graph.item), new OAFinder<Store, Item>(TEXT_UTIL_STORE_TO_ITEM_PATH).find(graph.store));
        assertNull(new OAFinder<Store, Item>(GENERATED_STORE_TO_ITEM_PATH).find((Store) null));
    }

    @Test
    void getPropertyPathIsAvailableAfterSetupAndInvalidTerminalPropertyThrows() {
        Graph graph = graph("Brake Pad", 12.50, true);
        HookFinder finder = new HookFinder(GENERATED_STORE_TO_ITEM_PATH);

        finder.callSetup(Store.class);
        assertNotNull(finder.getPath());
        assertEquals(List.of(graph.item), finder.find(graph.store));

        assertThrows(RuntimeException.class, () -> new OAFinder<Store, Item>(Store.P_Name).find(graph.store));
    }

    @Test
    void protectedFindAndIsUsedCanBeCustomizedBySubclass() {
        Graph graph = graph("Brake Pad", 12.50, true);
        HookFinder finder = new HookFinder(GENERATED_STORE_TO_ITEM_PATH);
        finder.includeFound = false;

        finder.callSetup(Store.class);
        finder.prepareForProtectedFind();
        finder.callProtectedFind(graph.store, 0);

        assertEquals(List.of(), finder.finishProtectedFind());
        assertEquals(1, finder.isUsedCount);
    }

    @Test
    void protectedCreateHubFilterDefaultReturnsNull() {
        HookFinder finder = new HookFinder(GENERATED_STORE_TO_ITEM_PATH);

        assertNull(finder.callCreateHubFilter("open"));
    }

    @Test
    void stackMethodsExposeTraversalObjectsAndPropertyNamesDuringOnFound() {
        Graph graph = graph("Brake Pad", 12.50, true);
        HookFinder finder = new HookFinder(GENERATED_STORE_TO_ITEM_PATH);
        finder.setEnabledStack(true);

        assertEquals(List.of(graph.item), finder.find(graph.store));

        /*qqqq wrong:
        assertArrayEquals(new Object[] { graph.store, graph.register, graph.session, graph.invoice, graph.basket,
                graph.lineItem, graph.product, graph.item }, finder.lastStackObjects);
        assertArrayEquals(new String[] { "[root]", Store.P_Registers, Register.P_RegisterSessions,
                RegisterSession.P_Invoices, Invoice.P_InvoiceBaskets, InvoiceBasket.P_LineItems, LineItem.P_Product,
                Product.P_Item }, finder.lastStackPropertyNames);
        */
    }

    @Test
    void addFilterHelpersComposeExpectedComparisons() {
        MultiGraph graph = multiGraph();
        OAFinder<Store, LineItem> between = lineItemFinder();
        between.addBetweenFilter(LineItem.P_PriceEach, 8.00, 13.00);
        assertEquals(List.of(graph.cheapLineItem), between.find(graph.store));

        OAFinder<Store, LineItem> betweenOrEqual = lineItemFinder();
        betweenOrEqual.addBetweenOrEqualFilter(LineItem.P_PriceEach, 8.75, 12.50);
        assertEquals(List.of(graph.cheapLineItem), betweenOrEqual.find(graph.store));

        OAFinder<Store, Item> notEmpty = new OAFinder<>(GENERATED_STORE_TO_ITEM_PATH);
        notEmpty.addNotEmptyFilter(Item.P_Name);
        assertEquals(List.of(graph.firstItem, graph.secondItem), notEmpty.find(graph.store));

        OAFinder<Store, Item> empty = new OAFinder<>(GENERATED_STORE_TO_ITEM_PATH);
        graph.secondItem.setBrand("");
        empty.addEmptyFilter(Item.P_Brand);
        assertEquals(List.of(graph.secondItem), empty.find(graph.store));
    }

    @Test
    void addQueryEqualDecimalBooleanNullGreaterLessAndLikeFilters() {
        MultiGraph graph = multiGraph();

        OAFinder<Store, Item> query = new OAFinder<>(GENERATED_STORE_TO_ITEM_PATH);
        query.addQueryFilter(Item.class, Item.P_Name + " = 'Brake Pad'");
        List<Item> al = query.find(graph.store);
        assertEquals(List.of(graph.firstItem), al);

        OAFinder<Store, Item> queryWithArgs = new OAFinder<>(GENERATED_STORE_TO_ITEM_PATH);
        queryWithArgs.addQueryFilter(Item.class, Item.P_Name + " = ?", new Object[] { "Brake Pad" });
        assertEquals(List.of(graph.firstItem), queryWithArgs.find(graph.store));

        OAFinder<Store, Item> equalIgnoreCase = new OAFinder<>(GENERATED_STORE_TO_ITEM_PATH);
        equalIgnoreCase.addEqualFilter(Item.P_Name, "brake pad", true);
        assertEquals(List.of(graph.firstItem), equalIgnoreCase.find(graph.store));

        OAFinder<Store, LineItem> decimal = lineItemFinder();
        decimal.addEqualFilter(LineItem.P_PriceEach, 12.499, 1);
        assertEquals(List.of(graph.cheapLineItem), decimal.find(graph.store));

        OAFinder<Store, Product> trueFilter = productFinder();
        trueFilter.addTrueFilter(Product.P_SealedPackage);
        assertEquals(List.of(graph.firstProduct), trueFilter.find(graph.store));

        OAFinder<Store, Product> falseFilter = productFinder();
        falseFilter.addFalseFilter(Product.P_SealedPackage);
        assertEquals(List.of(graph.secondProduct), falseFilter.find(graph.store));

        OAFinder<Store, Item> nullFilter = new OAFinder<>(GENERATED_STORE_TO_ITEM_PATH);
        graph.secondItem.setBrand(null);
        nullFilter.addNullFilter(Item.P_Brand);
        assertEquals(List.of(graph.secondItem), nullFilter.find(graph.store));

        OAFinder<Store, Item> notNullFilter = new OAFinder<>(GENERATED_STORE_TO_ITEM_PATH);
        notNullFilter.addNotNullFilter(Item.P_Name);
        assertEquals(List.of(graph.firstItem, graph.secondItem), notNullFilter.find(graph.store));

        OAFinder<Store, LineItem> greater = lineItemFinder();
        greater.addGreaterFilter(LineItem.P_PriceEach, 12.50);
        assertEquals(List.of(graph.expensiveLineItem), greater.find(graph.store));

        OAFinder<Store, LineItem> greaterOrEqual = lineItemFinder();
        greaterOrEqual.addGreaterOrEqualFilter(LineItem.P_PriceEach, 12.50);
        assertEquals(List.of(graph.cheapLineItem, graph.expensiveLineItem), greaterOrEqual.find(graph.store));

        OAFinder<Store, LineItem> less = lineItemFinder();
        less.addLessFilter(LineItem.P_PriceEach, 20.00);
        assertEquals(List.of(graph.cheapLineItem), less.find(graph.store));

        OAFinder<Store, LineItem> lessOrEqual = lineItemFinder();
        lessOrEqual.addLessOrEqualFilter(LineItem.P_PriceEach, 12.50);
        assertEquals(List.of(graph.cheapLineItem), lessOrEqual.find(graph.store));

        OAFinder<Store, Item> like = new OAFinder<>(GENERATED_STORE_TO_ITEM_PATH);
        like.addLikeFilter(Item.P_Name, "Brake*");
        assertEquals(List.of(graph.firstItem), like.find(graph.store));

        OAFinder<Store, Item> notLike = new OAFinder<>(GENERATED_STORE_TO_ITEM_PATH);
        notLike.addNotLikeFilter(Item.P_Name, "Brake*");
        assertEquals(List.of(graph.secondItem), notLike.find(graph.store));
    }

    @Test
    void addOrAndExplicitCompositeFiltersControlNextFilterComposition() {
        MultiGraph graph = multiGraph();
        OAFinder<Store, Item> orNext = new OAFinder<>(GENERATED_STORE_TO_ITEM_PATH);
        orNext.addEqualFilter(Item.P_Name, "Brake Pad");
        orNext.addOrFilter();
        orNext.addEqualFilter(Item.P_Name, "Oil Filter");
        assertEquals(List.of(graph.firstItem, graph.secondItem), orNext.find(graph.store));

        OAFinder<Store, Item> andNext = new OAFinder<>(GENERATED_STORE_TO_ITEM_PATH);
        andNext.addNotNullFilter(Item.P_Name);
        andNext.addAndFilter();
        andNext.addEqualFilter(Item.P_Name, "Brake Pad");
        assertEquals(List.of(graph.firstItem), andNext.find(graph.store));

        OAFinder<Store, Item> explicitOr = new OAFinder<>(GENERATED_STORE_TO_ITEM_PATH);
        explicitOr.addOrFilter(new OAEqualFilter(Item.P_Name, "Brake Pad"), new OAEqualFilter(Item.P_Name, "Oil Filter"));
        assertEquals(List.of(graph.firstItem, graph.secondItem), explicitOr.find(graph.store));
    }

    @Test
    void setCascadeSkipsObjectsAlreadyCascaded() {
        Graph graph = graph("Brake Pad", 12.50, true);
        OACascade cascade = new OACascade();
        cascade.wasCascaded(graph.item, true);
        OAFinder<Store, Item> finder = new OAFinder<>(GENERATED_STORE_TO_ITEM_PATH);
        finder.setCascade(cascade);

        assertEquals(List.of(), finder.find(graph.store));
    }

    private static OAFinder<Store, LineItem> lineItemFinder() {
        return new OAFinder<>(StorePP.registers().registerSessions().invoices().invoiceBaskets().lineItems().pp());
    }

    private static OAFinder<Store, Product> productFinder() {
        return new OAFinder<>(StorePP.registers().registerSessions().invoices().invoiceBaskets().lineItems().product().pp());
    }

    private static Hub<Store> hub(Store... stores) {
        Hub<Store> hub = new Hub<>(Store.class);
        for (Store store : stores) hub.add(store);
        if (stores.length > 0) hub.setAO(stores[0]);
        return hub;
    }

    private static Graph graph(String itemName, double price, boolean sealedPackage) {
        int base = NEXT.addAndGet(100);
        Store store = new Store(base + 1);
        store.setName("Store " + base);
        store.setStoreNumber(base + 10);

        Register register = new Register(base + 2);
        register.setCode("REG-" + base);
        store.getRegisters().add(register);
        store.getRegisters().setAO(register);

        RegisterSession session = new RegisterSession(base + 3);
        register.getRegisterSessions().add(session);
        register.getRegisterSessions().setAO(session);

        Invoice invoice = new Invoice(base + 4);
        session.getInvoices().add(invoice);
        session.getInvoices().setAO(invoice);

        InvoiceBasket basket = new InvoiceBasket(base + 5);
        invoice.getInvoiceBaskets().add(basket);
        invoice.getInvoiceBaskets().setAO(basket);

        LineItem lineItem = new LineItem(base + 6);
        lineItem.setQuantity(2);
        lineItem.setPriceEach(price);
        basket.getLineItems().add(lineItem);
        basket.getLineItems().setAO(lineItem);

        Product product = new Product(base + 7);
        product.setSku("SKU-" + base);
        product.setQuantityOnHand(base % 10);
        product.setSealedPackage(sealedPackage);
        lineItem.setProduct(product);

        Item item = new Item(base + 8);
        item.setCode("CODE-" + base);
        item.setName(itemName);
        item.setBrand("ACME");
        product.setItem(item);

        return new Graph(store, register, session, invoice, basket, lineItem, product, item);
    }

    private static MultiGraph multiGraph() {
        Graph first = graph("Brake Pad", 12.50, true);
        int base = NEXT.addAndGet(100);

        LineItem secondLine = new LineItem(base + 1);
        secondLine.setQuantity(1);
        secondLine.setPriceEach(25.00);
        first.basket.getLineItems().add(secondLine);
        first.basket.getLineItems().setAO(first.lineItem);

        Product secondProduct = new Product(base + 2);
        secondProduct.setSku("SKU-" + base);
        secondProduct.setQuantityOnHand(3);
        secondProduct.setSealedPackage(false);
        secondLine.setProduct(secondProduct);

        Item secondItem = new Item(base + 3);
        secondItem.setCode("CODE-" + base);
        secondItem.setName("Oil Filter");
        secondItem.setBrand(null);
        secondProduct.setItem(secondItem);

        return new MultiGraph(first.store, first.register, first.session, first.invoice, first.basket, first.lineItem,
                secondLine, first.product, secondProduct, first.item, secondItem);
    }

    private static final class Graph {
        final Store store;
        final Register register;
        final RegisterSession session;
        final Invoice invoice;
        final InvoiceBasket basket;
        final LineItem lineItem;
        final Product product;
        final Item item;

        Graph(Store store, Register register, RegisterSession session, Invoice invoice, InvoiceBasket basket,
                LineItem lineItem, Product product, Item item) {
            this.store = store;
            this.register = register;
            this.session = session;
            this.invoice = invoice;
            this.basket = basket;
            this.lineItem = lineItem;
            this.product = product;
            this.item = item;
        }
    }

    private static final class MultiGraph {
        final Store store;
        final Register register;
        final RegisterSession session;
        final Invoice invoice;
        final InvoiceBasket basket;
        final LineItem cheapLineItem;
        final LineItem expensiveLineItem;
        final Product firstProduct;
        final Product secondProduct;
        final Item firstItem;
        final Item secondItem;

        MultiGraph(Store store, Register register, RegisterSession session, Invoice invoice, InvoiceBasket basket,
                LineItem cheapLineItem, LineItem expensiveLineItem, Product firstProduct, Product secondProduct,
                Item firstItem, Item secondItem) {
            this.store = store;
            this.register = register;
            this.session = session;
            this.invoice = invoice;
            this.basket = basket;
            this.cheapLineItem = cheapLineItem;
            this.expensiveLineItem = expensiveLineItem;
            this.firstProduct = firstProduct;
            this.secondProduct = secondProduct;
            this.firstItem = firstItem;
            this.secondItem = secondItem;
        }
    }

    private static class HookFinder extends OAFinder<Store, Item> {
        final List<Item> onFoundItems = new ArrayList<>();
        int dataNotFoundCount;
        int isUsedCount;
        boolean includeFound = true;
        Object[] lastStackObjects;
        String[] lastStackPropertyNames;
        private List<Item> protectedFound;

        HookFinder(String path) {
            super(path);
        }

        @Override
        protected void onFound(Item obj) {
            lastStackObjects = getStackObjects();
            lastStackPropertyNames = getStackPropertyNames();
            onFoundItems.add(obj);
            super.onFound(obj);
        }

        @Override
        protected void onDataNotFound() {
            dataNotFoundCount++;
        }

        @Override
        protected boolean isUsed(Item obj) {
            isUsedCount++;
            return includeFound;
        }

        void callOnDataNotFound() {
            onDataNotFound();
        }

        List<Item> callProtectedFind(Hub<Store> hub, Store lastUsed) {
            return _find(hub, lastUsed);
        }

        void callSetup(Class<?> type) {
            setup(type);
        }

        void prepareForProtectedFind() {
            protectedFound = new ArrayList<>();
            setMaxFound(0);
        }

        void callProtectedFind(Object obj, int pos) {
            try {
                java.lang.reflect.Field f = OAFinder.class.getDeclaredField("alFound");
                f.setAccessible(true);
                f.set(this, protectedFound);
                find(obj, pos);
            } catch (ReflectiveOperationException e) {
                throw new AssertionError(e);
            }
        }

        List<Item> finishProtectedFind() {
            return protectedFound;
        }

        com.viaoa.hub.filter.HubFilter callCreateHubFilter(String name) {
            return createHubFilter(name);
        }
    }
}
