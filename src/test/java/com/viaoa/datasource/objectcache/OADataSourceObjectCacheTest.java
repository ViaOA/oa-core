package com.viaoa.datasource.objectcache;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.test.pos.model.oa.Register;
import com.test.pos.model.oa.Store;
import com.viaoa.datasource.OADataSourceIterator;
import com.viaoa.graph.OAGraph;
import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;
import com.viaoa.runtime.OARuntime;

class OADataSourceObjectCacheTest {

    @TempDir
    File tempDir;

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
    void constructorsConfigureLastFlagAndOptionalNextNumberHub() {
        Hub hub = new Hub();

        assertTrue(new OADataSourceObjectCache().getLast());
        assertFalse(new OADataSourceObjectCache(false).getLast());
        assertSame(hub, new OADataSourceObjectCache(hub, false).getNextNumbers());
    }

    @Test
    void selectReturnsInsertedObjectsFromOAObjectCache() {
        OADataSourceObjectCache ds = new OADataSourceObjectCache(false);
        Store one = new Store(1);
        one.setStoreNumber(200);
        Store two = new Store(2);
        two.setStoreNumber(100);
        ds.insert(one);
        ds.insert(two);

        OADataSourceIterator<Store> it = ds.select(Store.class, null, null, Store.P_StoreNumber, null, null, null, 0, null, false);

        assertSame(two, it.next());
        assertSame(one, it.next());
        assertFalse(it.hasNext());
    }

    @Test
    void selectAppliesQueryFilterAndMax() {
        OADataSourceObjectCache ds = new OADataSourceObjectCache(false);
        Store one = new Store(1);
        one.setName("East");
        Store two = new Store(2);
        two.setName("West");
        ds.insert(one);
        ds.insert(two);

        OADataSourceIterator<Store> it = ds.select(Store.class, Store.P_Name + " == ?", new Object[] { "West" }, null, null, null, null, 1,
                null, false);

        assertTrue(it.hasNext());
        assertSame(two, it.next());
        assertFalse(it.hasNext());
    }

    @Test
    void selectFromWhereObjectHubUsesRealOAPOSRelationship() {
        OADataSourceObjectCache ds = new OADataSourceObjectCache(false);
        Store store = new Store(1);
        Register register = new Register(2);
        register.setCode("R1");
        store.getRegisters().add(register);
        ds.insert(store);
        ds.insert(register);

        OADataSourceIterator<Register> it = ds.select(Register.class, null, null, null, store, Store.P_Registers, null, 0, null, false);

        assertTrue(it.hasNext());
        assertSame(register, it.next());
        assertFalse(it.hasNext());
    }

    @Test
    void selectPassthruUsesSameInMemorySelectionAsSelect() {
        OADataSourceObjectCache ds = new OADataSourceObjectCache(false);
        Store store = new Store(1);
        store.setName("Central");
        ds.insert(store);

        OADataSourceIterator<Store> it = ds.selectPassthru(Store.class, Store.P_Name + " == 'Central'", null, 0, null, false);

        assertTrue(it.hasNext());
        assertSame(store, it.next());
    }

    @Test
    void assignIdDelegatesToAutonumberSupport() {
        OADataSourceObjectCache ds = new OADataSourceObjectCache(new Hub(), false);
        ds.setStartingNextNumber(300);
        Register register = new Register();

        ds.assignId(register);

        assertEquals(300, register.getId());
    }

    @Test
    void getSupportsPreCountReturnsFalse() {
        assertFalse(new OADataSourceObjectCache(false).getSupportsPreCount());
    }

    @Test
    void insertAndInsertWithoutReferencesRegisterClassForStorage() throws Exception {
        OADataSourceObjectCache ds = new OADataSourceObjectCache(false);
        Store one = new Store(1);
        Store two = new Store(2);
        ds.insert(one);
        ds.insertWithoutReferences(two);
        File file = new File(tempDir, "cache.bin");

        ds.saveToStorageFile(file, "extra");

        assertTrue(file.exists());
        assertTrue(file.length() > 0);
    }

    @Test
    void saveToStorageFileIgnoresNullFileAndLoadFromStorageFileHandlesMissingFile() throws Exception {
        OADataSourceObjectCache ds = new OADataSourceObjectCache(false);

        assertDoesNotThrow(() -> ds.saveToStorageFile(null, null));
        assertFalse(ds.loadFromStorageFile(null));
        assertFalse(ds.loadFromStorageFile(new File(tempDir, "missing.bin")));
    }

    @Test
    void loadFromStorageFileRoundTripsStoredObjects() throws Exception {
        OADataSourceObjectCache ds = new OADataSourceObjectCache(false);
        Store store = new Store(7);
        store.setName("RoundTrip");
        ds.insert(store);
        File file = new File(tempDir, "cache.bin");
        ds.saveToStorageFile(file, null);
        // clearCache();
        OADataSourceObjectCache loaded = new OADataSourceObjectCache(false);

        assertTrue(loaded.loadFromStorageFile(file));
        OADataSourceIterator<Store> it = loaded.select(Store.class, Store.P_Name + " == ?", new Object[] { "RoundTrip" }, null, null, null,
                null, 0, null, false);

        assertTrue(it.hasNext());
        assertEquals("RoundTrip", it.next().getName());
    }
}
