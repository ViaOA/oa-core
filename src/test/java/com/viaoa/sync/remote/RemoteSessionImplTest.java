package com.viaoa.sync.remote;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.*;

import com.test.pos.model.oa.Register;
import com.viaoa.graph.OAGraphInternal;
import com.viaoa.graph.service.OAObjectService;
import com.viaoa.object.OAObjectKey;
import com.viaoa.runtime.OARuntime;
import com.viaoa.sync.model.ClientInfo;

class RemoteSessionImplTest {

    @BeforeEach
    void beforeEach() {
        clearCache();
    }

    @AfterEach
    void afterEach() {
        clearCache();
    }

    @Test
    void objectCreatedAddsGuidOnlyIfAbsent() {
        UUID guid = UUID.randomUUID();
        Map<UUID, Boolean> map = new ConcurrentHashMap<>();
        map.put(guid, Boolean.TRUE);
        TestRemoteSession session = new TestRemoteSession(7, map);

        session.objectCreated(guid);
        session.objectCreated(UUID.fromString("00000000-0000-0000-0000-000000000001"));

        assertEquals(Boolean.TRUE, map.get(guid));
        assertEquals(Boolean.FALSE, map.get(UUID.fromString("00000000-0000-0000-0000-000000000001")));
    }

    @Test
    void objectsFinalizedRemovesGuidsAndIgnoresNullArray() {
        UUID guid1 = UUID.randomUUID();
        UUID guid2 = UUID.randomUUID();
        Map<UUID, Boolean> map = new ConcurrentHashMap<>();
        map.put(guid1, Boolean.FALSE);
        map.put(guid2, Boolean.FALSE);
        TestRemoteSession session = new TestRemoteSession(7, map);

        session.objectsFinalized(null);
        session.objectsFinalized(new UUID[] { guid1 });

        assertFalse(map.containsKey(guid1));
        assertTrue(map.containsKey(guid2));
    }

    @Test
    void updateObjectsWithoutHubsTracksAndRemovesCachedObject() {
        Register register = new Register(1001);
        TestRemoteSession session = new TestRemoteSession(7, new ConcurrentHashMap<>());
        OAObjectKey key = register.getObjectKey();

        session.updateObjectsWithoutHubs(Register.class, key, false);
        assertTrue(session.hasObjectWithoutHub(register.getGuid()));

        session.updateObjectsWithoutHubs(Register.class, key, true);
        assertFalse(session.hasObjectWithoutHub(register.getGuid()));
    }

    @Test
    void updateObjectsWithoutHubsIgnoresNullInputs() {
        TestRemoteSession session = new TestRemoteSession(7, new ConcurrentHashMap<>());

        assertDoesNotThrow(() -> session.updateObjectsWithoutHubs(null, null, false));
        assertEquals(0, session.objectWithoutHubCount());
    }

    @Test
    void clearCachesClearsGuidAndObjectRetentionState() {
        Register register = new Register(1002);
        UUID guid = UUID.randomUUID();
        Map<UUID, Boolean> map = new ConcurrentHashMap<>();
        map.put(guid, Boolean.FALSE);
        TestRemoteSession session = new TestRemoteSession(7, map);
        session.updateObjectsWithoutHubs(Register.class, register.getObjectKey(), false);

        session.clearCaches();

        assertTrue(map.isEmpty());
        assertEquals(0, session.objectWithoutHubCount());
    }

    @Test
    void setLockByObjectAndClearLocksManageThisClientLockState() {
        Register register = new Register(1003);
        TestRemoteSession session = new TestRemoteSession(7, new ConcurrentHashMap<>());

        session.setLock(register, true);
        assertTrue(session.isLockedByThisClient(Register.class, register.getObjectKey()));
        assertTrue(session.isLocked(Register.class, register.getObjectKey()));

        session.clearLocks();

        assertFalse(session.isLockedByThisClient(Register.class, register.getObjectKey()));
    }

    @Test
    void setLockByKeyReturnsFalseWhenObjectIsMissingFromCache() {
        TestRemoteSession session = new TestRemoteSession(7, new ConcurrentHashMap<>());
        OAObjectKey missingKey = new OAObjectKey(new Object[] { 999999 });

        assertFalse(session.setLock(Register.class, missingKey, true));
    }

    @Test
    void setLockByKeyCanLockAndUnlockCachedObject() {
        Register register = new Register(1004);
        TestRemoteSession session = new TestRemoteSession(7, new ConcurrentHashMap<>());

        assertTrue(session.setLock(Register.class, register.getObjectKey(), true));
        assertTrue(session.isLockedByThisClient(Register.class, register.getObjectKey()));

        assertTrue(session.setLock(Register.class, register.getObjectKey(), false));
        assertFalse(session.isLockedByThisClient(Register.class, register.getObjectKey()));
    }

    @Test
    void createNewObjectReturnsCachedObjectAndTracksGuid() {
        Map<UUID, Boolean> map = new ConcurrentHashMap<>();
        TestRemoteSession session = new TestRemoteSession(7, map);

        Object obj = session.createNewObject(Register.class);

        assertInstanceOf(Register.class, obj);
        Register register = (Register) obj;
        assertEquals(Boolean.FALSE, map.get(register.getGuid()));
        assertTrue(session.hasObjectWithoutHub(register.getGuid()));
    }

    @Test
    void pingEchoesMessageAndPing2IsNoOp() {
        TestRemoteSession session = new TestRemoteSession(7, new ConcurrentHashMap<>());

        assertEquals("pong", session.ping("pong"));
        assertDoesNotThrow(() -> session.ping2("ignored"));
    }

    @Test
    void sendExceptionAndUpdateHooksCanBeImplementedBySubclass() {
        TestRemoteSession session = new TestRemoteSession(7, new ConcurrentHashMap<>());
        RuntimeException ex = new RuntimeException("boom");
        ClientInfo ci = new ClientInfo();

        session.sendException("msg", ex);
        session.update(ci);

        assertEquals("msg", session.lastExceptionMessage.get());
        assertSame(ex, session.lastException.get());
        assertSame(ci, session.lastClientInfo.get());
    }

    private void clearCache() {
        OAGraphInternal og = (OAGraphInternal) OARuntime.graph(Register.class);
        OAObjectService os = (OAObjectService) og.objectsInternal();
        os.getOAObjectCacheService().removeAllObjects();
    }

    private static class TestRemoteSession extends RemoteSessionImpl {
        final AtomicReference<String> lastExceptionMessage = new AtomicReference<>();
        final AtomicReference<Throwable> lastException = new AtomicReference<>();
        final AtomicReference<ClientInfo> lastClientInfo = new AtomicReference<>();

        TestRemoteSession(int sessionId, Map<UUID, Boolean> hmGuid) {
            super(sessionId, hmGuid);
        }

        boolean hasObjectWithoutHub(UUID guid) {
            return hmObjectsWithoutHubs.containsKey(guid);
        }

        int objectWithoutHubCount() {
            return hmObjectsWithoutHubs.size();
        }

        @Override
        public boolean isLocked(Class objectClass, OAObjectKey objectKey) {
            return isLockedByThisClient(objectClass, objectKey) || isLockedByAnotherClient(objectClass, objectKey);
        }

        @Override
        public boolean isLockedByAnotherClient(Class objectClass, OAObjectKey objectKey) {
            return false;
        }

        @Override
        public void sendException(String msg, Throwable ex) {
            lastExceptionMessage.set(msg);
            lastException.set(ex);
        }

        @Override
        public void update(ClientInfo ci) {
            lastClientInfo.set(ci);
        }
    }
}
