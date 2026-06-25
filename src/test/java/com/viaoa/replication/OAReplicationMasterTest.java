package com.viaoa.replication;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import com.test.pos.model.oa.Register;
import com.viaoa.datetime.OADateTime;
import com.viaoa.oa.OA;
import com.viaoa.remote.info.RequestInfo;
import com.viaoa.runtime.OARuntime;

class OAReplicationMasterTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void beforeEach() {
        OA oa = OARuntime.oa(Register.class);
    }
    @AfterEach
    void afterEach() {
        OARuntime.oa(Register.class).close();
    }

    @Test
    void constructorStartsWithZeroQueuePosition() {
        TestMaster master = new TestMaster(tlog("master.bin"));

        assertEquals(0L, master.getCirularQueuePos());
    }

    @Test
    void openTLogFileCreatesHeaderAndLoadRestoresEmptyHeader() {
        String fileName = tlog("empty-master.bin");
        TestMaster master = new TestMaster(fileName);

        master.openTLogFile();
        master.closeOutput();

        TestMaster reloaded = new TestMaster(fileName);
        reloaded.loadTLogFile();

        assertEquals(0L, reloaded.currentMasterSeq());
        assertEquals(0, reloaded.totalTLogs());
    }

    @Test
    void writeTLogAndLoadTLogFileRoundTripMessages() {
        String fileName = tlog("roundtrip-master.bin");
        TestMaster master = new TestMaster(fileName);
        master.openTLogFile();
        master.writeTLog(tlog("source-a", 1L, 0L, "refresh", Register.class));
        master.writeTLog(tlog("source-b", 2L, 0L, "propertyChange", Register.P_Code));
        master.closeOutput();

        TestMaster reloaded = new TestMaster(fileName);
        reloaded.loadTLogFile();

        assertEquals(2L, reloaded.currentMasterSeq());
        assertEquals(2, reloaded.totalTLogs());
        assertEquals("refresh", reloaded.allTLogs().get(0).getMethodName());
        assertEquals("propertyChange", reloaded.allTLogs().get(1).getMethodName());
    }

    @Test
    void createNewTLogFileOverwritesExistingLogWithCurrentHeader() {
        String fileName = tlog("new-master.bin");
        TestMaster master = new TestMaster(fileName);
        master.openTLogFile();
        master.writeTLog(tlog("source", 1L, 0L, "refresh"));
        master.closeOutput();
        master.setCurrentMasterSeq(44L);

        master.createNewTLogFile(fileName);
        master.closeOutput();

        TestMaster reloaded = new TestMaster(fileName);
        reloaded.loadTLogFile();
        assertEquals(44L, reloaded.currentMasterSeq());
        assertEquals(0, reloaded.totalTLogs());
    }

    @Test
    void addTLogStoresMessagesInOrder() {
        TestMaster master = new TestMaster(tlog("in-memory-master.bin"));
        OAReplTLog t1 = tlog("source", 1L, 0L, "refresh");
        OAReplTLog t2 = tlog("source", 2L, 0L, "propertyChange");

        master.addTLog(t1);
        master.addTLog(t2);

        assertEquals(List.of(t1, t2), master.allTLogs());
    }

    @Test
    void onClientDisconnectedWithoutSessionIsSafe() {
        TestMaster master = new TestMaster(tlog("disconnect-master.bin"));

        assertDoesNotThrow(() -> master.onClientDisconnected(123));
    }

    @Test
    void stopBeforeStartThrowsCurrentNullQueueBehavior() {
        TestMaster master = new TestMaster(tlog("stop-master.bin"));
        master.forceStarted();

        assertThrows(NullPointerException.class, master::stop);
    }

    private String tlog(String name) {
        return tempDir.resolve(name).toString();
    }

    private static OAReplTLog tlog(String source, long masterSeq, long clientSeq, String methodName, Object... args) {
        return new OAReplTLog(source, new OADateTime(10_000L + masterSeq), masterSeq, clientSeq, methodName, args);
    }

    private static class TestMaster extends OAReplicationMaster {
        TestMaster(String tlogFilename) {
            super(null, tlogFilename);
        }

        void closeOutput() {
            try {
                if (objectOutputStream() != null) objectOutputStream().close();
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        void forceStarted() {
            this.bStarted = true;
            this.bStop = false;
        }

        long currentMasterSeq() {
            return getLongField("currentMasterSeq");
        }

        void setCurrentMasterSeq(long value) {
            setLongField("currentMasterSeq", value);
        }

        int totalTLogs() {
            return allTLogs().size();
        }

        @SuppressWarnings("unchecked")
        List<OAReplTLog> allTLogs() {
            try {
                java.lang.reflect.Field f = OAReplicationMaster.class.getDeclaredField("alListReplTLog");
                f.setAccessible(true);
                List<List<OAReplTLog>> lists = (List<List<OAReplTLog>>) f.get(this);
                List<OAReplTLog> all = new ArrayList<>();
                for (List<OAReplTLog> list : lists) all.addAll(list);
                return all;
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private ObjectOutputStream objectOutputStream() {
            try {
                java.lang.reflect.Field f = OAReplicationMaster.class.getDeclaredField("objectOutputStream");
                f.setAccessible(true);
                return (ObjectOutputStream) f.get(this);
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private long getLongField(String name) {
            try {
                java.lang.reflect.Field f = OAReplicationMaster.class.getDeclaredField(name);
                f.setAccessible(true);
                return f.getLong(this);
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private void setLongField(String name, long value) {
            try {
                java.lang.reflect.Field f = OAReplicationMaster.class.getDeclaredField(name);
                f.setAccessible(true);
                f.setLong(this, value);
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        protected void onNewSyncMessage(RequestInfo ri) {
            super.onNewSyncMessage(ri);
        }
    }
}
