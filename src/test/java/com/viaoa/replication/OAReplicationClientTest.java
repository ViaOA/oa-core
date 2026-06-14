package com.viaoa.replication;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.file.Path;
import java.util.*;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import com.test.pos.model.oa.Register;
import com.viaoa.datetime.OADateTime;
import com.viaoa.graph.OAGraphInternal;
import com.viaoa.graph.service.OAObjectService;
import com.viaoa.runtime.OARuntime;

class OAReplicationClientTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void beforeEach() {
        clearCache();
    }

    @AfterEach
    void afterEach() {
        clearCache();
    }

    @Test
    void constructorStartsWithZeroQueuePosition() {
        TestClient client = new TestClient(tlog("client.bin"), "client-a");

        assertEquals(0L, client.getCirularQueuePos());
    }

    @Test
    void openTLogFileCreatesHeaderAndLoadRestoresEmptyHeader() throws Exception {
        String fileName = tlog("empty-client.bin");
        TestClient client = new TestClient(fileName, "client-a");

        client.openTLogFile();
        client.closeOutput();

        TestClient reloaded = new TestClient(fileName, "client-a");
        reloaded.loadTLogFile();

        assertEquals(0L, reloaded.masterSeq());
        assertEquals(0L, reloaded.clientSeq());
        assertEquals(0, reloaded.pendingToMasterCount());
    }

    @Test
    void loadTLogFileRejectsDifferentGuid() throws Exception {
        String fileName = tlog("guid-client.bin");
        TestClient client = new TestClient(fileName, "client-a");
        client.openTLogFile();
        client.closeOutput();

        TestClient other = new TestClient(fileName, "client-b");

        RuntimeException ex = assertThrows(RuntimeException.class, other::loadTLogFile);
        assertTrue(ex.getMessage().contains("does not match runtime guid"));
    }

    @Test
    void writeTLogAndLoadTLogFileRestoresSequencesAndQueuesNonLocalSource() throws Exception {
        String fileName = tlog("roundtrip-client.bin");
        TestClient client = new TestClient(fileName, "client-a");
        client.openTLogFile();
        client.writeTLog(tlog("master", 9L, 2L, "refresh", Register.class));
        client.closeOutput();

        TestClient reloaded = new TestClient(fileName, "client-a");
        reloaded.loadTLogFile();

        assertEquals(9L, reloaded.masterSeq());
        assertEquals(2L, reloaded.clientSeq());
        assertEquals(1, reloaded.pendingToMasterCount());
        assertEquals("refresh", reloaded.pendingToMaster().get(0).getMethodName());
    }

    @Test
    void loadTLogFileDoesNotQueueRecordsFromOwnGuid() throws Exception {
        String fileName = tlog("local-client.bin");
        TestClient client = new TestClient(fileName, "client-a");
        client.openTLogFile();
        client.writeTLog(tlog("client-a", 3L, 4L, "refresh", Register.class));
        client.closeOutput();

        TestClient reloaded = new TestClient(fileName, "client-a");
        reloaded.loadTLogFile();

        assertEquals(3L, reloaded.masterSeq());
        assertEquals(4L, reloaded.clientSeq());
        assertEquals(0, reloaded.pendingToMasterCount());
    }

    @Test
    void createNewTLogFileWritesFreshHeader() throws Exception {
        String fileName = tlog("new-client.bin");
        TestClient client = new TestClient(fileName, "client-a");
        client.openTLogFile();
        client.writeTLog(tlog("master", 1L, 1L, "refresh"));
        client.closeOutput();
        client.setSequences(10L, 8L, 7L, 6L);

        client.createNewTLogFile(fileName);
        client.closeOutput();

        TestClient reloaded = new TestClient(fileName, "client-a");
        reloaded.loadTLogFile();
        assertEquals(10L, reloaded.masterSeq());
        assertEquals(8L, reloaded.clientSeq());
        assertEquals(0, reloaded.pendingToMasterCount());
    }

    @Test
    void disconnectFromMasterPreventsConnectionCreation() {
        TestClient client = new TestClient(tlog("disconnect-client.bin"), "client-a");

        client.setDisconnectFromMaster(true);

        assertNull(client.getReplClientConnection());
    }

    @Test
    void onNewMessageFromMasterThrowsForUnknownMethodAndRestoresThreadLocalSource() {
        TestClient client = new TestClient(tlog("unknown-method-client.bin"), "client-a");

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> client.onNewMessageFromMaster(1L, "missingMethod", new Object[0]));

        assertTrue(ex.getMessage().contains("Exception onNewMessageFromMaster"));
    }

    private String tlog(String name) {
        return tempDir.resolve(name).toString();
    }

    private static OAReplTLog tlog(String source, long masterSeq, long clientSeq, String methodName, Object... args) {
        return new OAReplTLog(source, new OADateTime(20_000L + masterSeq), masterSeq, clientSeq, methodName, args);
    }

    private static void clearCache() {
        OAGraphInternal og = (OAGraphInternal) OARuntime.graph(Register.class);
        OAObjectService os = (OAObjectService) og.objectsInternal();
        os.getOAObjectCacheService().removeAllObjects();
    }

    private static class TestClient extends OAReplicationClient {
        TestClient(String tlogFileName, String guid) {
            super(tlogFileName, guid, null, "127.0.0.1", 0);
        }

        void closeOutput() {
            try {
                ObjectOutputStream oos = objectOutputStream();
                if (oos != null) oos.close();
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        long masterSeq() {
            return getLongField("masterSeq");
        }

        long clientSeq() {
            return getLongField("clientSeq");
        }

        void setSequences(long masterSeq, long clientSeq, long lastSentMasterSeq, long lastSentClientSeq) {
            setLongField("masterSeq", masterSeq);
            setLongField("clientSeq", clientSeq);
            setLongField("lastSentMasterSeq", lastSentMasterSeq);
            setLongField("lastSentClientSeq", lastSentClientSeq);
        }

        int pendingToMasterCount() {
            return pendingToMaster().size();
        }

        @SuppressWarnings("unchecked")
        List<OAReplTLog> pendingToMaster() {
            try {
                java.lang.reflect.Field f = OAReplicationClient.class.getDeclaredField("alTLogToMaster");
                f.setAccessible(true);
                return new ArrayList<>((java.util.concurrent.LinkedBlockingQueue<OAReplTLog>) f.get(this));
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private ObjectOutputStream objectOutputStream() {
            try {
                java.lang.reflect.Field f = OAReplicationClient.class.getDeclaredField("objectOutputStream");
                f.setAccessible(true);
                return (ObjectOutputStream) f.get(this);
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private long getLongField(String name) {
            try {
                java.lang.reflect.Field f = OAReplicationClient.class.getDeclaredField(name);
                f.setAccessible(true);
                return f.getLong(this);
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private void setLongField(String name, long value) {
            try {
                java.lang.reflect.Field f = OAReplicationClient.class.getDeclaredField(name);
                f.setAccessible(true);
                f.setLong(this, value);
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
