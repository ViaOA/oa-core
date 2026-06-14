package com.viaoa.replication.remote;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.*;

import org.junit.jupiter.api.Test;

import com.viaoa.remote.multiplexer.annotation.*;

class RemoteClientInterfaceTest {

    @Test
    void interfaceHasRemoteInterfaceAnnotation() {
        assertNotNull(RemoteClientInterface.class.getAnnotation(OARemoteInterface.class));
    }

    @Test
    void processMessageHasRemoteMethodAnnotationAndCanBeImplemented() throws Exception {
        Method method = RemoteClientInterface.class.getMethod("processMessage", long.class, String.class, Object[].class);
        assertNotNull(method.getAnnotation(OARemoteMethod.class));

        TestClient client = new TestClient();
        Object[] args = { "arg" };
        client.processMessage(9L, "refresh", args);

        assertEquals(9L, client.masterSeq.get());
        assertEquals("refresh", client.methodName.get());
        assertSame(args, client.args.get());
    }

    private static class TestClient implements RemoteClientInterface {
        final AtomicLong masterSeq = new AtomicLong();
        final AtomicReference<String> methodName = new AtomicReference<>();
        final AtomicReference<Object[]> args = new AtomicReference<>();

        @Override
        public void processMessage(long masterSeq, String methodName, Object[] args) {
            this.masterSeq.set(masterSeq);
            this.methodName.set(methodName);
            this.args.set(args);
        }
    }
}
