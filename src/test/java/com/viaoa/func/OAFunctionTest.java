package com.viaoa.func;

import static org.junit.jupiter.api.Assertions.*;

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
import com.viaoa.graph.api.internal.OAGraphInternal;
import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;
import com.viaoa.runtime.OARuntime;
import com.viaoa.text.OATextUtil;

class OAFunctionTest {

    private static final String RAW_STORE_TO_LINE_ITEMS =
            "registers.registerSessions.invoices.invoiceBaskets.lineItems";
    private static final String PP_STORE_TO_LINE_ITEMS = StorePP.registers().registerSessions().invoices()
            .invoiceBaskets().lineItems().pp();
    private static final String TEXT_UTIL_STORE_TO_LINE_ITEMS = OATextUtil.createPropertyPath(Store.P_Registers,
            Register.P_RegisterSessions, RegisterSession.P_Invoices, Invoice.P_InvoiceBaskets,
            InvoiceBasket.P_LineItems);
    private static final String RAW_STORE_TO_ITEM_NAME = RAW_STORE_TO_LINE_ITEMS + ".product.item.name";
    private static final String RAW_STORE_TO_PRICE_EACH = RAW_STORE_TO_LINE_ITEMS + "." + LineItem.P_PriceEach;

    @BeforeEach
    void beforeEach() {
        OAGraphInternal og = (OAGraphInternal) OARuntime.graph(Register.class);
    }
    @AfterEach
    void afterEach() {
        OAObject.setDebugMode(false);
        OARuntime.graph(Register.class).close();
    }

    @Test
    void countObjectPathCountsReachedObjectsAndHandlesNullInputs() {
        Store store = fixtureStore();

        assertEquals(2, OAFunction.count(store, RAW_STORE_TO_LINE_ITEMS));
        assertEquals(2, OAFunction.count(store, PP_STORE_TO_LINE_ITEMS));
        assertEquals(2, OAFunction.count(store, TEXT_UTIL_STORE_TO_LINE_ITEMS));
        assertEquals(0, OAFunction.count((Store) null, RAW_STORE_TO_LINE_ITEMS));
        assertEquals(0, OAFunction.count(store, null));
        assertEquals(0, OAFunction.count(store, ""));
    }

    @Test
    void countHubPathCountsAcrossAllHubRootsAndHandlesNullInputs() {
        Hub<Store> stores = new Hub<>(Store.class);
        stores.add(fixtureStore());
        stores.add(fixtureStore(200, "Second Store", 4.0, 1.0));

        assertEquals(4, OAFunction.count(stores, RAW_STORE_TO_LINE_ITEMS));
        assertEquals(0, OAFunction.count((Hub) null, RAW_STORE_TO_LINE_ITEMS));
        assertEquals(0, OAFunction.count(stores, null));
        assertEquals(0, OAFunction.count(stores, ""));
    }

    @Test
    void sumObjectPathAggregatesNumericTerminalValuesAndHandlesNullInputs() {
        Store store = fixtureStore();

        assertEquals(20.0, OAFunction.sum(store, RAW_STORE_TO_PRICE_EACH), 0.0001);
        assertEquals(0.0, OAFunction.sum((Store) null, RAW_STORE_TO_PRICE_EACH), 0.0001);
        assertEquals(0.0, OAFunction.sum(store, null), 0.0001);
        assertEquals(0.0, OAFunction.sum(store, ""), 0.0001);
    }

    @Test
    void sumHubPathAggregatesNumericTerminalValuesAcrossHubRootsAndHandlesNullInputs() {
        Hub<Store> stores = new Hub<>(Store.class);
        stores.add(fixtureStore());
        stores.add(fixtureStore(200, "Second Store", 4.0, 1.0));

        assertEquals(25.0, OAFunction.sum(stores, RAW_STORE_TO_PRICE_EACH), 0.0001);
        assertEquals(0.0, OAFunction.sum((Hub) null, RAW_STORE_TO_PRICE_EACH), 0.0001);
        assertEquals(0.0, OAFunction.sum(stores, null), 0.0001);
        assertEquals(0.0, OAFunction.sum(stores, ""), 0.0001);
    }

    @Test
    void sumObjectNavigationAndTerminalPropertyAggregatesReachedObjects() {
        Store store = fixtureStore();

        assertEquals(20.0, OAFunction.sum(store, RAW_STORE_TO_LINE_ITEMS, LineItem.P_PriceEach), 0.0001);
        assertEquals(0.0, OAFunction.sum((Store) null, RAW_STORE_TO_LINE_ITEMS, LineItem.P_PriceEach), 0.0001);
        assertEquals(0.0, OAFunction.sum(store, RAW_STORE_TO_LINE_ITEMS, null), 0.0001);
    }

    @Test
    void sumHubNavigationAndTerminalPropertyAggregatesReachedObjects() {
        Hub<Store> stores = new Hub<>(Store.class);
        stores.add(fixtureStore());
        stores.add(fixtureStore(200, "Second Store", 4.0, 1.0));

        assertEquals(25.0, OAFunction.sum(stores, RAW_STORE_TO_LINE_ITEMS, LineItem.P_PriceEach), 0.0001);
        assertEquals(0.0, OAFunction.sum((Hub) null, RAW_STORE_TO_LINE_ITEMS, LineItem.P_PriceEach), 0.0001);
        assertEquals(0.0, OAFunction.sum(stores, RAW_STORE_TO_LINE_ITEMS, null), 0.0001);
    }

    @Test
    void maxObjectPathUsesOaCompareAndHandlesNullInputs() {
        Store store = fixtureStore();

        assertEquals(12.5, OAFunction.max(store, RAW_STORE_TO_PRICE_EACH));
        assertEquals("Oil Filter", OAFunction.max(store, RAW_STORE_TO_ITEM_NAME));
        assertEquals(0, OAFunction.max((Store) null, RAW_STORE_TO_PRICE_EACH));
        assertEquals(0, OAFunction.max(store, null));
    }

    @Test
    void maxHubPathUsesOaCompareAcrossHubRootsAndHandlesNullInputs() {
        Hub<Store> stores = new Hub<>(Store.class);
        stores.add(fixtureStore());
        stores.add(fixtureStore(200, "Second Store", 99.0, 1.0));

        assertEquals(99.0, OAFunction.max(stores, RAW_STORE_TO_PRICE_EACH));
        assertEquals(0, OAFunction.max((Hub) null, RAW_STORE_TO_PRICE_EACH));
        assertEquals(0, OAFunction.max(stores, null));
    }

    @Test
    void maxObjectNavigationAndTerminalPropertyUsesReachedObjects() {
        Store store = fixtureStore();

        assertEquals(12.5, OAFunction.max(store, RAW_STORE_TO_LINE_ITEMS, LineItem.P_PriceEach));
        assertNull(OAFunction.max(store, RAW_STORE_TO_LINE_ITEMS, LineItem.P_SerialCode));
        assertEquals(0, OAFunction.max((Store) null, RAW_STORE_TO_LINE_ITEMS, LineItem.P_PriceEach));
        assertEquals(0, OAFunction.max(store, RAW_STORE_TO_LINE_ITEMS, null));
    }

    @Test
    void maxHubNavigationAndTerminalPropertyUsesReachedObjects() {
        Hub<Store> stores = new Hub<>(Store.class);
        stores.add(fixtureStore());
        stores.add(fixtureStore(200, "Second Store", 99.0, 1.0));

        assertEquals(99.0, OAFunction.max(stores, RAW_STORE_TO_LINE_ITEMS, LineItem.P_PriceEach));
        assertNull(OAFunction.max(stores, RAW_STORE_TO_LINE_ITEMS, LineItem.P_SerialCode));
        assertEquals(0, OAFunction.max((Hub) null, RAW_STORE_TO_LINE_ITEMS, LineItem.P_PriceEach));
        assertEquals(0, OAFunction.max(stores, RAW_STORE_TO_LINE_ITEMS, null));
    }

    @Test
    void minObjectPathUsesOaCompareAndHandlesNullInputs() {
        Store store = fixtureStore();

        assertEquals(7.5, OAFunction.min(store, RAW_STORE_TO_PRICE_EACH));
        assertEquals("Brake Pads", OAFunction.min(store, RAW_STORE_TO_ITEM_NAME));
        assertEquals(0, OAFunction.min((Store) null, RAW_STORE_TO_PRICE_EACH));
        assertEquals(0, OAFunction.min(store, null));
    }

    @Test
    void minHubPathUsesOaCompareAcrossHubRootsAndHandlesNullInputs() {
        Hub<Store> stores = new Hub<>(Store.class);
        stores.add(fixtureStore());
        stores.add(fixtureStore(200, "Second Store", 4.0, 1.0));

        assertEquals(1.0, OAFunction.min(stores, RAW_STORE_TO_PRICE_EACH));
        assertEquals(0, OAFunction.min((Hub) null, RAW_STORE_TO_PRICE_EACH));
        assertEquals(0, OAFunction.min(stores, null));
    }

    @Test
    void minObjectNavigationAndTerminalPropertyUsesReachedObjects() {
        Store store = fixtureStore();

        assertEquals(7.5, OAFunction.min(store, RAW_STORE_TO_LINE_ITEMS, LineItem.P_PriceEach));
        assertNull(OAFunction.min(store, RAW_STORE_TO_LINE_ITEMS, LineItem.P_SerialCode));
        assertEquals(0, OAFunction.min((Store) null, RAW_STORE_TO_LINE_ITEMS, LineItem.P_PriceEach));
        assertEquals(0, OAFunction.min(store, RAW_STORE_TO_LINE_ITEMS, null));
    }

    @Test
    void minHubNavigationAndTerminalPropertyUsesReachedObjects() {
        Hub<Store> stores = new Hub<>(Store.class);
        stores.add(fixtureStore());
        stores.add(fixtureStore(200, "Second Store", 4.0, 1.0));

        assertEquals(1.0, OAFunction.min(stores, RAW_STORE_TO_LINE_ITEMS, LineItem.P_PriceEach));
        assertNull(OAFunction.min(stores, RAW_STORE_TO_LINE_ITEMS, LineItem.P_SerialCode));
        assertEquals(0, OAFunction.min((Hub) null, RAW_STORE_TO_LINE_ITEMS, LineItem.P_PriceEach));
        assertEquals(0, OAFunction.min(stores, RAW_STORE_TO_LINE_ITEMS, null));
    }

    @Test
    void templateObjectProcessesOaTemplateAndHandlesNullInputs() {
        Store store = fixtureStore();

        assertEquals("Store 100 has Brake Pads, Oil Filter", OAFunction.template(store,
                "Store <%= storeNumber %> has <%= " + RAW_STORE_TO_ITEM_NAME + " %>"));
        assertNull(OAFunction.template((Store) null, "<%= name %>"));
        assertNull(OAFunction.template(store, null));
        assertNull(OAFunction.template(store, ""));
    }

    @Test
    void templateHubProcessesOaTemplateForeachAndHandlesNullInputs() {
        Hub<Store> stores = new Hub<>(Store.class);
        stores.add(fixtureStore());
        stores.add(fixtureStore(200, "Second Store", 4.0, 1.0));

        assertEquals("100;200;", OAFunction.template(stores, "<%=foreach%><%= storeNumber %>;<%=end%>"));
        assertNull(OAFunction.template((Hub) null, "<%=foreach%><%= name %><%=end%>"));
        assertNull(OAFunction.template(stores, null));
        assertNull(OAFunction.template(stores, ""));
    }

    @Test
    void lengthObjectReturnsStringLengthAndZeroForNullOrNonStringValues() {
        Store store = fixtureStore();

        assertEquals(10, OAFunction.length(store, Store.P_Name));
        assertEquals(0, OAFunction.length(store, Store.P_StoreNumber));
        assertThrows(NullPointerException.class, () -> OAFunction.length((Store) null, Store.P_Name));
    }

    @Test
    void lengthHubSumsStringLengthsAndHandlesNullHub() {
        Hub<Store> stores = new Hub<>(Store.class);
        stores.add(fixtureStore());
        stores.add(fixtureStore(200, "Second Store", 4.0, 1.0));

        assertEquals(22, OAFunction.length(stores, Store.P_Name));
        assertEquals(0, OAFunction.length(stores, Store.P_StoreNumber));
        assertEquals(0, OAFunction.length((Hub) null, Store.P_Name));
    }

    private static Store fixtureStore() {
        return fixtureStore(100, "Main Store", 12.5, 7.5);
    }

    private static Store fixtureStore(int storeNumber, String storeName, double price1, double price2) {
        Store store = new Store(storeNumber);
        store.setStoreNumber(storeNumber);
        store.setName(storeName);

        Register register = new Register(storeNumber + 1);
        register.setCode("R" + storeNumber);
        RegisterSession session = new RegisterSession(storeNumber + 2);
        Invoice invoice = new Invoice(storeNumber + 3);
        InvoiceBasket basket = new InvoiceBasket(storeNumber + 4);
        LineItem line1 = lineItem(storeNumber + 5, price1, "SKU-" + storeNumber + "-A", "Brake Pads");
        LineItem line2 = lineItem(storeNumber + 6, price2, "SKU-" + storeNumber + "-B", "Oil Filter");

        store.getRegisters().add(register);
        register.getRegisterSessions().add(session);
        session.getInvoices().add(invoice);
        invoice.getInvoiceBaskets().add(basket);
        basket.getLineItems().add(line1);
        basket.getLineItems().add(line2);
        return store;
    }

    private static LineItem lineItem(int id, double price, String sku, String itemName) {
        LineItem line = new LineItem(id);
        line.setQuantity(1);
        line.setPriceEach(price);
        Product product = new Product(id + 1000);
        product.setSku(sku);
        Item item = new Item(id + 2000);
        item.setCode(sku);
        item.setName(itemName);
        line.setProduct(product);
        product.setItem(item);
        return line;
    }
}
