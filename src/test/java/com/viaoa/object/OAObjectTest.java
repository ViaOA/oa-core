package com.viaoa.object;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.test.pos.model.oa.Customer;
import com.test.pos.model.oa.Register;
import com.test.pos.model.oa.Store;
import com.viaoa.callback.OAObjectCallback;
import com.viaoa.graph.OAGraph;
import com.viaoa.hub.Hub;
import com.viaoa.lang.oa.VEnum;
import com.viaoa.runtime.OARuntime;

class OAObjectTest {

    @BeforeEach
    void beforeEach() {
        OAGraph og = OARuntime.graph(Register.class);
    }
    @AfterEach
    void afterEach() {
        OAObject.setDebugMode(false);
        OARuntime.graph(Register.class).close();
    }
	
    @Test
    void getOAVersionReturnsVersionString() {
        assertTrue(OAObject.getOAVersion().startsWith("4.0.0"));
    }

    @Test
    void constructorInitializesGuidLifecycleFlagsAndCacheIdentity() {
        Store store = new Store();

        assertNotNull(store.getGuid());
        assertNotNull(store.getObjectKey());
        assertTrue(store.getNew());
        assertTrue(store.isNew());
        assertTrue(store.getChanged());
        assertTrue(store.isChanged());
        assertFalse(store.getDeleted());
        assertFalse(store.wasDeleted());
        assertFalse(store.isDeleted());
    }

    @Test
    void setPropertyOverloadsAndGetPropertyUseModelMetadata() {
        Store store = new Store();

        store.setProperty(Store.P_StoreNumber, 55);
        store.setProperty(Store.P_Name, "Main");
        store.setProperty(Store.P_Id, 7L);
        store.setProperty(Store.P_StoreNumber, 12.0d);
        store.setProperty(Store.P_Name, "Formatted", null);

        assertEquals(12, store.getProperty(Store.P_StoreNumber));
        assertEquals("Formatted", store.getProperty(Store.P_Name));
    }

    @Test
    void setNullRemovePropertyAndNullChecksClearProperties() {
        Store store = new Store();
        store.setName("North");

        store.setNull(Store.P_Name);
        assertNull(store.getName());
        assertTrue(store.isNull(Store.P_Name));
        assertTrue(store.getNull(Store.P_Name));

        store.setName("South");
        store.removeProperty(Store.P_Name);
        assertNull(store.getName());
    }

    @Test
    void getPropertyAsStringOverloadsFormatAndNullValues() {
        Store store = new Store();
        store.setStoreNumber(123);

        assertEquals("123", store.getPropertyAsString(Store.P_StoreNumber));
        assertEquals("123", store.getPropertyAsString(Store.P_StoreNumber, true));
        assertEquals("123", store.getPropertyAsString(Store.P_StoreNumber, ""));
        assertEquals("missing", store.getPropertyAsString(Store.P_Name, null, "missing"));
    }

    @Test
    void validationEnabledVisibleAndCommandCallbacksAllowNormalModelProperties() {
        Store store = new Store();

        assertTrue(store.isValidPropertyChange(Store.P_Name, null, "New"));
        assertTrue(store.isValidPropertyChange(Store.P_Name, "New"));
        assertNotNull(store.getIsValidPropertyChangeObjectCallback(Store.P_Name, null, "New"));
        assertNotNull(store.getIsValidPropertyChangeObjectCallback(Store.P_Name, "New"));
        assertTrue(store.isEnabled(Store.P_Name));
        assertNotNull(store.getIsEnabledObjectCallback(Store.P_Name, null, "New"));
        assertTrue(store.isEnabled());
        assertNotNull(store.getIsEnabledObjectCallback());
        assertTrue(store.isVisible(Store.P_Name));
        assertNotNull(store.getIsVisibleObjectCallback(Store.P_Name));
        assertTrue(store.isVisible());
        assertNotNull(store.getIsVisibleObjectCallback());
        assertTrue(store.verifyCommand("callback"));
        assertNotNull(store.getVerifyCommand("callback"));
        assertNotNull(store.getAllowSubmit());
        assertNotNull(store.getVerifySaveObjectCallback());
    }

    @Test
    void lifecycleFlagsCanBeUpdated() {
        Store store = new Store();

        store.setNew(false);
        assertFalse(store.getNew());
        assertFalse(store.isNew());

        store.setDeleted(true);
        assertTrue(store.getDeleted());
        assertTrue(store.wasDeleted());
        assertTrue(store.isDeleted());

        store.setDeleted(false);
        assertFalse(store.isDeleted());
    }

    @Test
    void equalsHashCodeAndCompareToUseGuidIdentity() {
        Store one = new Store(1);
        Store two = new Store(1);
        OAObjectInternalBridge bridge = new OAObjectInternalBridge();
        UUID guid = one.getGuid();
        bridge.getObjectFriendAccess().setGuid(two, null);
        bridge.getObjectFriendAccess().setGuid(two, guid);

        assertEquals(one, two);
        assertEquals(one.hashCode(), two.hashCode());
        assertEquals(0, one.compareTo(two));
        assertTrue(one.compareTo(null) > 0);
        assertNotEquals(one, "x");
        assertNotEquals(one, new Store(2));
    }

    @Test
    void changedFlagsCanBeClearedAndSet() {
        Store store = new Store();

        store.setChanged(false);
        assertFalse(store.getChanged());
        assertFalse(store.isChanged());
        assertFalse(store.getChanged(false));
        assertFalse(store.isChanged(false));
        assertFalse(store.getChanged(OAObject.CASCADE_NONE));

        store.setName("Changed");
        assertTrue(store.getChanged());
    }

    @Test
    void createCopyCopyWithExclusionsAndCopyIntoUseModelProperties() {
        Store store = new Store(1);
        store.setName("Original");
        store.setStoreNumber(101);

        Store copy = (Store) store.createCopy();
        assertNotSame(store, copy);
        assertEquals("Original", copy.getName());
        assertEquals(101, copy.getStoreNumber());

        Store excluded = (Store) store.createCopy(new String[] { Store.P_Name });
        assertNull(excluded.getName());

        Store target = new Store();
        store.copyInto(target);
        assertEquals("Original", target.getName());
    }

    @Test
    void finalizeSaveFlagCurrentlyAlwaysFalse() {
        OAObject.setFinalizeSave(true);

        assertFalse(OAObject.getFinalizeSave());
    }

    @Test
    void generatedSettersUpdatePropertiesAndChangedFlag() {
        Store store = new Store();
        store.setChanged(false);

        store.setName("Central");

        assertEquals("Central", store.getName());
        assertTrue(store.getChanged());
    }

    @Test
    void setHubAndGeneratedHubAccessMaintainOneToManyRelationship() {
        Store store = new Store(1);
        Hub<Register> hub = new Hub<>(Register.class);
        Register register = new Register(2);

        store.setHub(Store.P_Registers, hub);
        store.getRegisters().add(register);

        assertSame(hub, store.getRegisters());
        assertSame(store, register.getStore());
        assertFalse(store.isReferenceObjectNull(Store.P_Registers));
    }

    @Test
    void saveDeleteAndLockMethodsExposeSafeInMemoryBehavior() {
        Store store = new Store();

        assertTrue(store.canSave());
        assertTrue(store.canDelete());
        store.afterSave();
        store.afterDelete();
        store.lock();
        assertTrue(store.isLocked());
        store.unlock();
        assertFalse(store.isLocked());
    }

    @Test
    void findAndFindAllTraverseRealHubRelationships() {
        Store store = new Store(1);
        Register one = new Register(2);
        one.setCode("R1");
        Register two = new Register(3);
        two.setCode("R2");
        store.getRegisters().add(one);
        store.getRegisters().add(two);

        assertSame(two, store.find(Store.P_Registers + "." + Register.P_Code, "R2"));
        Object[] found = store.findAll(Store.P_Registers + "." + Register.P_Code, "R1");
        assertEquals(1, found.length);
        assertSame(one, found[0]);
    }

    @Test
    void threadAndMessageMethodsAreStableInSingleUserTests() {
        Store store = new Store();

        assertFalse(store.isRemoteThread());
        assertDoesNotThrow(() -> store.sendMessages(false));
        assertDoesNotThrow(() -> store.sendMessages(true));
        store.afterLoad();
    }

    @Test
    void objectKeyGuidAutoAddEmptyAndHubLoadedAccessorsWork() {
        Store store = new Store(1);

        assertNotNull(store.getObjectKey());
        assertNotNull(store.getGuid());
        store.setAutoAdd(false);
        assertFalse(store.getAutoAdd());
        store.setAutoAdd(true);
        assertTrue(store.getAutoAdd());
        assertTrue(store.isEmpty(""));
        assertFalse(store.isEmpty("x"));
        assertTrue(store.isHubLoaded(Store.P_Registers));
    }

    @Test
    void loadReferencesOverloadsAreNoOpsForAlreadyInMemoryGraph() {
        Store store = new Store(1);
        store.getRegisters().add(new Register(2));

        assertDoesNotThrow(() -> store.loadReferences(false));
        assertDoesNotThrow(() -> store.loadReferences(true, true, false));
        assertDoesNotThrow(() -> store.loadReferences(1, 1, false));
        assertDoesNotThrow(() -> store.loadReferences(1, 1, false, 10));
    }

    @Test
    void remoteHelpersReportUnavailableWithoutSyncClient() {
        Store store = new Store();
        Hub<Store> hub = new Hub<>(Store.class);

        assertNull(OAObject.callRemote(null, "x"));
        assertFalse(store.isRemoteAvailable());
        assertFalse(OAObject.isRemoteAvailable(null));
        assertFalse(OAObject.isRemoteAvailable(hub));
        assertThrows(RuntimeException.class, () -> store.remote("x"));
    }

    @Test
    void uniqueInstanceLookupCanReturnNullOrAutoCreate() {
        assertNull(OAObject.getUniqueInstance(Store.class, Store.P_StoreNumber, 999, false));

        OAObject obj = OAObject.getUniqueInstance(Store.class, Store.P_StoreNumber, 999, true);

        assertTrue(obj instanceof Store);
        assertEquals(999, ((Store) obj).getStoreNumber());
    }

    @Test
    void loadedReferenceReferenceKeyAndHierarchyHelpersUseCurrentGraphState() {
        Store store = new Store(1);
        Register register = new Register(2);
        store.getRegisters().add(register);

        assertTrue(store.isLoaded(Store.P_Name));
        assertTrue(store.isPropertyLoaded(Store.P_Name));
        assertFalse(register.isReferenceNull(Register.P_Store));
        assertEquals(store.getObjectKey(), register.getReferenceObjectKey(Register.P_Store));
        assertEquals("Fallback", store.hierFind(Store.P_Name, Store.P_Address));
    }

    @Test
    void serverOnlyDebugSubmittedAndPropertyLockMethodsHaveDeterministicDefaults() {
        Store store = new Store();
        AtomicInteger calls = new AtomicInteger();

        assertFalse(store.startServerOnly());
        assertDoesNotThrow(store::endServerOnly);
        store.runOnServerOnly(calls::incrementAndGet);
        assertEquals(0, calls.get());

        OAObject.setDebugMode(true);
        assertTrue(OAObject.getDebugMode());
        assertFalse(store.isPropertyLocked(Store.P_Name));
        assertTrue(store.isSubmitted());
        assertTrue(store._isSubmitted(11));
    }

    @Test
    void compareAndSwapUpdatesOnlyWhenExpectedValueMatches() {
        Store store = new Store();
        store.setName("Old");

        assertTrue(store.compareAndSwap(Store.P_Name, "Old", "New", false));
        assertEquals("New", store.getName());
        assertFalse(store.compareAndSwap(Store.P_Name, "Old", "Other", false));
        assertEquals("New", store.getName());
        assertFalse(store.compareAndSwap(null, "New", "Other", false));
        assertFalse(store.compareAndSwap("", "New", "Other", false));
    }

    @Test
    void setObjectDefaultsCanBeCalledBySubclassesAndGeneratedClasses() {
        Store store = new Store();

        assertDoesNotThrow(store::setObjectDefaults);
        assertNotNull(store.getCreated());
    }

    @Test
    void foreignKeyHelpersUseRealOneToOneRelationship() {
        Store store = new Store(44);
        Register register = new Register(2);
        register.setStore(store);

        assertEquals(44, register.getFkeyProperty(Register.P_StoreId));
        assertEquals(44, register.getFkeyProperty(Register.P_Store, Store.P_Id));
        assertTrue(register.setFkeyProperty(Register.P_StoreId, 55));
        assertEquals(55, register.getFkeyProperty(Register.P_StoreId));
        assertFalse(register.setFkeyProperty(null, 1));
        assertNull(register.getFkeyProperty("missing"));
        assertNull(register.getFkeyProperty("", Store.P_Id));
        assertThrows(RuntimeException.class, () -> register.getFkeyProperty(Register.P_Store, "missing"));
    }

    @Test
    void refreshMethodsReturnForNewOrDatasourceMissingObjects() {
        Store store = new Store(1);

        assertDoesNotThrow(() -> store.refresh());
        assertDoesNotThrow(() -> store.refresh(Store.P_Registers));
        assertDoesNotThrow(() -> store.refresh("missing"));
    }

    @Test
    void getNameValuesReturnsEnumValuesWhenModelDefinesThem() {
        Customer customer = new Customer();

        Hub<VEnum> values = customer.getNameValues(Customer.P_Type);

        assertNotNull(values);
        assertTrue(values.getSize() > 0);
    }

    @Test
    void friendAccessCanReadAndWriteInternalFlags() {
        Store store = new Store();
        OAObject.FriendAccess fa = new OAObjectInternalBridge().getObjectFriendAccess();
        UUID guid = UUID.randomUUID();

        fa.setGuid(store, null);
        fa.setGuid(store, guid);
        fa.setNew(store, false);
        fa.setDeletedFlag(store, true);
        fa.setChangedFlag(store, false);
        fa.setNulls(store, new byte[] { 1 });
        fa.setProperties(store, new Object[] { "x" });

        assertEquals(guid, fa.getGuid(store));
        assertFalse(fa.isNew(store));
        assertFalse(fa.getNewFlag(store));
        assertTrue(fa.getDeleteFlag(store));
        assertFalse(fa.getChangedFlag(store));
        assertArrayEquals(new byte[] { 1 }, fa.getNulls(store));
        assertArrayEquals(new Object[] { "x" }, fa.getProperties(store));
    }

    @Test
    void getGraphReturnsRuntimeGraphForObject() {
        assertSame(OARuntime.graph(Store.class), new Store().getGraph());
    }

}
