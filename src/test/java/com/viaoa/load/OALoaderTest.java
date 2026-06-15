package com.viaoa.load;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;

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
import com.viaoa.graph.service.OAObjectInternalService;
import com.viaoa.hub.Hub;
import com.viaoa.runtime.OARuntime;
import com.viaoa.select.OASelect;

class OALoaderTest {

    private static final String STORE_TO_LINE_ITEMS = StorePP.registers().registerSessions().invoices()
            .invoiceBaskets().lineItems().pp();
    private static final String STORE_TO_ITEM_NAME = StorePP.registers().registerSessions().invoices()
            .invoiceBaskets().lineItems().product().item().name();

    private static class ExposedLoader extends OALoader<Store, LineItem> {
        ExposedLoader(int threadCount, String propPath) {
            super(threadCount, propPath);
        }

        void setupFor(Class<?> clazz) {
            setup(clazz);
        }

        void loadObject(Store store) {
            _load(store);
        }
    }

    @BeforeEach
    void beforeEach() {
        OAGraphInternal og = (OAGraphInternal) OARuntime.graph(Register.class);
        OAObjectInternalService os = (OAObjectInternalService) og.objectsInternal();
        os.getOAObjectCacheService().removeAllObjects();
    }

    @Test
    void constructorInitializesCountersAndCapsAreInternalOnly() {
        OALoader<Store, LineItem> loader = new OALoader<>(100, STORE_TO_LINE_ITEMS);

        assertEquals(0, loader.getVisitCount());
        assertEquals(0, loader.getNotLoadedCount());
    }

    @Test
    void stopBeforeLoadDoesNotPreventNextLoadBecauseLoadResetsStopFlag() {
        OALoader<Store, LineItem> loader = new OALoader<>(0, STORE_TO_LINE_ITEMS);

        loader.stop();
        loader.load(fixtureStore());

        assertTrue(loader.getVisitCount() > 0);
    }

    @Test
    void getVisitCountAndGetNotLoadedCountReflectObjectLoadTraversal() {
        OALoader<Store, LineItem> loader = new OALoader<>(0, STORE_TO_LINE_ITEMS);

        loader.load(fixtureStore());

        assertTrue(loader.getVisitCount() >= 5);
        assertEquals(0, loader.getNotLoadedCount());
    }

    @Test
    void loadHubTraversesAllHubRootsAndCompletesWaitState() {
        Hub<Store> stores = new Hub<>(Store.class);
        stores.add(fixtureStore());
        stores.add(fixtureStore(200));
        OALoader<Store, LineItem> loader = new OALoader<>(0, STORE_TO_LINE_ITEMS);

        loader.load(stores);

        assertTrue(loader.getVisitCount() >= 10);
        assertEquals(0, loader.getNotLoadedCount());
        assertTimeoutPreemptively(Duration.ofSeconds(1), loader::waitUntilDone);
    }

    @Test
    void loadHubNullIsNoOpForCounters() {
        OALoader<Store, LineItem> loader = new OALoader<>(0, STORE_TO_LINE_ITEMS);

        loader.load((Hub<Store>) null);

        assertEquals(0, loader.getVisitCount());
        assertEquals(0, loader.getNotLoadedCount());
    }

    @Test
    void loadSelectNullIsNoOpForCounters() {
        OALoader<Store, LineItem> loader = new OALoader<>(0, STORE_TO_LINE_ITEMS);

        loader.load((OASelect<Store>) null);

        assertEquals(0, loader.getVisitCount());
        assertEquals(0, loader.getNotLoadedCount());
    }

    @Test
    void loadObjectTraversesConfiguredObjectPathAndCompletesWaitState() {
        OALoader<Store, LineItem> loader = new OALoader<>(0, STORE_TO_LINE_ITEMS);

        loader.load(fixtureStore());

        assertTrue(loader.getVisitCount() >= 5);
        assertEquals(0, loader.getNotLoadedCount());
        assertTimeoutPreemptively(Duration.ofSeconds(1), loader::waitUntilDone);
    }

    @Test
    void loadObjectNullIsNoOpForCounters() {
        OALoader<Store, LineItem> loader = new OALoader<>(0, STORE_TO_LINE_ITEMS);

        loader.load((Store) null);

        assertEquals(0, loader.getVisitCount());
        assertEquals(0, loader.getNotLoadedCount());
    }

    @Test
    void protectedLoadNullIsNoOp() {
        ExposedLoader loader = new ExposedLoader(0, STORE_TO_LINE_ITEMS);
        loader.setupFor(Store.class);

        loader.loadObject(null);

        assertEquals(0, loader.getVisitCount());
        assertEquals(0, loader.getNotLoadedCount());
    }

    @Test
    void setupAcceptsObjectEndingPathAndRejectsScalarEndingPath() {
        ExposedLoader loader = new ExposedLoader(0, STORE_TO_LINE_ITEMS);

        loader.setupFor(Store.class);
        assertEquals(0, loader.getVisitCount());

        ExposedLoader bad = new ExposedLoader(0, STORE_TO_ITEM_NAME);
        assertThrows(RuntimeException.class, () -> bad.setupFor(Store.class));
    }

    private static Store fixtureStore() {
        return fixtureStore(100);
    }

    private static Store fixtureStore(int id) {
        Store store = new Store(id);
        store.setStoreNumber(id);
        store.setName("Store " + id);
        Register register = new Register(id + 1);
        RegisterSession session = new RegisterSession(id + 2);
        Invoice invoice = new Invoice(id + 3);
        InvoiceBasket basket = new InvoiceBasket(id + 4);
        LineItem line = new LineItem(id + 5);
        line.setQuantity(1);
        line.setPriceEach(12.5);
        Product product = new Product(id + 6);
        product.setSku("SKU-" + id);
        Item item = new Item(id + 7);
        item.setName("Item " + id);

        store.getRegisters().add(register);
        register.getRegisterSessions().add(session);
        session.getInvoices().add(invoice);
        invoice.getInvoiceBaskets().add(basket);
        basket.getLineItems().add(line);
        line.setProduct(product);
        product.setItem(item);
        return store;
    }
}
