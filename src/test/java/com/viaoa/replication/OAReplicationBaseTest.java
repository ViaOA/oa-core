package com.viaoa.replication;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import com.viaoa.remote.info.RequestInfo;
import com.viaoa.sync.remote.RemoteSyncInterface;

class OAReplicationBaseTest {

    @Test
    void constructorInitializesQueuePositionToZero() {
        TestReplication repl = new TestReplication();

        assertEquals(0L, repl.getCirularQueuePos());
    }

    @Test
    void getMethodReturnsRemoteSyncMethodAndCachesLookup() throws Exception {
        TestReplication repl = new TestReplication();

        Method method = repl.findMethod("refresh");

        assertNotNull(method);
        assertEquals("refresh", method.getName());
        assertSame(RemoteSyncInterface.class, method.getDeclaringClass());
        assertSame(method, repl.findMethod("refresh"));
    }

    @Test
    void getMethodReturnsNullForUnknownName() {
        TestReplication repl = new TestReplication();

        assertNull(repl.findMethod("missingMethod"));
    }

    private static class TestReplication extends OAReplicationBase {
        TestReplication() {
            super(null);
        }

        Method findMethod(String name) {
            return getMethod(name);
        }

        @Override
        protected void onNewSyncMessage(RequestInfo ri) {
        }
    }
}
