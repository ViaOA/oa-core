package com.viaoa.trigger;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
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
import com.test.pos.model.oa.propertypath.StorePP;
import com.viaoa.graph.api.internal.OAGraphInternal;
import com.viaoa.hub.HubEvent;
import com.viaoa.runtime.OARuntime;
import com.viaoa.text.OATextUtil;

class OATriggerTest {

    private final List<OATrigger> registeredTriggers = new ArrayList<>();

    @BeforeEach
    void beforeEach() {
        OAGraphInternal og = (OAGraphInternal) OARuntime.graph(Register.class);
    }

    @AfterEach
    void afterEach() {
        for (OATrigger trigger : registeredTriggers) {
            OARuntime.graph(trigger.getRootClass()).services().triggers().removeTrigger(trigger);
        }
        registeredTriggers.clear();
        OARuntime.graph(Register.class).close();
    }
    
    @Test
    void constructorWithPropertyPathArrayBindsRootClassListenerPathsAndFlags() {
        OATriggerListener<Store> listener = (objRoot, hubEvent, propertyPathFromRoot) -> {
        };
        String rawPath = Store.P_Registers + "." + Register.P_RegisterSessions + "." + RegisterSession.P_Invoices
                + "." + Invoice.P_InvoiceBaskets + "." + InvoiceBasket.P_LineItems + "." + LineItem.P_Product + "."
                + Product.P_Sku;
        String generatedPath = StorePP.registers().registerSessions().invoices().invoiceBaskets().lineItems().product()
                .sku();
        String textUtilPath = OATextUtil.createPropertyPath(Store.P_Registers, Register.P_RegisterSessions,
                RegisterSession.P_Invoices, Invoice.P_InvoiceBaskets, InvoiceBasket.P_LineItems, LineItem.P_Product,
                Product.P_Sku);
        String[] paths = { rawPath, generatedPath, textUtilPath };

        OATrigger trigger = new OATrigger("storeProductSku", Store.class, listener, paths, true, true, true, true);

        assertEquals(Store.class, trigger.getRootClass());
        assertSame(listener, trigger.getTriggerListener());
        assertSame(paths, trigger.getPropertyPaths());
        assertArrayEquals(new String[] { rawPath, rawPath, rawPath }, trigger.getPropertyPaths());
        assertTrue(trigger.getOnlyUseLoadedData());
        assertTrue(trigger.getServerSideOnly());
        assertTrue(trigger.getUseBackgroundThread());
        assertTrue(trigger.getUseBackgroundThreadIfNeeded());
        assertEquals("storeProductSku", trigger.getName());
    }

    @Test
    void constructorWithSinglePropertyPathWrapsPathAndStoresFalseFlags() {
        OATriggerListener<Store> listener = (objRoot, hubEvent, propertyPathFromRoot) -> {
        };

        OATrigger trigger = new OATrigger("storeName", Store.class, listener, Store.P_Name, false, false, false,
                false);

        assertEquals(Store.class, trigger.getRootClass());
        assertSame(listener, trigger.getTriggerListener());
        assertArrayEquals(new String[] { Store.P_Name }, trigger.getPropertyPaths());
        assertFalse(trigger.getOnlyUseLoadedData());
        assertFalse(trigger.getServerSideOnly());
        assertFalse(trigger.getUseBackgroundThread());
        assertFalse(trigger.getUseBackgroundThreadIfNeeded());
        assertEquals("storeName", trigger.getName());
    }

    @Test
    void constructorsAllowNullValuesByCurrentContract() {
        OATrigger arrayTrigger = new OATrigger(null, null, null, (String[]) null, false, false, false, false);
        OATrigger singleTrigger = new OATrigger(null, null, null, (String) null, false, false, false, false);

        assertNull(arrayTrigger.getName());
        assertNull(arrayTrigger.getRootClass());
        assertNull(arrayTrigger.getTriggerListener());
        assertNull(arrayTrigger.getPropertyPaths());
        assertArrayEquals(new String[] { null }, singleTrigger.getPropertyPaths());
    }

    @Test
    void getRootClassReturnsConfiguredClass() {
        OATrigger trigger = newTrigger("rootClass", Store.P_Name);

        assertEquals(Store.class, trigger.getRootClass());
    }

    @Test
    void getDependentTriggersReturnsConfiguredArray() {
        OATrigger parent = newTrigger("parent", Store.P_Name);
        OATrigger child = newTrigger("child", Store.P_StoreNumber);
        OATrigger[] children = { child };
        parent.setDependentTriggers(children);

        assertSame(children, parent.getDependentTriggers());
    }

    @Test
    void getTriggerListenerReturnsConfiguredListener() {
        OATriggerListener<Store> listener = (objRoot, hubEvent, propertyPathFromRoot) -> {
        };
        OATrigger trigger = new OATrigger("listener", Store.class, listener, Store.P_Name, false, false, false, false);

        assertSame(listener, trigger.getTriggerListener());
    }

    @Test
    void getUseBackgroundThreadReturnsConfiguredFlag() {
        assertTrue(new OATrigger("flag", Store.class, null, Store.P_Name, false, false, true, false)
                .getUseBackgroundThread());
        assertFalse(new OATrigger("flag", Store.class, null, Store.P_Name, false, false, false, false)
                .getUseBackgroundThread());
    }

    @Test
    void getServerSideOnlyReturnsConfiguredFlag() {
        assertTrue(new OATrigger("flag", Store.class, null, Store.P_Name, false, true, false, false)
                .getServerSideOnly());
        assertFalse(new OATrigger("flag", Store.class, null, Store.P_Name, false, false, false, false)
                .getServerSideOnly());
    }

    @Test
    void getOnlyUseLoadedDataReturnsConfiguredFlag() {
        assertTrue(new OATrigger("flag", Store.class, null, Store.P_Name, true, false, false, false)
                .getOnlyUseLoadedData());
        assertFalse(new OATrigger("flag", Store.class, null, Store.P_Name, false, false, false, false)
                .getOnlyUseLoadedData());
    }

    @Test
    void getUseBackgroundThreadIfNeededReturnsConfiguredFlag() {
        assertTrue(new OATrigger("flag", Store.class, null, Store.P_Name, false, false, false, true)
                .getUseBackgroundThreadIfNeeded());
        assertFalse(new OATrigger("flag", Store.class, null, Store.P_Name, false, false, false, false)
                .getUseBackgroundThreadIfNeeded());
    }

    @Test
    void getPropertyPathsReturnsConfiguredArrayByCurrentContract() {
        String[] paths = { Store.P_Name };
        OATrigger trigger = new OATrigger("paths", Store.class, null, paths, false, false, false, false);

        assertSame(paths, trigger.getPropertyPaths());

        paths[0] = Store.P_StoreNumber;
        assertArrayEquals(new String[] { Store.P_StoreNumber }, trigger.getPropertyPaths());
    }

    @Test
    void getNameReturnsConfiguredName() {
        OATrigger trigger = newTrigger("namedTrigger", Store.P_Name);

        assertEquals("namedTrigger", trigger.getName());
    }

    @Test
    void geDependentTriggersReturnsSameValueAsGetDependentTriggers() {
        OATrigger parent = newTrigger("parent", Store.P_Name);
        OATrigger child = newTrigger("child", Store.P_StoreNumber);
        OATrigger[] children = { child };
        parent.setDependentTriggers(children);

        assertSame(children, parent.geDependentTriggers());
        assertSame(parent.getDependentTriggers(), parent.geDependentTriggers());
    }

    @Test
    void setDependentTriggersStoresArrayByCurrentContract() {
        OATrigger parent = newTrigger("parent", Store.P_Name);
        OATrigger child = newTrigger("child", Store.P_StoreNumber);
        OATrigger[] children = { child };

        parent.setDependentTriggers(children);

        assertSame(children, parent.getDependentTriggers());

        children[0] = null;
        assertArrayEquals(new OATrigger[] { null }, parent.getDependentTriggers());
    }

    @Test
    void registeredTriggerFiresForMatchingPropertyAndStopsAfterRemoval() {
        AtomicInteger count = new AtomicInteger();
        OATrigger trigger = new OATrigger("storeNameFire", Store.class,
                (OATriggerListener<Store>) (objRoot, hubEvent, propertyPathFromRoot) -> {
                    assertSame(hubEvent.getObject(), objRoot);
                    assertEquals(Store.P_Name, hubEvent.getPropertyName());
                    count.incrementAndGet();
                }, Store.P_Name, false, false, false, false);
        register(trigger);
        Store store = new Store();

        store.setName("Main");
        assertEquals(1, count.get());

        OARuntime.graph(Store.class).services().triggers().removeTrigger(trigger);
        registeredTriggers.remove(trigger);
        store.setName("Removed");

        assertEquals(1, count.get());
    }

    @Test
    void registeredTriggerDoesNotFireForNonMatchingProperty() {
        AtomicInteger count = new AtomicInteger();
        OATrigger trigger = new OATrigger("storeNameOnly", Store.class,
                (OATriggerListener<Store>) (objRoot, hubEvent, propertyPathFromRoot) -> count.incrementAndGet(),
                Store.P_Name, false, false, false, false);
        register(trigger);
        Store store = new Store();

        store.setStoreNumber(42);

        assertEquals(0, count.get());
    }

    private OATrigger newTrigger(String name, String propertyPath) {
        return new OATrigger(name, Store.class, (objRoot, hubEvent, propertyPathFromRoot) -> {
        }, propertyPath, false, false, false, false);
    }

    private void register(OATrigger trigger) {
        OARuntime.graph(trigger.getRootClass()).services().triggers().addTrigger(trigger);
        registeredTriggers.add(trigger);
    }

}
