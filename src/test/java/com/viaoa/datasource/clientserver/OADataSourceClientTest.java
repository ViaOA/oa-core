package com.viaoa.datasource.clientserver;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.test.pos.model.oa.Register;
import com.test.pos.model.oa.Store;
import com.viaoa.datasource.OADataSourceIterator;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectKey;
import com.viaoa.sync.remote.RemoteClientInterface;

class OADataSourceClientTest {

    @Test
    void constructorsUseProvidedOrDefaultPackageWithoutConnecting() {
        assertNotNull(new TestClient("pos"));
        assertNotNull(new TestClient());
    }

    @Test
    void setAssignIdOnCreateOverridesRemoteLookup() {
        TestClient ds = new TestClient();

        ds.setAssignIdOnCreate(true);

        assertTrue(ds.getAssignIdOnCreate());
        assertEquals(0, ds.remote.datasourceCalls);
    }

    @Test
    void getAssignIdOnCreateFetchesAndCachesRemoteValue() {
        TestClient ds = new TestClient();
        ds.remote.datasourceResult = Boolean.TRUE;

        assertTrue(ds.getAssignIdOnCreate());
        assertTrue(ds.getAssignIdOnCreate());

        assertEquals(1, ds.remote.datasourceCalls);
        assertEquals(OADataSourceClient.GET_ASSIGN_ID_ON_CREATE, ds.remote.lastCommand);
    }

    @Test
    void isAvailableReturnsBooleanRemoteResponseOnly() {
        TestClient ds = new TestClient();
        ds.remote.datasourceResult = Boolean.TRUE;

        assertTrue(ds.isAvailable());

        ds.remote.datasourceResult = "notBoolean";
        assertFalse(ds.isAvailable());
    }

    @Test
    void getMaxLengthFetchesCachesAndSetMaxLengthOverridesRemoteLookup() {
        TestClient ds = new TestClient();
        ds.remote.datasourceResult = Integer.valueOf(50);

        assertEquals(50, ds.getMaxLength(Store.class, Store.P_Name));
        ds.remote.datasourceResult = Integer.valueOf(10);
        assertEquals(50, ds.getMaxLength(Store.class, Store.P_Name));
        ds.setMaxLength(Store.class, Store.P_Name, 75);
        ds.setMaxLength(null, Store.P_Name, 1);
        ds.setMaxLength(Store.class, null, 1);

        assertEquals(75, ds.getMaxLength(Store.class, Store.P_Name));
    }

    @Test
    void verifyConnectionThrowsWhenRemoteClientIsMissing() {
        TestClient ds = new TestClient((FakeRemoteClient) null);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> ds.verifyConnectionForTest());

        assertTrue(ex.getMessage().contains("connection is not set"));
    }

    @Test
    void isClassSupportedHandlesNullRemoteAndCachesRemoteResponse() {
        TestClient missingRemote = new TestClient((FakeRemoteClient) null);
        assertFalse(missingRemote.isClassSupported(Store.class, null));
        assertFalse(missingRemote.isClassSupported(null, null));

        TestClient ds = new TestClient();
        ds.remote.datasourceResult = Boolean.TRUE;

        assertTrue(ds.isClassSupported(Store.class, null));
        ds.remote.datasourceResult = Boolean.FALSE;
        assertTrue(ds.isClassSupported(Store.class, null));
        assertEquals(OADataSourceClient.IS_CLASS_SUPPORTED, ds.remote.lastCommand);
    }

    @Test
    void insertWithoutReferencesAndInsertIgnoreNullAndForwardObjects() {
        TestClient ds = new TestClient();
        Register register = new Register(1);

        ds.insertWithoutReferences(null);
        ds.insert(null);
        ds.insertWithoutReferences(register);
        assertEquals(OADataSourceClient.INSERT_WO_REFERENCES, ds.remote.lastCommand);
        assertSame(register, ds.remote.lastArgs[0]);

        ds.insert(register);
        assertEquals(OADataSourceClient.INSERT, ds.remote.lastCommand);
        assertSame(register, ds.remote.lastArgs[0]);
    }

    @Test
    void updateSaveDeleteAndDeleteAllForwardExpectedCommands() {
        TestClient ds = new TestClient();
        Register register = new Register(1);

        ds.update(null, null, null);
        ds.save(null);
        ds.delete(null);
        ds.deleteAll(null);
        assertEquals(0, ds.remote.datasourceCalls);
        assertEquals(0, ds.remote.returnOnQueueCalls);

        ds.update(register, new String[] { Register.P_Code }, null);
        assertEquals(OADataSourceClient.UPDATE, ds.remote.lastCommand);

        ds.save(register);
        assertEquals(OADataSourceClient.SAVE, ds.remote.lastReturnOnQueueCommand);

        ds.delete(register);
        assertEquals(OADataSourceClient.DELETE, ds.remote.lastReturnOnQueueCommand);

        ds.deleteAll(Register.class);
        assertEquals(OADataSourceClient.DELETE_ALL, ds.remote.lastReturnOnQueueCommand);
    }

    @Test
    void countAndCountPassthruReturnIntegerRemoteResponsesOnly() {
        TestClient ds = new TestClient();
        ds.remote.datasourceResult = Integer.valueOf(4);

        assertEquals(4, ds.count(Store.class, "id > 0", null, null, null, null, 0));
        assertEquals(OADataSourceClient.COUNT, ds.remote.lastCommand);

        ds.remote.datasourceResult = "notInteger";
        assertEquals(-1, ds.count(Store.class, "id > 0", null, null, null, null, 0));

        ds.remote.datasourceResult = Integer.valueOf(5);
        assertEquals(5, ds.countPassthru(Store.class, "native", 0));
        assertEquals(OADataSourceClient.COUNTPASSTHRU, ds.remote.lastCommand);
    }

    @Test
    void supportsStorageFetchesAndCachesRemoteValue() {
        TestClient ds = new TestClient();
        ds.remote.datasourceResult = Boolean.TRUE;

        assertTrue(ds.supportsStorage());
        ds.remote.datasourceResult = Boolean.FALSE;
        assertTrue(ds.supportsStorage());

        assertEquals(1, ds.remote.datasourceCalls);
        assertEquals(OADataSourceClient.SUPPORTSSTORAGE, ds.remote.lastCommand);
    }

    @Test
    void selectReturnsNullWhenRemoteReturnsNull() {
        TestClient ds = new TestClient();
        ds.remote.datasourceResult = null;

        assertNull(ds.select(Store.class, null, null, null, null, null, null, 0, null, false));

        assertEquals(OADataSourceClient.SELECT, ds.remote.lastCommand);
    }

    @Test
    void selectPassthruCreatesIteratorForRemoteTokenAndUsesBatchCommand() {
        TestClient ds = new TestClient();
        ds.remote.datasourceResult = "iteratorId";
        ds.remote.nextBatch = new Object[0];

        OADataSourceIterator it = ds.selectPassthru(Store.class, "native", Store.P_Name, 10, null, false);

        assertNotNull(it);
        assertFalse(it.hasNext());
        assertEquals(OADataSourceClient.IT_NEXT, ds.remote.lastCommand);
    }

    @Test
    void executeAndAssignIdDelegateToRemoteClient() {
        TestClient ds = new TestClient();
        Register register = new Register(1);
        ds.remote.datasourceResult = "done";

        assertEquals("done", ds.execute("cmd"));
        assertEquals(OADataSourceClient.EXECUTE, ds.remote.lastCommand);

        ds.assignId(register);
        assertEquals(OADataSourceClient.ASSIGN_ID, ds.remote.lastReturnOnQueueCommand);
    }

    @Test
    void willCreatePropertyValueReturnsBooleanRemoteResponseOnly() {
        TestClient ds = new TestClient();
        Register register = new Register(1);
        ds.remote.datasourceResult = Boolean.TRUE;

        assertTrue(ds.willCreatePropertyValue(register, Register.P_Id));
        ds.remote.datasourceResult = "notBoolean";
        assertFalse(ds.willCreatePropertyValue(register, Register.P_Id));
    }

    @Test
    void iteratorRemoveForwardsNoReturnAndCloses() {
        TestClient ds = new TestClient();
        ds.remote.datasourceResult = "iteratorId";
        ds.remote.nextBatch = new Object[0];
        OADataSourceIterator it = ds.selectPassthru(Store.class, "native", null, 0, null, false);

        it.remove();

        assertEquals(OADataSourceClient.IT_REMOVE, ds.remote.lastNoReturnCommand);
    }

    @Test
    void updateMany2ManyLinksAndGetPropertyBlobValueForwardObjectKeys() {
        TestClient ds = new TestClient();
        Register register = new Register(1);
        byte[] bytes = new byte[] { 1, 2, 3 };

        ds.updateMany2ManyLinks(register, new OAObject[0], new OAObject[0], Register.P_RegisterSessions);
        assertEquals(OADataSourceClient.UPDATE_MANY2MANY_LINKS, ds.remote.lastCommand);
        assertSame(Register.class, ds.remote.lastArgs[0]);

        ds.remote.datasourceResult = bytes;
        assertArrayEquals(bytes, ds.getPropertyBlobValue(register, Register.P_Code));
        assertEquals(OADataSourceClient.GET_PROPERTY, ds.remote.lastCommand);

        ds.remote.datasourceResult = "notBytes";
        assertNull(ds.getPropertyBlobValue(register, Register.P_Code));
    }

    @Test
    void isClientReturnsTrue() {
        assertTrue(new TestClient().isClient());
    }

    private static class TestClient extends OADataSourceClient {
        private final FakeRemoteClient remote;

        TestClient() {
            this("test");
        }

        TestClient(String packageName) {
            this(packageName, new FakeRemoteClient());
        }

        TestClient(FakeRemoteClient remote) {
            this("test", remote);
        }

        TestClient(String packageName, FakeRemoteClient remote) {
            super(packageName);
            this.remote = remote;
        }

        @Override
        public RemoteClientInterface getRemoteClient() {
            return remote;
        }

        void verifyConnectionForTest() {
            verifyConnection();
        }
    }

    private static class FakeRemoteClient implements RemoteClientInterface {
        Object datasourceResult;
        Object[] nextBatch;
        int datasourceCalls;
        int returnOnQueueCalls;
        int noReturnCalls;
        int lastCommand = -1;
        int lastReturnOnQueueCommand = -1;
        int lastNoReturnCommand = -1;
        Object[] lastArgs;

        @Override
        public <T extends OAObject> T createCopy(Class<T> objectClass, OAObjectKey objectKey, String[] excludeProperties) {
            return null;
        }

        @Override
        public Object getDetail(int id, Class masterClass, OAObjectKey masterObjectKey, String property, boolean bForHubMerger) {
            return null;
        }

        @Override
        public Object getDetail(int id, Class masterClass, OAObjectKey masterObjectKey, String property, String[] masterProps,
                OAObjectKey[] siblingKeys, boolean bForHubMerger) {
            return null;
        }

        @Override
        public Object getDetailNow(int id, Class masterClass, OAObjectKey masterObjectKey, String property, String[] masterProps,
                OAObjectKey[] siblingKeys, boolean bForHubMerger) {
            return null;
        }

        @Override
        public Object datasource(int command, Object[] objects) {
            datasourceCalls++;
            lastCommand = command;
            lastArgs = objects;
            if (command == OADataSourceClient.IT_NEXT) {
                return nextBatch;
            }
            return datasourceResult;
        }

        @Override
        public Object datasourceReturnOnQueue(int command, Object[] objects) {
            returnOnQueueCalls++;
            lastReturnOnQueueCommand = command;
            lastArgs = objects;
            return datasourceResult;
        }

        @Override
        public void datasourceNoReturn(int command, Object[] objects) {
            noReturnCalls++;
            lastNoReturnCommand = command;
            lastArgs = objects;
        }

        @Override
        public boolean deleteAll(Class objectClass, OAObjectKey objectKey, String hubPropertyName) {
            return false;
        }

        @Override
        public void refresh(Class clazz, OAObjectKey objectKey) {
        }

        @Override
        public void refresh(Class clazz, OAObjectKey objectKey, String propertyName) {
        }
    }
}
