package com.viaoa.graph.api;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import com.viaoa.trigger.OATrigger;

import org.junit.jupiter.api.Test;

class GraphApiExecutableContractTest {

    static class FakeSyncOps implements SyncOps {
        enum Role { SINGLE, SERVER, CLIENT }

        Role role = Role.SINGLE;
        boolean running;
        int serverPort;
        String host;
        int clientPort;
        boolean failStart;
        boolean failStop;
        final List<String> events = new ArrayList<>();

        @Override
        public void createServer(int port) {
            if (running) throw new IllegalStateException("running");
            if (role != Role.SINGLE) throw new IllegalStateException("already configured");
            if (port <= 0) throw new IllegalArgumentException("invalid port");
            role = Role.SERVER;
            serverPort = port;
            events.add("createServer");
        }

        @Override
        public void createClient(String hostName, int serverPort) {
            if (running) throw new IllegalStateException("running");
            if (role != Role.SINGLE) throw new IllegalStateException("already configured");
            if (hostName == null || hostName.isBlank()) throw new IllegalArgumentException("host");
            if (serverPort <= 0) throw new IllegalArgumentException("port");
            role = Role.CLIENT;
            host = hostName;
            clientPort = serverPort;
            events.add("createClient");
        }

        @Override
        public void start() throws Exception {
            if (running) throw new IllegalStateException("already running");
            if (role == Role.SINGLE) throw new IllegalStateException("not configured");
            if (failStart) throw new Exception("start failed");
            running = true;
            events.add("start");
        }

        @Override
        public void stop() throws Exception {
            if (!running) throw new IllegalStateException("not running");
            if (failStop) throw new Exception("stop failed");
            running = false;
            events.add("stop");
        }

        @Override
        public boolean isSingleUser() {
            return role == Role.SINGLE;
        }

        @Override
        public boolean isServer() {
            return role == Role.SERVER;
        }

        @Override
        public boolean isClient() {
            return role == Role.CLIENT;
        }

        @Override
        public boolean isRunning() {
            return running;
        }
    }

    static class FakeTriggerOps implements TriggerOps {
        final List<OATrigger> triggers = new ArrayList<>();
        boolean failAdd;
        boolean failRemove;

        @Override
        public void addTrigger(OATrigger trigger) {
            addTrigger(trigger, false);
        }

        @Override
        public void addTrigger(OATrigger trigger, boolean bSkipFirstNonManyProperty) {
            if (trigger == null) throw new IllegalArgumentException("trigger");
            if (failAdd) throw new RuntimeException("add failed");
            if (!triggers.contains(trigger)) {
                triggers.add(trigger);
            }
        }

        @Override
        public boolean removeTrigger(OATrigger trigger) {
            if (trigger == null) return false;
            if (failRemove) throw new RuntimeException("remove failed");
            return triggers.remove(trigger);
        }
    }

    @Test
    void syncStartsSingleUserThenConfiguresServerThenRuns() throws Exception {
        FakeSyncOps sync = new FakeSyncOps();

        assertTrue(sync.isSingleUser());
        assertFalse(sync.isServer());
        assertFalse(sync.isClient());
        assertFalse(sync.isRunning());

        sync.createServer(1099);

        assertFalse(sync.isSingleUser());
        assertTrue(sync.isServer());
        assertFalse(sync.isClient());
        assertFalse(sync.isRunning());

        sync.start();

        assertTrue(sync.isRunning());
        assertEquals(List.of("createServer", "start"), sync.events);
    }

    @Test
    void syncClientRoleLifecycleIsDeterministic() throws Exception {
        FakeSyncOps sync = new FakeSyncOps();

        sync.createClient("localhost", 1100);

        assertFalse(sync.isSingleUser());
        assertFalse(sync.isServer());
        assertTrue(sync.isClient());
        assertFalse(sync.isRunning());

        sync.start();
        assertTrue(sync.isRunning());

        sync.stop();
        assertFalse(sync.isRunning());
        assertTrue(sync.isClient(), "stop must not erase configured role");
    }

    @Test
    void syncStartWithoutRoleFailsWithoutChangingState() {
        FakeSyncOps sync = new FakeSyncOps();

        Exception ex = assertThrows(Exception.class, sync::start);

        assertEquals("not configured", ex.getMessage());
        assertTrue(sync.isSingleUser());
        assertFalse(sync.isRunning());
    }

    @Test
    void syncConflictingRoleCreationFailsVisibly() {
        FakeSyncOps sync = new FakeSyncOps();

        sync.createServer(1099);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> sync.createClient("localhost", 1100));

        assertEquals("already configured", ex.getMessage());
        assertTrue(sync.isServer());
        assertFalse(sync.isClient());
    }

    @Test
    void syncDuplicateStartFailsWithoutChangingRunningState() throws Exception {
        FakeSyncOps sync = new FakeSyncOps();

        sync.createServer(1099);
        sync.start();

        RuntimeException ex = assertThrows(RuntimeException.class, sync::start);

        assertEquals("already running", ex.getMessage());
        assertTrue(sync.isRunning());
    }

    @Test
    void syncFailedStartLeavesConfiguredButNotRunning() {
        FakeSyncOps sync = new FakeSyncOps();
        sync.createServer(1099);
        sync.failStart = true;

        Exception ex = assertThrows(Exception.class, sync::start);

        assertEquals("start failed", ex.getMessage());
        assertTrue(sync.isServer());
        assertFalse(sync.isRunning());
    }

    @Test
    void syncFailedStopLeavesRunningStateVisible() throws Exception {
        FakeSyncOps sync = new FakeSyncOps();
        sync.createClient("localhost", 1100);
        sync.start();
        sync.failStop = true;

        Exception ex = assertThrows(Exception.class, sync::stop);

        assertEquals("stop failed", ex.getMessage());
        assertTrue(sync.isRunning(), "failed stop must not pretend stopped");
        assertTrue(sync.isClient());
    }

    @Test
    void syncInvalidConfigurationFailsWithoutRoleMutation() {
        FakeSyncOps sync = new FakeSyncOps();

        assertThrows(IllegalArgumentException.class, () -> sync.createServer(0));
        assertThrows(IllegalArgumentException.class, () -> sync.createClient("", 100));
        assertThrows(IllegalArgumentException.class, () -> sync.createClient("localhost", 0));

        assertTrue(sync.isSingleUser());
        assertFalse(sync.isRunning());
    }

    @Test
    void triggerAddRemoveLifecycleIsVisibleAndDeterministic() {
        FakeTriggerOps ops = new FakeTriggerOps();
        OATrigger trigger = new OATrigger("test", String.class, null, false, null);

        ops.addTrigger(trigger);

        assertEquals(1, ops.triggers.size());
        assertTrue(ops.triggers.contains(trigger));

        assertTrue(ops.removeTrigger(trigger));
        assertFalse(ops.removeTrigger(trigger));
        assertTrue(ops.triggers.isEmpty());
    }

    @Test
    void triggerDuplicateAddDoesNotRegisterTwice() {
        FakeTriggerOps ops = new FakeTriggerOps();
        OATrigger trigger = new OATrigger("test", String.class, null, false, null);

        ops.addTrigger(trigger);
        ops.addTrigger(trigger, true);

        assertEquals(1, ops.triggers.size());
    }

    @Test
    void triggerNullAddFailsAndNullRemoveReturnsFalse() {
        FakeTriggerOps ops = new FakeTriggerOps();

        assertThrows(IllegalArgumentException.class, () -> ops.addTrigger(null));
        assertFalse(ops.removeTrigger(null));
        assertTrue(ops.triggers.isEmpty());
    }

    @Test
    void triggerFailedAddDoesNotPartiallyRegister() {
        FakeTriggerOps ops = new FakeTriggerOps();
        OATrigger trigger = new OATrigger("test", String.class, null, false, null);
        ops.failAdd = true;

        RuntimeException ex = assertThrows(RuntimeException.class, () -> ops.addTrigger(trigger));

        assertEquals("add failed", ex.getMessage());
        assertFalse(ops.triggers.contains(trigger));
    }

    @Test
    void triggerFailedRemoveDoesNotSilentlyReportSuccess() {
        FakeTriggerOps ops = new FakeTriggerOps();
        OATrigger trigger = new OATrigger("test", String.class, null, false, null);
        ops.addTrigger(trigger);
        ops.failRemove = true;

        RuntimeException ex = assertThrows(RuntimeException.class, () -> ops.removeTrigger(trigger));

        assertEquals("remove failed", ex.getMessage());
        assertTrue(ops.triggers.contains(trigger));
    }

    @Test
    void replOpsHasNoExecutableLifecycleYet() {
        ReplOps repl = new ReplOps() {
        };

        assertEquals(0, repl.getClass().getInterfaces()[0].getDeclaredMethods().length);
    }
}
