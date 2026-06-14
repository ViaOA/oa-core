package com.viaoa.datasource;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.test.pos.model.oa.Store;
import com.viaoa.filter.OAFilter;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectKey;

class OADataSourceTest {

    @Test
    void setGuidAndGetGuidStoreValue() {
        TestDataSource ds = new TestDataSource();

        ds.setGuid("guid-1");

        assertEquals("guid-1", ds.getGuid());
    }

    @Test
    void enabledDefaultsTrueAndCanBeChanged() {
        TestDataSource ds = new TestDataSource();

        assertTrue(ds.getEnabled());
        ds.setEnabled(false);
        assertFalse(ds.getEnabled());
    }

    @Test
    void getObjectConvenienceOverloadsDelegateToObjectKeyLookup() {
        TestDataSource ds = new TestDataSource();
        Store store = new Store(12);
        ds.nextSelect = new OADataSourceListIterator(java.util.List.of(store));

        assertSame(store, ds.getObject(Store.class, 12));
        assertEquals(Store.class, ds.lastSelectClass);
        assertEquals(Store.P_Id + " == ?", ds.lastQueryWhere);
        assertArrayEquals(new Object[] { 12 }, ds.lastParams);
    }

    @Test
    void getObjectReturnsNullForNullKey() {
        TestDataSource ds = new TestDataSource();

        assertNull(ds.getObject(Store.class, (OAObjectKey) null));
        assertEquals(0, ds.selectCalls);
    }

    @Test
    void setAssignIdOnCreateAndGetAssignIdOnCreateStoreValue() {
        TestDataSource ds = new TestDataSource();

        ds.setAssignIdOnCreate(true);

        assertTrue(ds.getAssignIdOnCreate());
    }

    @Test
    void isAvailableDefaultsTrueAndGetInfoIsNoOp() {
        TestDataSource ds = new TestDataSource();

        assertTrue(ds.isAvailable());
        assertDoesNotThrow(() -> ds.getInfo(new java.util.ArrayList<>()));
    }

    @Test
    void getMaxLengthDefaultsToMinusOne() {
        assertEquals(-1, new TestDataSource().getMaxLength(Store.class, Store.P_Name));
    }

    @Test
    void lastAndNamePropertiesRoundTrip() {
        TestDataSource ds = new TestDataSource();

        ds.setLast(true);
        ds.setName("primary");

        assertTrue(ds.getLast());
        assertEquals("primary", ds.getName());
    }

    @Test
    void updateObjectDelegatesToUpdateWithNoIncludeOrExcludeProperties() {
        TestDataSource ds = new TestDataSource();
        Store store = new Store(1);

        ds.update(store);

        assertSame(store, ds.updated);
        assertNull(ds.lastIncludeProperties);
        assertNull(ds.lastExcludeProperties);
    }

    @Test
    void deleteAllDefaultsToNoOp() {
        assertDoesNotThrow(() -> new TestDataSource().deleteAll(Store.class));
    }

    @Test
    void saveIgnoresNullObject() {
        TestDataSource ds = new TestDataSource();

        ds.save(null);

        assertEquals(0, ds.insertCalls);
        assertEquals(0, ds.updateCalls);
    }

    @Test
    void saveInsertsNewObjectsAndUpdatesExistingObjects() {
        TestDataSource ds = new TestDataSource();
        Store newStore = new Store(1);
        Store existingStore = new Store(2);
        existingStore.setNew(false);

        ds.save(newStore);
        ds.save(existingStore);

        assertSame(newStore, ds.inserted);
        assertSame(existingStore, ds.updated);
    }

    @Test
    void countConvenienceMethodsDelegateAndCapMax() {
        TestDataSource ds = new TestDataSource();
        ds.countResult = 10;

        assertEquals(10, ds.count(Store.class, "id > 0"));
        assertEquals(3, ds.count(Store.class, "id > 0", 3));
        assertEquals(4, ds.count(Store.class, "id == ?", new Object[] { 1 }, 4));
        assertEquals(2, ds.count(Store.class, "id == ?", 1, 2));
        assertEquals(10, ds.count(Store.class, "id == ?", new Object[] { 1 }));
        assertEquals(10, ds.count(Store.class, "id == ?", Integer.valueOf(1)));
        assertEquals(5, ds.count(Store.class, new Store(1), Store.P_Registers, 5));
        assertEquals(10, ds.count(Store.class, new Store(1), Store.P_Registers));
        assertEquals(6, ds.count(Store.class, new Store(1), Store.P_Registers, new Object[0], Store.P_Registers, 6));
        assertEquals(10, ds.count(Store.class, new Store(1), Store.P_Registers, new Object[0], Store.P_Registers));
    }

    @Test
    void countPassthruConvenienceMethodsDelegateAndCapMax() {
        TestDataSource ds = new TestDataSource();
        ds.countPassthruResult = 9;

        assertEquals(4, ds.countPassthru("select", 4));
        assertEquals(9, ds.countPassthru("select"));
    }

    @Test
    void selectConvenienceMethodsDelegateToPrimarySelect() {
        TestDataSource ds = new TestDataSource();
        OAFilter<Store> filter = obj -> true;

        ds.select(Store.class);
        ds.select(Store.class, "id > 0");
        ds.select(Store.class, "id > 0", Store.P_Name);
        ds.select(Store.class, "id > 0", Store.P_Name, 3, filter, true);
        ds.select(Store.class, "id > 0", Store.P_Name, 3, true);
        ds.select(Store.class, "id > 0", Store.P_Name, true);
        ds.select(Store.class, "id == ?", new Object[] { 1 }, Store.P_Name, 3, true);
        ds.select(Store.class, "id == ?", new Object[] { 1 }, Store.P_Name, true);
        ds.select(Store.class, "id == ?", new Object[] { 1 }, Store.P_Name, 3, filter, true);
        ds.select(Store.class, "id == ?", 1, Store.P_Name, 3, filter, true);
        ds.select(Store.class, "id == ?", 1, Store.P_Name, 3, true);
        ds.select(Store.class, "id == ?", 1, Store.P_Name, true);
        ds.select(Store.class, new Store(1), Store.P_Registers, "id > 0", new Object[0], Store.P_Name, 3, filter, true);
        ds.select(Store.class, new Store(1), Store.P_Registers, "id > 0", new Object[0], Store.P_Name, 3, true);
        ds.select(Store.class, new Store(1), Store.P_Registers, "id > 0", new Object[0], Store.P_Name, true);
        ds.select(Store.class, new Store(1), Store.P_Registers, Store.P_Name, 3, filter, true);
        ds.select(Store.class, new Store(1), Store.P_Registers, Store.P_Name, 3, true);
        ds.select(Store.class, new Store(1), Store.P_Registers, Store.P_Name, true);

        assertEquals(18, ds.selectCalls);
        assertEquals(Store.class, ds.lastSelectClass);
    }

    @Test
    void selectPassthruConvenienceMethodsDelegateToPrimaryPassthruSelect() {
        TestDataSource ds = new TestDataSource();
        OAFilter<Store> filter = obj -> true;

        ds.selectPassthru(Store.class, "select", 3, filter, true);
        ds.selectPassthru(Store.class, "select", 3, true);
        ds.selectPassthru(Store.class, "select", true);
        ds.selectPassthru(Store.class, "select", Store.P_Name, 3, true);
        ds.selectPassthru(Store.class, "select", Store.P_Name, true);

        assertEquals(5, ds.selectPassthruCalls);
        assertEquals(Store.class, ds.lastPassthruClass);
    }

    @Test
    void defaultFlagsAndNoOpLifecycleMethodsAreStable() {
        TestDataSource ds = new TestDataSource();

        assertFalse(ds.willCreatePropertyValue(new Store(1), Store.P_Id));
        assertTrue(ds.getAllowIdChange());
        assertTrue(ds.getSupportsPreCount());
        assertFalse(ds.isAllowingBatch());
        assertFalse(ds.isInTransaction());
        ds.setReadOnly(true);
        assertTrue(ds.getReadOnly());
        ds.setIgnoreWrites(true);
        assertTrue(ds.getIgnoreWrites());
        assertDoesNotThrow(() -> ds.close());
        assertDoesNotThrow(() -> ds.reopen(1));
    }

    private static class TestDataSource extends OADataSource {
        int insertCalls;
        int updateCalls;
        int selectCalls;
        int selectPassthruCalls;
        int countResult;
        int countPassthruResult;
        OAObject inserted;
        OAObject updated;
        String[] lastIncludeProperties;
        String[] lastExcludeProperties;
        Class<?> lastSelectClass;
        String lastQueryWhere;
        Object[] lastParams;
        Class<?> lastPassthruClass;
        OADataSourceIterator nextSelect = new OADataSourceEmptyIterator();

        @Override
        public boolean isClassSupported(Class clazz, OAFilter filter) {
            return clazz == Store.class;
        }

        @Override
        public void updateMany2ManyLinks(OAObject masterObject, OAObject[] adds, OAObject[] removes, String propFromMaster) {
        }

        @Override
        public void insert(OAObject obj) {
            insertCalls++;
            inserted = obj;
        }

        @Override
        public void insertWithoutReferences(OAObject obj) {
            inserted = obj;
        }

        @Override
        public void update(OAObject obj, String[] includeProperties, String[] excludeProperties) {
            updateCalls++;
            updated = obj;
            lastIncludeProperties = includeProperties;
            lastExcludeProperties = excludeProperties;
        }

        @Override
        public void delete(OAObject obj) {
        }

        @Override
        public int count(Class selectClass, String queryWhere, Object[] params, OAObject whereObject, String propertyFromWhereObject,
                String extraWhere, int max) {
            return countResult;
        }

        @Override
        public int countPassthru(Class selectClass, String queryWhere, int max) {
            return countPassthruResult;
        }

        @Override
        public boolean supportsStorage() {
            return true;
        }

        @Override
        public <T> OADataSourceIterator<T> select(Class<T> selectClass, String queryWhere, Object[] params, String queryOrder,
                OAObject whereObject, String propertyFromWhereObject, String extraWhere, int max, OAFilter<T> filter, boolean bDirty) {
            selectCalls++;
            lastSelectClass = selectClass;
            lastQueryWhere = queryWhere;
            lastParams = params;
            return nextSelect;
        }

        @Override
        public <T> OADataSourceIterator<T> selectPassthru(Class<T> selectClass, String queryWhere, String queryOrder, int max,
                OAFilter<T> filter, boolean bDirty) {
            selectPassthruCalls++;
            lastPassthruClass = selectClass;
            return new OADataSourceEmptyIterator();
        }

        @Override
        public Object execute(String command) {
            return null;
        }

        @Override
        public void assignId(OAObject obj) {
        }

        @Override
        public byte[] getPropertyBlobValue(OAObject obj, String propertyName) {
            return null;
        }
    }
}
