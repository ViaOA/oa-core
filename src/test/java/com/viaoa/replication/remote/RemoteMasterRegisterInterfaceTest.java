package com.viaoa.replication.remote;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.viaoa.remote.multiplexer.annotation.*;

class RemoteMasterRegisterInterfaceTest {

    @Test
    void interfaceHasRemoteInterfaceAnnotation() {
        assertNotNull(RemoteMasterRegisterInterface.class.getAnnotation(OARemoteInterface.class));
    }

    @Test
    void registerClientHasRemoteMethodAndRemoteClientParameterAnnotations() throws Exception {
        Method method = RemoteMasterRegisterInterface.class.getMethod("registerClient", String.class,
                RemoteClientInterface.class, long.class, long.class);

        assertNotNull(method.getAnnotation(OARemoteMethod.class));
        Annotation[][] annotations = method.getParameterAnnotations();
        assertEquals(1, annotations[1].length);
        assertEquals(OARemoteParameter.class, annotations[1][0].annotationType());
    }

    @Test
    void registerClientImplementationCanReturnRemoteMaster() {
        TestRegister register = new TestRegister();
        RemoteClientInterface client = (masterSeq, methodName, args) -> { };

        RemoteMasterInterface master = register.registerClient("client-a", client, 3L, 4L);

        assertSame(register.master, master);
        assertEquals("client-a", register.guid.get());
        assertSame(client, register.client.get());
    }

    private static class TestRegister implements RemoteMasterRegisterInterface {
        final RemoteMasterInterface master = new NoOpMaster();
        final AtomicReference<String> guid = new AtomicReference<>();
        final AtomicReference<RemoteClientInterface> client = new AtomicReference<>();

        @Override
        public RemoteMasterInterface registerClient(String guid, RemoteClientInterface remoteClient, long lastSentMasterSeq,
                long lastSentClientSeq) {
            this.guid.set(guid);
            this.client.set(remoteClient);
            return master;
        }
    }

    private static class NoOpMaster implements RemoteMasterInterface {
        @Override public void processMessage(long masterSeq, long clientSeq, String methodName, Object[] args) {}
        @Override public long getLastReceivedClientSeq() { return 0; }
        @Override public long getLastProcessedClientSeq() { return 0; }
        @Override public long getLastReceivedMasterSeq() { return 0; }
        @Override public void setLastReceivedMasterSeq(long seq) {}
        @Override public long getLastProcessedMasterSeq() { return 0; }
        @Override public void setEnabled(boolean b) {}
        @Override public boolean getEnabled() { return true; }
    }
}
