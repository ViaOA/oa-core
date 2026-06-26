package com.viaoa.sync.remote;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.*;

import com.test.pos.model.oa.Register;
import com.test.pos.model.oa.Store;
import com.viaoa.datasource.clientserver.OADataSourceClient;
import com.viaoa.oa.OA;
import com.viaoa.object.OAObject;
import com.viaoa.runtime.OARuntime;

class RemoteClientImplTest {

    @BeforeEach
    void beforeEach() {
        OA oa = OARuntime.createDefaultOA(Register.class);
    }
    @AfterEach
    void afterEach() {
        OARuntime.oa(Register.class).close();
    }

    @Test
    void getDetailReturnsNullForInvalidRequestThroughClientGetDetail() {
        TestRemoteClient client = new TestRemoteClient(1, new ConcurrentHashMap<>());

        assertNull(client.getDetail(1, Store.class, null, Store.P_Registers, false));
        assertNull(client.getDetailNow(1, Store.class, null, Store.P_Registers, null, null, false));
    }

    @Test
    void getRemoteDataSourceIsLazyStableAndMarksCachedObjects() {
        Map<UUID, Boolean> map = new ConcurrentHashMap<>();
        TestRemoteClient client = new TestRemoteClient(1, map);
        Register register = new Register(100);

        RemoteDataSource ds = client.getRemoteDataSource();
        ds.setCached(register);

        assertSame(ds, client.getRemoteDataSource());
        assertEquals(Boolean.FALSE, map.get(register.getGuid()));
        assertEquals(1, client.updateObjectCacheCount.get());
    }

    @Test
    void closeReleasesLazyHelpersAndAllowsNewRemoteDataSource() {
        TestRemoteClient client = new TestRemoteClient(1, new ConcurrentHashMap<>());
        RemoteDataSource ds = client.getRemoteDataSource();

        client.close();

        assertNotSame(ds, client.getRemoteDataSource());
    }

    @Test
    void datasourceMethodsWrapDatasourceExceptions() {
        TestRemoteClient client = new TestRemoteClient(1, new ConcurrentHashMap<>()) {
            @Override
            public RemoteDataSource getRemoteDataSource() {
                return new RemoteDataSource() {
                    @Override
                    public Object datasource(int command, Object[] objects) {
                        throw new IllegalStateException("fail");
                    }

                    @Override
                    public void setCached(OAObject obj) {
                    }
                };
            }
        };

        RuntimeException ex = assertThrows(RuntimeException.class, () -> client.datasource(OADataSourceClient.IS_AVAILABLE, new Object[0]));
        assertTrue(ex.getMessage().contains("remoteClient.datasource"));
        assertThrows(RuntimeException.class, () -> client.datasourceReturnOnQueue(OADataSourceClient.IS_AVAILABLE, new Object[0]));
        assertThrows(RuntimeException.class, () -> client.datasourceNoReturn(OADataSourceClient.IS_AVAILABLE, new Object[0]));
    }

    @Test
    void createCopyReturnsCopyForCachedObjectAndNullForMissingObject() {
        Store store = new Store(1);
        store.setName("Original");
        TestRemoteClient client = new TestRemoteClient(1, new ConcurrentHashMap<>());

        Store copy = (Store) client.createCopy(Store.class, store.getObjectKey(), null);

        assertNotNull(copy);
        assertNotSame(store, copy);
        assertEquals("Original", copy.getName());
        assertNull(client.createCopy(Store.class, new com.viaoa.object.OAObjectKey(new Object[] { 999 }), null));
    }

    @Test
    void deleteAllClearsLoadedHub() {
        Store store = new Store(2);
        store.getRegisters().add(new Register(101));
        TestRemoteClient client = new TestRemoteClient(1, new ConcurrentHashMap<>());

        assertTrue(client.deleteAll(Store.class, store.getObjectKey(), Store.P_Registers));

        assertEquals(0, store.getRegisters().size());
    }

    @Test
    void refreshMethodsNoOpForMissingObject() {
        TestRemoteClient client = new TestRemoteClient(1, new ConcurrentHashMap<>());

        assertDoesNotThrow(() -> client.refresh(Store.class, new com.viaoa.object.OAObjectKey(new Object[] { 999 })));
        assertDoesNotThrow(() -> client.refresh(Store.class, new com.viaoa.object.OAObjectKey(new Object[] { 999 }), Store.P_Name));
    }

    private static class TestRemoteClient extends RemoteClientImpl {
        final AtomicInteger updateObjectCacheCount = new AtomicInteger();

        TestRemoteClient(int sessionId, Map<UUID, Boolean> hmGuid) {
            super(sessionId, hmGuid);
        }

        @Override
        public void updateObjectCache(OAObject obj) {
            updateObjectCacheCount.incrementAndGet();
        }
    }
}
