package com.viaoa.sync.remote;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.*;

import com.test.pos.model.oa.Store;
import com.viaoa.datasource.*;
import com.viaoa.datasource.clientserver.OADataSourceClient;
import com.viaoa.filter.OAFilter;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectKey;

class RemoteDataSourceTest {

    @Test
    void datasourceDispatchesClassBasedCapabilities() {
        TestDataSource ds = new TestDataSource();
        TestRemoteDataSource rds = new TestRemoteDataSource(ds);

        assertEquals(Boolean.TRUE, rds.datasource(OADataSourceClient.IS_CLASS_SUPPORTED, new Object[] { Store.class }));
        assertEquals(42, rds.datasource(OADataSourceClient.MAX_LENGTH, new Object[] { Store.class, Store.P_Name }));
        assertEquals(12, rds.datasource(OADataSourceClient.COUNT, countArgs(Store.class, null, null)));
        assertEquals(5, rds.datasource(OADataSourceClient.COUNTPASSTHRU, new Object[] { Store.class, "native", 0 }));
    }

    @Test
    void datasourceDispatchesClasslessCapabilitiesWhenDefaultDatasourceIsAvailable() {
        TestDataSource ds = new TestDataSource();
        ds.setAssignIdOnCreate(true);
        TestRemoteDataSource rds = new TestRemoteDataSource(ds);

        assertEquals(Boolean.TRUE, rds.datasource(OADataSourceClient.IS_AVAILABLE, new Object[0]));
        assertEquals(Boolean.TRUE, rds.datasource(OADataSourceClient.GET_ASSIGN_ID_ON_CREATE, new Object[0]));
        assertEquals(Boolean.TRUE, rds.datasource(OADataSourceClient.SUPPORTSSTORAGE, new Object[0]));
        assertEquals("ran:select 1", rds.datasource(OADataSourceClient.EXECUTE, new Object[] { "select 1" }));
    }

    @Test
    void datasourceWriteCommandsDelegateToDatasourceAndReturnNull() {
        TestDataSource ds = new TestDataSource();
        TestRemoteDataSource rds = new TestRemoteDataSource(ds);
        Store store = new Store(1);

        assertNull(rds.datasource(OADataSourceClient.INSERT, new Object[] { store }));
        assertNull(rds.datasource(OADataSourceClient.UPDATE, new Object[] { store, null, null }));
        assertNull(rds.datasource(OADataSourceClient.SAVE, new Object[] { store }));
        assertNull(rds.datasource(OADataSourceClient.DELETE, new Object[] { store }));
        assertNull(rds.datasource(OADataSourceClient.DELETE_ALL, new Object[] { Store.class }));
        assertNull(rds.datasource(OADataSourceClient.INSERT_WO_REFERENCES, new Object[] { store }));

        assertEquals(2, ds.insertCount);
        assertEquals(1, ds.updateCount);
        assertEquals(1, ds.deleteCount);
        assertEquals(1, ds.deleteAllCount);
        assertEquals(1, ds.insertWithoutReferencesCount);
    }

    @Test
    void datasourceSelectCreatesIteratorAndNextBatchesObjectsAndMarksCache() {
        Store s1 = new Store(1);
        Store s2 = new Store(2);
        TestDataSource ds = new TestDataSource();
        ds.selectResults.add(s1);
        ds.selectResults.add(s2);
        TestRemoteDataSource rds = new TestRemoteDataSource(ds);

        String id = (String) rds.datasource(OADataSourceClient.SELECT, selectArgs(Store.class));
        assertNotNull(id);
        assertEquals(Boolean.TRUE, rds.datasource(OADataSourceClient.IT_HASNEXT, new Object[] { id }));

        Object[] batch = (Object[]) rds.datasource(OADataSourceClient.IT_NEXT, new Object[] { id });

        assertArrayEquals(new Object[] { s1, s2 }, batch);
        assertEquals(2, rds.cachedCount.get());
        assertEquals(Boolean.FALSE, rds.datasource(OADataSourceClient.IT_HASNEXT, new Object[] { id }));
    }

    @Test
    void datasourceNextReturnsNullForUnknownIteratorAndRemoveClearsKnownIterator() {
        TestDataSource ds = new TestDataSource();
        ds.selectResults.add(new Store(3));
        TestRemoteDataSource rds = new TestRemoteDataSource(ds);

        assertNull(rds.datasource(OADataSourceClient.IT_NEXT, new Object[] { "missing" }));
        String id = (String) rds.datasource(OADataSourceClient.SELECT, selectArgs(Store.class));

        assertNull(rds.datasource(OADataSourceClient.IT_REMOVE, new Object[] { id }));
        assertEquals(Boolean.FALSE, rds.datasource(OADataSourceClient.IT_HASNEXT, new Object[] { id }));
    }

    @Test
    void datasourceGetPropertyDelegatesBlobLookup() {
        Store store = new Store(4);
        TestDataSource ds = new TestDataSource();
        ds.objectsById.put(store.getId(), store);
        TestRemoteDataSource rds = new TestRemoteDataSource(ds);

        Object value = rds.datasource(OADataSourceClient.GET_PROPERTY, new Object[] { Store.class, store.getObjectKey(), Store.P_Name });

        assertArrayEquals(new byte[] { 1, 2, 3 }, (byte[]) value);
        assertNull(ds.lastBlobObject);
        assertEquals(Store.P_Name, ds.lastBlobProperty);
    }

    @Test
    void unsupportedCommandFallsThroughToNull() {
        TestRemoteDataSource rds = new TestRemoteDataSource(new TestDataSource());

        assertNull(rds.datasource(OADataSourceClient.WILLCREATEPROPERTYVALUE, new Object[] { new Store(5), Store.P_Name }));
    }

    private static Object[] countArgs(Class<?> clazz, Class<?> whereClass, Object whereKey) {
        return new Object[] { clazz, null, null, whereClass, whereKey, null, null, 0 };
    }

    private static Object[] selectArgs(Class<?> clazz) {
        return new Object[] { clazz, null, null, null, null, null, null, null, 0, false, false };
    }

    private static class TestRemoteDataSource extends RemoteDataSource {
        final TestDataSource ds;
        final AtomicInteger cachedCount = new AtomicInteger();

        TestRemoteDataSource(TestDataSource ds) {
            this.ds = ds;
        }

        @Override
        protected OADataSource getDataSource(Class c) {
            return ds;
        }

        @Override
        protected OADataSource getDataSource() {
            return ds;
        }

        @Override
        public void setCached(OAObject obj) {
            cachedCount.incrementAndGet();
        }
    }

    private static class TestDataSource extends OADataSource {
        final List<Store> selectResults = new ArrayList<>();
        final Map<Integer, Store> objectsById = new HashMap<>();
        int insertCount;
        int insertWithoutReferencesCount;
        int updateCount;
        int deleteCount;
        int deleteAllCount;
        OAObject lastBlobObject;
        String lastBlobProperty;

        @Override
        public boolean isClassSupported(Class clazz, OAFilter filter) {
            return clazz == Store.class;
        }

        @Override
        public void updateMany2ManyLinks(OAObject masterObject, OAObject[] adds, OAObject[] removes, String propFromMaster) {
        }

        @Override
        public void insert(OAObject obj) {
            insertCount++;
        }

        @Override
        public void insertWithoutReferences(OAObject obj) {
            insertWithoutReferencesCount++;
        }

        @Override
        public void update(OAObject obj, String[] includeProperties, String[] excludeProperties) {
            updateCount++;
        }

        @Override
        public void delete(OAObject obj) {
            deleteCount++;
        }

        @Override
        public void deleteAll(Class c) {
            deleteAllCount++;
        }

        @Override
        public int count(Class selectClass, String queryWhere, Object[] params, OAObject whereObject, String propertyFromWhereObject, String extraWhere, int max) {
            return 12;
        }

        @Override
        public int countPassthru(Class selectClass, String queryWhere, int max) {
            return 5;
        }

        @Override
        public boolean supportsStorage() {
            return true;
        }

        @Override
        public <T> OADataSourceIterator<T> select(Class<T> selectClass, String queryWhere, Object[] params, String queryOrder, OAObject whereObject, String propertyFromWhereObject, String extraWhere, int max, OAFilter<T> filter, boolean bDirty) {
            return new IteratorDataSource<>((List<T>) selectResults);
        }

        @Override
        public <T> OADataSourceIterator<T> selectPassthru(Class<T> selectClass, String queryWhere, String queryOrder, int max, OAFilter<T> filter, boolean bDirty) {
            return new IteratorDataSource<>((List<T>) selectResults);
        }

        @Override
        public Object execute(String command) {
            return "ran:" + command;
        }

        @Override
        public void assignId(OAObject obj) {
        }

        @Override
        public int getMaxLength(Class c, String propertyName) {
            return 42;
        }

        @Override
        public <T extends OAObject> T getObject(Class<T> clazz, OAObjectKey key) {
            Object[] ids = key == null ? null : key.getObjectIds();
            if (ids == null || ids.length == 0) return null;
            Object id = ids[0];
            if (id instanceof OAObjectKey) {
                Object[] nested = ((OAObjectKey) id).getObjectIds();
                id = nested == null || nested.length == 0 ? null : nested[0];
            }
            return (T) objectsById.get(id);
        }

        @Override
        public byte[] getPropertyBlobValue(OAObject obj, String propertyName) {
            lastBlobObject = obj;
            lastBlobProperty = propertyName;
            return new byte[] { 1, 2, 3 };
        }
    }

    private static class IteratorDataSource<T> implements OADataSourceIterator<T> {
        final Iterator<T> it;

        IteratorDataSource(List<T> list) {
            this.it = list.iterator();
        }

        @Override
        public boolean hasNext() {
            return it.hasNext();
        }

        @Override
        public T next() {
            return it.next();
        }

        @Override
        public void remove() {
        }
    }
}
