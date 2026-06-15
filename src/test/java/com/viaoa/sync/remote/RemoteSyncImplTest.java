package com.viaoa.sync.remote;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;

import com.test.pos.model.oa.Register;
import com.test.pos.model.oa.Store;
import com.viaoa.graph.api.internal.OAGraphInternal;
import com.viaoa.graph.service.OAObjectInternalService;
import com.viaoa.runtime.OARuntime;

class RemoteSyncImplTest {

    @BeforeEach
    void beforeEach() {
        clearCache();
    }

    @AfterEach
    void afterEach() {
        clearCache();
    }

    @Test
    void propertyChangeUpdatesCachedObjectProperty() {
        Store store = new Store(1);
        RemoteSyncImpl sync = new RemoteSyncImpl();

        boolean result = sync.propertyChange(Store.class, store.getObjectKey(), Store.P_Name, "Main", false);

        assertTrue(result);
        assertEquals("Main", store.getName());
    }

    @Test
    void propertyChangeReturnsFalseForMissingObject() {
        RemoteSyncImpl sync = new RemoteSyncImpl();

        assertFalse(sync.propertyChange(Store.class, new com.viaoa.object.OAObjectKey(new Object[] { 999 }), Store.P_Name, "Main", false));
    }

    @Test
    void addToHubAddsObjectToLoadedHub() {
        Store store = new Store(2);
        store.getRegisters();
        Register register = new Register(10);
        RemoteSyncImpl sync = new RemoteSyncImpl();

        assertTrue(sync.addToHub(Store.class, store.getObjectKey(), Store.P_Registers, register));

        assertEquals(1, store.getRegisters().size());
        assertSame(register, store.getRegisters().get(0));
    }

    @Test
    void insertInHubInsertsObjectAtRequestedPosition() {
        Store store = new Store(3);
        Register first = new Register(11);
        Register second = new Register(12);
        store.getRegisters().add(first);
        RemoteSyncImpl sync = new RemoteSyncImpl();

        assertTrue(sync.insertInHub(Store.class, store.getObjectKey(), Store.P_Registers, second, 0));

        assertSame(second, store.getRegisters().get(0));
        assertSame(first, store.getRegisters().get(1));
    }

    @Test
    void removeFromHubRemovesMatchingObject() {
        Store store = new Store(4);
        Register register = new Register(13);
        store.getRegisters().add(register);
        RemoteSyncImpl sync = new RemoteSyncImpl();

        assertTrue(sync.removeFromHub(Store.class, store.getObjectKey(), Store.P_Registers, Register.class, register.getObjectKey()));

        assertEquals(0, store.getRegisters().size());
    }

    @Test
    void removeAllFromHubClearsLoadedHub() {
        Store store = new Store(5);
        store.getRegisters().add(new Register(14));
        store.getRegisters().add(new Register(15));
        RemoteSyncImpl sync = new RemoteSyncImpl();

        assertTrue(sync.removeAllFromHub(Store.class, store.getObjectKey(), Store.P_Registers));

        assertEquals(0, store.getRegisters().size());
    }

    @Test
    void moveObjectInHubMovesBetweenPositions() {
        Store store = new Store(6);
        Register first = new Register(16);
        Register second = new Register(17);
        store.getRegisters().add(first);
        store.getRegisters().add(second);
        RemoteSyncImpl sync = new RemoteSyncImpl();

        assertTrue(sync.moveObjectInHub(Store.class, store.getObjectKey(), Store.P_Registers, 0, 1));

        assertSame(second, store.getRegisters().get(0));
        assertSame(first, store.getRegisters().get(1));
    }

    @Test
    void sortOrdersLoadedHubByPropertyPath() {
        Store store = new Store(7);
        Register b = new Register(18);
        b.setCode("B");
        Register a = new Register(19);
        a.setCode("A");
        store.getRegisters().add(b);
        store.getRegisters().add(a);
        RemoteSyncImpl sync = new RemoteSyncImpl();

        assertTrue(sync.sort(Store.class, store.getObjectKey(), Store.P_Registers, Register.P_Code, true, null));

        assertSame(a, store.getRegisters().get(0));
        assertSame(b, store.getRegisters().get(1));
    }

    @Test
    void clearHubChangesIgnoresMissingObjectsOrUnloadedHubs() {
        RemoteSyncImpl sync = new RemoteSyncImpl();
        Store store = new Store(8);

        assertDoesNotThrow(() -> sync.clearHubChanges(Store.class, store.getObjectKey(), Store.P_Registers));
        assertDoesNotThrow(() -> sync.clearHubChanges(Store.class, new com.viaoa.object.OAObjectKey(new Object[] { 999 }), Store.P_Registers));
    }

    @Test
    void serverDeleteAndClientDeleteNoOpWhenObjectMissingOrWrongSyncSide() {
        RemoteSyncImpl sync = new RemoteSyncImpl();
        Store store = new Store(9);

        assertDoesNotThrow(() -> sync.serverDelete(Store.class, store.getObjectKey()));
        assertDoesNotThrow(() -> sync.clientDelete(Store.class, store.getObjectKey()));
        assertDoesNotThrow(() -> sync.serverDelete(Store.class, new com.viaoa.object.OAObjectKey(new Object[] { 999 })));
    }

    private void clearCache() {
        OAGraphInternal og = (OAGraphInternal) OARuntime.graph(Register.class);
        OAObjectInternalService os = (OAObjectInternalService) og.objectsInternal();
        os.getOAObjectCacheService().removeAllObjects();
    }
}
