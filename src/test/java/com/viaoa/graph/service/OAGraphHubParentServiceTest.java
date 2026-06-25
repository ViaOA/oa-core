package com.viaoa.graph.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.test.pos.model.oa.Register;
import com.test.pos.model.oa.Store;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubEvent;
import com.viaoa.hub.HubListenerAdapter;
import com.viaoa.oa.OA;
import com.viaoa.object.OAObject;
import com.viaoa.runtime.OARuntime;
import com.viaoa.select.OASelect;

class OAHubParentServiceTest {
	
//    private HubInternalService hubs;

    @BeforeEach
    void beforeEach() {
//        hubs = hubService();
        OA oa = OARuntime.oa(Register.class);
    }
    @AfterEach
    void afterEach() {
        OAObject.setDebugMode(false);
        OARuntime.oa(Register.class).close();
    }
/*qqqqq
    private static HubInternalService hubService() {
        OA graph = OARuntime.graph(Register.class);
        return (HubInternalService) graph.hubsInternal();
    }

    @Test
    void parentExposesCoordinatedHubChildServices() {
        assertNotNull(hubs.getHubAddRemoveService());
        assertNotNull(hubs.getHubAOService());
        assertNotNull(hubs.getHubDataService());
        assertNotNull(hubs.getHubDetailService());
        assertNotNull(hubs.getHubEventService());
        assertNotNull(hubs.getHubFindService());
        assertNotNull(hubs.getHubLinkService());
        assertNotNull(hubs.getHubPropertyService());
        assertNotNull(hubs.getHubRootService());
        assertNotNull(hubs.getHubSelectService());
        assertNotNull(hubs.getHubShareService());
        assertNotNull(hubs.getHubSizeService());
        assertNotNull(hubs.getHubSortService());
        assertNotNull(hubs.getHubStatusService());
    }

    @Test
    void addRemoveAoAndDataServicesOperateThroughParent() {
        Hub<Register> hub = new Hub<>(Register.class);
        Register r1 = new Register();
        r1.setCode("B");
        Register r2 = new Register();
        r2.setCode("A");

        assertTrue(hubs.callHubAddRemoveAdd(hub, r1));
        assertTrue(hubs.callHubAddRemoveInsert(hub, r2, 0));
        assertEquals(2, hubs.callHubSizeGetSize(hub));
        assertSame(r2, hubs.callHubDataGetObjectAt(hub, 0));
        assertTrue(hubs.callHubDataContains(hub, r1));
        assertEquals(1, hubs.callHubDataGetPos(hub, r1, false, false));

        assertSame(r1, hubs.callHubAOSetActiveObject(hub, 1));
        assertSame(r1, hub.getAO());

        hubs.callHubAddRemoveSwap(hub, 0, 1);
        assertSame(r1, hub.getAt(0));
        hubs.callHubAddRemoveMove(hub, 0, 1);
        assertSame(r1, hub.getAt(1));

        assertTrue(hubs.callHubAddRemoveRemove(hub, r2));
        assertEquals(1, hub.size());
        assertSame(r1, hubs.callHubAddRemoveRemove(hub, 0));
        assertTrue(hub.isEmpty());
    }

    @Test
    void eventFindPropertyRootSelectAndSortServicesOperateThroughParent() {
        Hub<Register> hub = new Hub<>(Register.class);
        Register r1 = new Register();
        r1.setCode("B");
        Register r2 = new Register();
        r2.setCode("A");

        AtomicInteger adds = new AtomicInteger();
        HubListenerAdapter<Register> listener = new HubListenerAdapter<Register>() {
            @Override
            public void afterAdd(HubEvent<Register> e) {
                adds.incrementAndGet();
            }
        };
        hubs.callHubEventAddHubListener(hub, listener);

        hubs.callHubAddRemoveAdd(hub, r1);
        hubs.callHubAddRemoveAdd(hub, r2);
        assertEquals(2, adds.get());

        assertSame(r2, hubs.callHubFindFindFirst(hub, Register.P_Code, "A", false, null));

        hubs.callHubPropertySetProperty(hub, "testProperty", "value");
        assertEquals("value", hubs.callHubPropertyGetProperty(hub, "testProperty"));
        hubs.callHubPropertyRemoveProperty(hub, "testProperty");
        assertNull(hubs.callHubPropertyGetProperty(hub, "testProperty"));

        hubs.callHubRootSetRootHub(hub, true);
        assertSame(hub, hubs.callHubRootGetRootHub(hub));

        hubs.callHubSelectSetSelectWhere(hub, "code = ?");
        hubs.callHubSelectSetSelectOrder(hub, Register.P_Code);
        assertEquals("code = ?", hubs.callHubSelectGetSelectWhere(hub));
        assertEquals(Register.P_Code, hubs.callHubSelectGetSelectOrder(hub));
        OASelect<Register> select = hubs.callHubSelectGetSelect(hub, true);
        assertNotNull(select);

        hubs.callHubSortSort(hub, Register.P_Code, true, null);
        assertTrue(hubs.callHubSortIsSorted(hub));
        assertSame(r2, hub.getAt(0));
        hubs.callHubSortCancelSort(hub);
        assertFalse(hubs.callHubSortIsSorted(hub));

        hubs.callHubEventRemoveHubListener(hub, listener);
    }

    @Test
    void detailShareLinkAndStatusServicesOperateThroughParent() {
        Store store = new Store();
        Register register = new Register();
        store.getRegisters().add(register);

        Hub<Store> stores = new Hub<>(Store.class);
        stores.add(store);
        stores.setAO(store);

        Hub<?> detail = hubs.callHubDetailGetDetailHub(stores, Store.P_Registers);
        assertNotNull(detail);
        assertEquals(Register.class, detail.getObjectClass());
        assertSame(register, detail.getAt(0));
        assertEquals(Store.P_Registers, hubs.callHubDetailGetPropertyFromMasterToDetail(detail));
        assertEquals(Register.P_Store, hubs.callHubDetailGetPropertyFromDetailToMaster(detail));

        Hub<Register> shared = hubs.callHubShareCreateSharedHub(store.getRegisters(), true);
        assertTrue(hubs.callHubShareIsUsingSameSharedHub(store.getRegisters(), shared));
        assertSame(store.getRegisters(), hubs.callHubShareGetMainSharedHub(shared));

        Hub<Register> linked = new Hub<>(Register.class);
        Hub<Store> linkTargets = new Hub<>(Store.class);
        linkTargets.add(store);
        hubs.callHubLinkSetLinkHub(linked, Register.P_Store, linkTargets, null, false, false, false);
        assertEquals(Register.P_Store, hubs.callHubLinkGetLinkToProperty(linked));

        assertTrue(hubs.callHubStatusIsValid(store.getRegisters()));
        hubs.callHubStatusSetChanged(store.getRegisters(), true);
        assertTrue(hubs.callHubStatusGetChanged(store.getRegisters(), 0, null));
        assertEquals(HubCurrentStateEnum.InSync,
                hubs.callHubStatusGetCurrentState(store.getRegisters(), store.getRegisters(), null));
    }
*/    
}
