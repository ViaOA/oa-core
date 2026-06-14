package com.viaoa.graph;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.test.pos.model.oa.Invoice;
import com.test.pos.model.oa.InvoiceBasket;
import com.test.pos.model.oa.LineItem;
import com.test.pos.model.oa.Product;
import com.test.pos.model.oa.Register;
import com.test.pos.model.oa.RegisterSession;
import com.test.pos.model.oa.Store;
import com.viaoa.filter.OAFilter;
import com.viaoa.find.OAFinder;
import com.viaoa.graph.service.OAObjectService;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubEvent;
import com.viaoa.hub.HubListenerAdapter;
import com.viaoa.hub.copy.HubCopy;
import com.viaoa.hub.filter.HubFilter;
import com.viaoa.hub.merge.HubMerger;
import com.viaoa.hub.view.HubFlattened;
import com.viaoa.hub.view.OALeftJoin;
import com.viaoa.metadata.OAObjectInfo;
import com.viaoa.runtime.OARuntime;
import com.viaoa.select.OASelect;
import com.viaoa.text.OATextUtil;

class OAGraphTest {
    private OAGraph graph;

    @BeforeEach
    void beforeEach() {
        graph = OARuntime.graph(Register.class);
        clearCache();
    }

    @AfterEach
    void afterEach() {
        clearCache();
    }

    private static void clearCache() {
        OAGraphInternal og = (OAGraphInternal) OARuntime.graph(Register.class);
        OAObjectService os = (OAObjectService) og.objectsInternal();
        os.getOAObjectCacheService().removeAllObjects();
    }

    @Test
    void graphIdentityAndPublicServiceAccessorsAreStable() {
        assertEquals(Register.class.getPackage().getName(), graph.getPackageName());
        assertSame(graph, OARuntime.graph(Store.class));
        assertNotNull(graph.sync());
        assertNotNull(graph.replication());

        OAGraphInternal internal = (OAGraphInternal) graph;
        assertNotNull(internal.objectsInternal());
        assertNotNull(internal.hubsInternal());
        assertNotNull(internal.syncInternal());
        assertNotNull(internal.replInternal());
        assertNotNull(internal.triggerInternal());
    }

    @Test
    void createAndCreateHubUseOaposModelTypes() {
        Register register = graph.create(Register.class);
        assertNotNull(register);
        assertEquals(Register.class, register.getClass());

        Hub<Register> hub = graph.createHub(Register.class);
        assertEquals(Register.class, hub.getObjectClass());
        assertTrue(hub.isEmpty());
    }

    @Test
    void saveDeleteAndNullInputsAreNoOpsForUnsavedInMemoryObjects() {
        Register register = graph.create(Register.class);
        Hub<Register> hub = graph.createHub(Register.class);
        hub.add(register);

        assertDoesNotThrow(() -> {
            graph.save(register);
            graph.save(hub);
            graph.delete(register);
            graph.delete(hub);
            graph.save((com.viaoa.object.OAObject) null);
            graph.save((Hub<?>) null);
            graph.delete((com.viaoa.object.OAObject) null);
            graph.delete((Hub<?>) null);
        });
    }

    @Test
    void getSelectAndSelectCreateConfiguredSelectorsAndHubs() {
        assertNull(graph.select((Class<Register>) null, null, null));

        Hub<Register> hub = graph.select(Register.class, "code = ?", Register.P_Code, "A");
        assertEquals(Register.class, hub.getObjectClass());

        Hub<Register> existing = graph.createHub(Register.class);
        assertDoesNotThrow(() -> graph.select(existing, "code = ?", Register.P_Code, "A"));
        assertDoesNotThrow(() -> graph.select((Hub<?>) null, "code = ?", Register.P_Code, "A"));

        OASelect<Register> select = graph.getSelect(Register.class, "code = ?", Register.P_Code, "A");
        assertNotNull(select);
    }

    @Test
    void finderVerbsTraverseOaposObjectGraphUsingRawConstantsAndTextUtilPath() {
        Store store = new Store();
        Register register = new Register();
        RegisterSession session = new RegisterSession();
        Invoice invoice = new Invoice();
        InvoiceBasket basket = new InvoiceBasket();
        LineItem lineItem = new LineItem();
        Product product = new Product();
        product.setSku("SKU-1");
        lineItem.setProduct(product);
        basket.getLineItems().add(lineItem);
        invoice.getInvoiceBaskets().add(basket);
        session.getInvoices().add(invoice);
        register.getRegisterSessions().add(session);
        store.getRegisters().add(register);

        String path = OATextUtil.createPropertyPath(
                Store.P_Registers,
                Register.P_RegisterSessions,
                RegisterSession.P_Invoices,
                Invoice.P_InvoiceBaskets,
                InvoiceBasket.P_LineItems,
                LineItem.P_Product);

        OAFinder<Store, Product> finder = graph.finder(store, Product.class, path);
        assertSame(product, finder.findFirst());

        Hub<Store> stores = new Hub<>(Store.class);
        stores.add(store);
        OAFinder<Store, Product> hubFinder = graph.finder(stores, Product.class, path, true);
        assertSame(product, hubFinder.findFirst());
    }

    @Test
    void observeRegistersHubListenerThroughGraphVerb() {
        Hub<Register> hub = graph.createHub(Register.class);
        AtomicInteger adds = new AtomicInteger();
        graph.observe(hub, new HubListenerAdapter<Register>() {
            @Override
            public void afterAdd(HubEvent<Register> e) {
                adds.incrementAndGet();
            }
        });
        graph.observe(null, new HubListenerAdapter<>());
        graph.observe(hub, null);

        hub.add(new Register());
        assertEquals(1, adds.get());
    }

    @Test
    void detailShareCopyFilterAndCombineUseHubParentServices() {
        Store store = new Store();
        Register register = new Register();
        store.getRegisters().add(register);

        Hub<Store> stores = graph.createHub(Store.class);
        stores.add(store);
        stores.setAO(store);

        Hub<?> detail = graph.detail(stores, Store.P_Registers);
        assertNotNull(detail);
        assertEquals(1, detail.size());
        assertSame(register, detail.getAt(0));
        assertNull(graph.detail(null, Store.P_Registers));

        Hub<Register> source = graph.createHub(Register.class);
        Hub<Register> shared = graph.createHub(Register.class);
        source.add(register);
        graph.share(shared, source, true);
        assertEquals(source.toList(), shared.toList());
        graph.share(null, source, true);

        Hub<Register> copied = graph.createHub(Register.class);
        HubCopy<Register> copy = graph.copy(source, copied, true);
        assertNotNull(copy);
        assertEquals(source.toList(), copied.toList());

        Hub<Register> filtered = graph.createHub(Register.class);
        OAFilter<Register> keepAll = r -> true;
        HubFilter<Register> filter = graph.filter(source, filtered, keepAll, Register.P_Code);
        assertNotNull(filter);
        assertEquals(source.toList(), filtered.toList());
        assertNull(graph.filter(null, filtered));

        Hub<Register> combined = graph.createHub(Register.class);
        graph.combine(combined, source);
        assertEquals(source.toList(), combined.toList());
        graph.combine(null, source);
    }

    @Test
    void mergeFlattenLeftJoinAndInfoReturnExpectedRuntimeObjects() {
        Hub<Store> stores = graph.createHub(Store.class);
        Store store = new Store();
        Register register = new Register();
        store.getRegisters().add(register);
        stores.add(store);

        Hub<Register> merged = graph.createHub(Register.class);
        HubMerger<Store, Register> merger = graph.merge(stores, merged, Store.P_Registers);
        assertNotNull(merger);
        assertTrue(merged.contains(register));

        Hub<Register> flat = graph.createHub(Register.class);
        HubFlattened<Register> flattened = graph.flatten(store.getRegisters(), flat);
        assertNotNull(flattened);
        assertEquals(store.getRegisters().toList(), flat.toList());
        assertNull(graph.flatten(null, flat));
        assertNull(graph.flatten(store.getRegisters(), null));
        assertNull(graph.flatten(null));

        Hub<OALeftJoin<Store, Register>> leftJoin = graph.leftJoin(stores, store.getRegisters(), Store.P_Registers, false);
        assertNotNull(leftJoin);

        OAObjectInfo infoClass = graph.info(Register.class);
        assertNotNull(infoClass);
        assertSame(infoClass, graph.info(new Register()));
        assertSame(infoClass, graph.info(new Hub<>(Register.class)));
        assertNull(graph.info((Class<com.viaoa.object.OAObject>) null));
        assertNull(graph.info((com.viaoa.object.OAObject) null));
        assertNull(graph.info((Hub<?>) null));
    }

    @Test
    void addAndRemoveTriggerAcceptNullSafely() {
        assertDoesNotThrow(() -> {
            graph.addTrigger(null);
            graph.removeTrigger(null);
        });
    }
}
