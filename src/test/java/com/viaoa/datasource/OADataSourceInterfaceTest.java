package com.viaoa.datasource;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Iterator;

import org.junit.jupiter.api.Test;

import com.test.pos.model.oa.Store;
import com.viaoa.filter.OAFilter;
import com.viaoa.metadata.OAObjectInfo;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectKey;

class OADataSourceInterfaceTest {

    @Test
    void defaultMaintenanceMethodsAreNoOpsAndIsClientIsFalse() throws Exception {
        OADataSourceInterface ds = new RecordingDataSource();

        assertDoesNotThrow(() -> ds.checkForCorruption());
        assertDoesNotThrow(() -> ds.backup("memory"));
        assertDoesNotThrow(() -> ds.restore("memory"));
        assertDoesNotThrow(() -> ds.compress());
        assertFalse(ds.isClient());
    }

    @Test
    void interfaceCanBeImplementedForCoreCrudAndQueryMethods() {
        RecordingDataSource ds = new RecordingDataSource();
        Store store = new Store(1);

        assertTrue(ds.isClassSupported(Store.class));
        assertTrue(ds.isClassSupported(Store.class, obj -> true));
        assertTrue(ds.supportsStorage());
        assertTrue(ds.isAvailable());
        assertTrue(ds.getEnabled());
        ds.setEnabled(false);
        assertFalse(ds.getEnabled());
        assertTrue(ds.getAllowIdChange());
        ds.setAssignIdOnCreate(true);
        assertTrue(ds.getAssignIdOnCreate());
        ds.assignId(store);
        assertEquals(1, ds.assignCalls);
        assertTrue(ds.getSupportsPreCount());
        ds.close();
        ds.reopen(2);
        assertFalse(ds.willCreatePropertyValue(store, Store.P_Id));
        ds.save(store);
        ds.update(store, new String[] { Store.P_Name }, null);
        ds.update(store);
        ds.insert(store);
        ds.insertWithoutReferences(store);
        ds.delete(store);
        ds.deleteAll(Store.class);
        ds.updateMany2ManyLinks(store, new OAObject[0], new OAObject[0], Store.P_Registers);
        assertNotNull(ds.select(Store.class, null, null, null, null, null, null, 0, null, false));
        assertNotNull(ds.selectPassthru(Store.class, null, null, 0, null, false));
        assertEquals("ok", ds.execute("ping"));
        assertEquals(7, ds.count(Store.class, null, null, null, null, null, 0));
        assertEquals(8, ds.countPassthru(Store.class, null, 0));
        assertNull(ds.getObject(null, Store.class, new OAObjectKey(1), false));
        assertNull(ds.getPropertyBlobValue(store, Store.P_Name));
        assertEquals(50, ds.getMaxLength(Store.class, Store.P_Name));
    }

    private static class RecordingDataSource implements OADataSourceInterface {
        boolean enabled = true;
        boolean assignIdOnCreate;
        int assignCalls;

        @Override
        public boolean isClassSupported(Class<?> clazz) {
            return clazz == Store.class;
        }

        @Override
        public <T> boolean isClassSupported(Class<?> clazz, OAFilter<T> filter) {
            return clazz == Store.class;
        }

        @Override
        public boolean supportsStorage() {
            return true;
        }

        @Override
        public boolean isAvailable() {
            return true;
        }

        @Override
        public boolean getEnabled() {
            return enabled;
        }

        @Override
        public void setEnabled(boolean b) {
            enabled = b;
        }

        @Override
        public boolean getAllowIdChange() {
            return true;
        }

        @Override
        public void setAssignIdOnCreate(boolean b) {
            assignIdOnCreate = b;
        }

        @Override
        public boolean getAssignIdOnCreate() {
            return assignIdOnCreate;
        }

        @Override
        public void assignId(OAObject object) {
            assignCalls++;
        }

        @Override
        public boolean getSupportsPreCount() {
            return true;
        }

        @Override
        public void close() {
        }

        @Override
        public void reopen(int pos) {
        }

        @Override
        public boolean willCreatePropertyValue(OAObject object, String propertyName) {
            return false;
        }

        @Override
        public void save(OAObject obj) {
        }

        @Override
        public void update(OAObject object, String[] includeProperties, String[] excludeProperties) {
        }

        @Override
        public void update(OAObject obj) {
        }

        @Override
        public void insert(OAObject object) {
        }

        @Override
        public void insertWithoutReferences(OAObject obj) {
        }

        @Override
        public void delete(OAObject object) {
        }

        @Override
        public void deleteAll(Class c) {
        }

        @Override
        public void updateMany2ManyLinks(OAObject masterObject, OAObject[] adds, OAObject[] removes, String propFromMaster) {
        }

        @Override
        public <T> Iterator<T> select(Class<T> selectClass, String queryWhere, Object[] params, String queryOrder,
                OAObject whereObject, String propertyFromWhereObject, String extraWhere, int max, OAFilter<T> filter, boolean bDirty) {
            return new OADataSourceEmptyIterator();
        }

        @Override
        public <T> Iterator<T> selectPassthru(Class<T> selectClass, String queryWhere, String queryOrder, int max, OAFilter<T> filter,
                boolean bDirty) {
            return new OADataSourceEmptyIterator();
        }

        @Override
        public Object execute(String command) {
            return "ok";
        }

        @Override
        public int count(Class<?> selectClass, String queryWhere, Object[] params, OAObject whereObject, String propertyFromWhereObject,
                String extraWhere, int max) {
            return 7;
        }

        @Override
        public int countPassthru(Class<?> selectClass, String queryWhere, int max) {
            return 8;
        }

        @Override
        public <T> T getObject(OAObjectInfo oi, Class<T> clazz, OAObjectKey key, boolean bDirty) {
            return null;
        }

        @Override
        public byte[] getPropertyBlobValue(OAObject obj, String propertyName) {
            return null;
        }

        @Override
        public int getMaxLength(Class<?> c, String propertyName) {
            return 50;
        }
    }
}
