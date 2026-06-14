package com.viaoa.sync.remote;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;

import com.test.pos.model.oa.Store;
import com.viaoa.graph.OAGraphInternal;
import com.viaoa.graph.service.OAObjectService;
import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectKey;
import com.viaoa.runtime.OARuntime;
import com.viaoa.sync.model.ClientInfo;

class RemoteServerImplTest {

    @BeforeEach
    void beforeEach() {
        clearCache();
    }

    @AfterEach
    void afterEach() {
        clearCache();
    }

    @Test
    void pingEchoesAndPing2IsNoOp() {
        TestRemoteServer server = new TestRemoteServer();

        assertEquals("ping", server.ping("ping"));
        assertDoesNotThrow(() -> server.ping2("ignored"));
    }

    @Test
    void getDisplayMessageDefaultsToOASyncServer() {
        assertEquals("OASyncServer", new TestRemoteServer().getDisplayMessage());
    }

    @Test
    void saveReturnsFalseForMissingCachedObject() {
        TestRemoteServer server = new TestRemoteServer();

        assertFalse(server.save(Store.class, new OAObjectKey(new Object[] { 999 }), 0));
    }

    @Test
    void getObjectReturnsCachedObjectByObjectKey() {
        Store store = new Store(10);
        TestRemoteServer server = new TestRemoteServer();

        Store found = (Store) server.getObject(Store.class, store.getObjectKey());

        assertSame(store, found);
    }

    @Test
    void getObjectUsingPkeyReturnsNullWhenNoPrimaryKeyCacheEntryExists() {
        Store store = new Store(11);
        TestRemoteServer server = new TestRemoteServer();

        Store found = (Store) server.getObjectUsingPkey(Store.class, store.getObjectKey());

        assertNull(found);
    }

    @Test
    void runRemoteMethodInvokesObjectMethodByKey() {
        Store store = new Store(12);
        store.setName("Remote Store");
        TestRemoteServer server = new TestRemoteServer();

        Object value = server.runRemoteMethod(Store.class, store.getObjectKey(), "getName", null);

        assertEquals("Remote Store", value);
    }

    @Test
    void runRemoteMethodThrowsForMissingObjectOrMethod() {
        Store store = new Store(13);
        TestRemoteServer server = new TestRemoteServer();

        assertThrows(RuntimeException.class, () -> server.runRemoteMethod(Store.class, new OAObjectKey(new Object[] { 999 }), "getName", null));
        assertThrows(RuntimeException.class, () -> server.runRemoteMethod(Store.class, store.getObjectKey(), "notAMethod", null));
    }

    @Test
    void runRemoteMethod2InvokesMethodOnProvidedObject() {
        Store store = new Store(14);
        store.setName("Provided Store");
        TestRemoteServer server = new TestRemoteServer();

        Object value = server.runRemoteMethod2(store, "getName", null);

        assertEquals("Provided Store", value);
    }

    @Test
    void runRemoteMethodForHubReturnsNullForNullHubAndThrowsForMissingMethod() {
        TestRemoteServer server = new TestRemoteServer();

        assertNull(server.runRemoteMethod((Hub) null, "any", null));
        assertThrows(RuntimeException.class, () -> server.runRemoteMethod(new Hub<>(Store.class), "notAStaticHubMethod", null));
    }

    @Test
    void performThreadDumpReturnsStackTraceText() {
        String text = new TestRemoteServer().performThreadDump("test dump");

        assertNotNull(text);
        assertFalse(text.isBlank());
    }

    private void clearCache() {
        OAGraphInternal og = (OAGraphInternal) OARuntime.graph(Store.class);
        OAObjectService os = (OAObjectService) og.objectsInternal();
        os.getOAObjectCacheService().removeAllObjects();
    }

    private static class TestRemoteServer extends RemoteServerImpl {
        @Override
        public RemoteClientInterface getRemoteClient(ClientInfo clientInfo) {
            return null;
        }

        @Override
        public RemoteSessionInterface getRemoteSession(ClientInfo clientInfo, RemoteClientCallbackInterface callback) {
            return null;
        }

        @Override
        public long getNextFiftyObjectGuids() {
            return 50L;
        }

        @Override
        public void refreshCache(Class clazz) {
        }

        @Override
        public OAObject getUnique(Class<? extends OAObject> clazz, String propertyName, Object uniqueKey, boolean bAutoCreate) {
            return null;
        }
    }
}
