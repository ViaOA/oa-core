package com.viaoa.runtime.thread;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.test.pos.model.oa.Register;

class OAThreadLocalTest {
    @Test
    void scalarAccessorsAndCountersRoundTrip() {
        OAThreadLocal local = new OAThreadLocal();
        Register register = new Register();
        Object[] array = new Object[] { register };

        assertNotNull(local.getThreadName());
        local.setThreadName("worker");
        local.setTime(123L);
        local.setLoading(true);
        local.setCacheAddMode(7);
        local.setSendSyncMessages(false);
        local.setSendSyncMessagesHold(true);
        local.setDeleting(array);
        local.setFlags(array);
        local.setLocks(array);
        local.setWaitingOnLock(true);
        local.setCompoundUndoableName("compound");
        local.setCreateUndoablePropertyChanges(true);
        local.setStatus("busy");
        local.setNotifyObject(register);
        local.setIgnoreTreeListenerProperty(Register.P_Code);
        local.setRefreshing(2);
        local.replicationSource = "source";

        assertEquals("worker", local.getThreadName());
        assertEquals(123L, local.getTime());
        assertTrue(local.getLoading());
        assertEquals(7, local.getCacheAddMode());
        assertFalse(local.getSendSyncMessages());
        assertTrue(local.getSendSyncMessagesHold());
        assertSame(array, local.getDeleting());
        assertSame(array, local.getFlags());
        assertSame(array, local.getLocks());
        assertTrue(local.getWaitingOnLock());
        assertEquals("compound", local.getCompoundUndoableName());
        assertTrue(local.isCreateUndoablePropertyChanges());
        assertEquals("busy", local.getStatus());
        assertSame(register, local.getNotifyObject());
        assertEquals(Register.P_Code, local.getIgnoreTreeListenerProperty());
        assertEquals(2, local.getRefreshing());
        assertEquals("source", local.replicationSource);

        assertEquals(1, local.incStartServerOnly());
        assertEquals(0, local.decStartServerOnly());
        assertEquals(1, local.incHubMergerChangingCount());
        assertEquals(0, local.decHubMergerChangingCount());
        assertEquals(1, local.incRecursiveTriggerCount());
        assertEquals(0, local.decRecursiveTriggerCount());
        assertEquals(1, local.incHubListenerTreeCount());
        assertEquals(0, local.decHubListenerTreeCount());
    }
}
