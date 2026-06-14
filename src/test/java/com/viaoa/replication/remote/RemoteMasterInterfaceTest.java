package com.viaoa.replication.remote;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import com.viaoa.remote.multiplexer.annotation.*;

class RemoteMasterInterfaceTest {

    @Test
    void interfaceHasRemoteInterfaceAnnotation() {
        assertNotNull(RemoteMasterInterface.class.getAnnotation(OARemoteInterface.class));
    }

    @Test
    void processMessageHasRemoteMethodAnnotation() throws Exception {
        Method method = RemoteMasterInterface.class.getMethod("processMessage", long.class, long.class, String.class, Object[].class);
        assertNotNull(method.getAnnotation(OARemoteMethod.class));
    }

    @Test
    void getLastReceivedClientSeqHasRemoteMethodAnnotation() throws Exception {
        assertNotNull(RemoteMasterInterface.class.getMethod("getLastReceivedClientSeq").getAnnotation(OARemoteMethod.class));
    }

    @Test
    void getLastProcessedClientSeqHasRemoteMethodAnnotation() throws Exception {
        assertNotNull(RemoteMasterInterface.class.getMethod("getLastProcessedClientSeq").getAnnotation(OARemoteMethod.class));
    }

    @Test
    void getLastReceivedMasterSeqHasRemoteMethodAnnotation() throws Exception {
        assertNotNull(RemoteMasterInterface.class.getMethod("getLastReceivedMasterSeq").getAnnotation(OARemoteMethod.class));
    }

    @Test
    void setLastReceivedMasterSeqHasRemoteMethodAnnotation() throws Exception {
        assertNotNull(RemoteMasterInterface.class.getMethod("setLastReceivedMasterSeq", long.class).getAnnotation(OARemoteMethod.class));
    }

    @Test
    void getLastProcessedMasterSeqHasRemoteMethodAnnotation() throws Exception {
        assertNotNull(RemoteMasterInterface.class.getMethod("getLastProcessedMasterSeq").getAnnotation(OARemoteMethod.class));
    }

    @Test
    void setEnabledHasRemoteMethodAnnotation() throws Exception {
        assertNotNull(RemoteMasterInterface.class.getMethod("setEnabled", boolean.class).getAnnotation(OARemoteMethod.class));
    }

    @Test
    void getEnabledHasRemoteMethodAnnotationAndImplementationCanTrackState() {
        TestMaster master = new TestMaster();

        master.setEnabled(false);

        assertFalse(master.getEnabled());
    }

    private static class TestMaster implements RemoteMasterInterface {
        boolean enabled = true;
        long lastReceivedMasterSeq;

        @Override public void processMessage(long masterSeq, long clientSeq, String methodName, Object[] args) {}
        @Override public long getLastReceivedClientSeq() { return 11L; }
        @Override public long getLastProcessedClientSeq() { return 12L; }
        @Override public long getLastReceivedMasterSeq() { return lastReceivedMasterSeq; }
        @Override public void setLastReceivedMasterSeq(long seq) { lastReceivedMasterSeq = seq; }
        @Override public long getLastProcessedMasterSeq() { return 13L; }
        @Override public void setEnabled(boolean b) { enabled = b; }
        @Override public boolean getEnabled() { return enabled; }
    }
}
