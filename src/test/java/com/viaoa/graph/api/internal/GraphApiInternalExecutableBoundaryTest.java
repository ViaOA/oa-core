package com.viaoa.graph.api.internal;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import com.viaoa.cascade.OACascade;
import com.viaoa.graph.api.SyncOps;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectKey;
import com.viaoa.sync.OASyncClient;
import com.viaoa.sync.OASyncServer;
import com.viaoa.sync.model.ClientInfo;
import com.viaoa.sync.remote.RemoteClientInterface;
import com.viaoa.sync.remote.RemoteServerInterface;
import com.viaoa.trigger.OATrigger;
import com.viaoa.trigger.OATriggerListener;

import org.junit.jupiter.api.Test;

class GraphApiInternalExecutableBoundaryTest {

    static class FakeSyncInternal implements SyncInternalOps {
        boolean server;
        boolean client;
        boolean running;
        boolean connected;
        int connectionId = -1;
        ClientInfo clientInfo;
        final List<String> remoteRefreshes = new ArrayList<>();
        final List<String> events = new ArrayList<>();

        @Override
        public void createServer(int port) {
            if (client || server) throw new IllegalStateException("configured");
            server = true;
        }

        @Override
        public void createClient(String hostName, int serverPort) {
            if (client || server) throw new IllegalStateException("configured");
            client = true;
        }

        @Override
        public void start() {
            if (!server && !client) throw new IllegalStateException("not configured");
            running = true;
            connected = client;
            connectionId = client ? 1 : 0;
        }

        @Override
        public void stop() {
            running = false;
            connected = false;
            connectionId = -1;
        }

        @Override
        public boolean isSingleUser() {
            return !server && !client;
        }

        @Override
        public boolean isServer() {
            return server;
        }

        @Override
        public boolean isClient() {
            return client;
        }

        @Override
        public boolean isRunning() {
            return running;
        }

        @Override
        public OASyncClient getClient() {
            return null;
        }

        @Override
        public OASyncServer getServer() {
            return null;
        }

        @Override
        public boolean isConnected() {
            return connected;
        }

        @Override
        public int getConnectionId() {
            return connectionId;
        }

        @Override
        public void sendException(String msg, Throwable ex) {
            events.add("exception:" + msg + ":" + (ex == null ? "null" : ex.getClass().getSimpleName()));
        }

        @Override
        public ClientInfo getClientInfo() {
            return clientInfo;
        }

        @Override
        public void updateClientInfo(ClientInfo ci) {
            clientInfo = ci;
        }

        @Override
        public void saveCache(OACascade cascade, int iCascadeRule) {
            events.add("saveCache:" + iCascadeRule);
        }

        @Override
        public void performDGC() {
            events.add("dgc");
        }

        @Override
        public void callRemoteClientRefresh(Class<? extends OAObject> class1, OAObjectKey objectKey) {
            remoteRefreshes.add(class1.getSimpleName() + ":" + objectKey);
        }

        @Override
        public void callRemoteClientRefresh(Class<? extends OAObject> class1, OAObjectKey objectKey, String linkPropertyName) {
            remoteRefreshes.add(class1.getSimpleName() + ":" + objectKey + ":" + linkPropertyName);
        }

        @Override
        public RemoteClientInterface getRemoteClient() {
            return null;
        }

        @Override
        public RemoteServerInterface getRemoteServer() {
            return null;
        }
    }

    static class FakeReplInternal implements ReplInternalOps {
        boolean master;
        boolean client;

        @Override
        public boolean isMaster() {
            return master;
        }

        @Override
        public boolean isClient() {
            return client;
        }
    }

    static class FakeTriggerInternal implements TriggerInternalOps {
        final List<OATrigger> triggers = new ArrayList<>();
        int runCount;
        boolean failRun;

        @Override
        public void addTrigger(OATrigger trigger) {
            addTrigger(trigger, false);
        }

        @Override
        public void addTrigger(OATrigger trigger, boolean bSkipFirstNonManyProperty) {
            if (trigger == null) throw new IllegalArgumentException("trigger");
            triggers.add(trigger);
        }

        @Override
        public boolean removeTrigger(OATrigger trigger) {
            return triggers.remove(trigger);
        }

        @Override
        public void runTrigger(Runnable r) {
            if (r == null) throw new IllegalArgumentException("runnable");
            if (failRun) throw new RuntimeException("run failed");
            r.run();
            runCount++;
        }
    }

    static class Item extends OAObject {
    }

    @Test
    void syncInternalImplementsPublicSyncOpsContract() {
        FakeSyncInternal sync = new FakeSyncInternal();

        assertInstanceOf(SyncOps.class, sync);
        assertTrue(sync.isSingleUser());

        sync.createClient("localhost", 1099);
        sync.start();

        assertTrue(sync.isClient());
        assertTrue(sync.isRunning());
        assertTrue(sync.isConnected());
        assertEquals(1, sync.getConnectionId());

        sync.stop();

        assertFalse(sync.isRunning());
        assertFalse(sync.isConnected());
        assertEquals(-1, sync.getConnectionId());
    }

    @Test
    void syncInternalServerLifecycleKeepsConnectionBoundaryDistinct() {
        FakeSyncInternal sync = new FakeSyncInternal();

        sync.createServer(1099);
        sync.start();

        assertTrue(sync.isServer());
        assertFalse(sync.isClient());
        assertTrue(sync.isRunning());
        assertFalse(sync.isConnected(), "server running is not the same as client connection");
        assertEquals(0, sync.getConnectionId());
    }

    @Test
    void syncInternalDiagnosticsAndMaintenanceAreVisible() {
        FakeSyncInternal sync = new FakeSyncInternal();

        sync.sendException("x", new IllegalStateException());
        sync.saveCache(new OACascade(), 7);
        sync.performDGC();

        assertEquals(List.of("exception:x:IllegalStateException", "saveCache:7", "dgc"), sync.events);
    }

    @Test
    void syncInternalRemoteRefreshCallsAreDistinct() {
        FakeSyncInternal sync = new FakeSyncInternal();
        OAObjectKey key = new OAObjectKey(1);

        sync.callRemoteClientRefresh(Item.class, key);
        sync.callRemoteClientRefresh(Item.class, key, "children");

        assertEquals(2, sync.remoteRefreshes.size());
        assertTrue(sync.remoteRefreshes.get(0).contains("Item"));
        assertTrue(sync.remoteRefreshes.get(1).endsWith(":children"));
    }

    @Test
    void replInternalRoleQueriesAreExplicitSeparateFromPublicEmptyReplOps() {
        FakeReplInternal repl = new FakeReplInternal();

        assertFalse(repl.isMaster());
        assertFalse(repl.isClient());

        repl.master = true;

        assertTrue(repl.isMaster());
        assertFalse(repl.isClient());

        repl.master = false;
        repl.client = true;

        assertFalse(repl.isMaster());
        assertTrue(repl.isClient());
    }

    @Test
    void triggerInternalRunTriggerExecutesRunnableAndCountsOnlyCompletedRuns() {
        FakeTriggerInternal trigger = new FakeTriggerInternal();
        final int[] count = new int[1];

        trigger.runTrigger(() -> count[0]++);

        assertEquals(1, count[0]);
        assertEquals(1, trigger.runCount);
    }

    @Test
    void triggerInternalRunTriggerRejectsNullRunnable() {
        FakeTriggerInternal trigger = new FakeTriggerInternal();

        assertThrows(IllegalArgumentException.class, () -> trigger.runTrigger(null));
        assertEquals(0, trigger.runCount);
    }

    @Test
    void triggerInternalRunTriggerFailureDoesNotCountAsCompleted() {
        FakeTriggerInternal trigger = new FakeTriggerInternal();
        trigger.failRun = true;

        RuntimeException ex = assertThrows(RuntimeException.class, () -> trigger.runTrigger(() -> fail("should not run")));

        assertEquals("run failed", ex.getMessage());
        assertEquals(0, trigger.runCount);
    }

    @Test
    void triggerInternalStillSupportsPublicTriggerRegistration() {
        FakeTriggerInternal trigger = new FakeTriggerInternal();
        
        /**
         
               qqqqqqqqqqqqqqqqqqqqqqqqqqqqq
         *      
		String name,
        Class rootClass,
        OATriggerListener triggerListener,
        String[] propertyPaths, 
        final boolean bOnlyUseLoadedData, 
        final boolean bServerSideOnly, 
        final boolean bUseBackgroundThread,
        final boolean bUseBackgroundThreadIfNeeded)
        
         * Creates a trigger that monitors one or more property paths relative to a
         * given root class and invokes the supplied listener when events occur.
         *
         * @param name                         the trigger name
         * @param rootClass                    the root class from which property paths are evaluated
         * @param triggerListener              the listener to invoke when the trigger fires
         * @param propertyPaths                the property paths that this trigger depends on
         * @param bOnlyUseLoadedData           true to restrict evaluation to already-loaded data
         * @param bServerSideOnly              true to limit execution to the server
         * @param bUseBackgroundThread         true to execute the trigger in a background thread
         * @param bUseBackgroundThreadIfNeeded true to run in a background thread only when required
         */
        
        OATrigger t = new OATrigger("test", Item.class, null, false, null);

        trigger.addTrigger(t, true);

        assertEquals(1, trigger.triggers.size());
        assertTrue(trigger.removeTrigger(t));
        assertFalse(trigger.removeTrigger(t));
    }
}
