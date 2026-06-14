package com.viaoa.remote.info;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import com.viaoa.remote.multiplexer.annotation.OARemoteInterface;
import com.viaoa.remote.multiplexer.annotation.OARemoteMethod;
import com.viaoa.remote.multiplexer.annotation.OARemoteParameter;

class BindInfoTest {

    @Test
    void constructorStoresNameObjectInterfaceBroadcastAndQueueState() {
        RemoteImpl impl = new RemoteImpl();
        BindInfo bind = new BindInfo("name", impl, Remote.class, null, true, "queue", 25);

        assertEquals("name", bind.name);
        assertSame(impl, bind.getObject());
        assertSame(Remote.class, bind.interfaceClass);
        assertTrue(bind.isBroadcast);
        assertTrue(bind.usesQueue);
        assertEquals("queue", bind.asyncQueueName);
        assertEquals(25, bind.asyncQueueSize);
        assertFalse(bind.isOASync);
    }

    @Test
    void constructorReadsOASyncFromRemoteInterfaceAnnotation() {
        BindInfo bind = new BindInfo("sync", new SyncRemoteImpl(), SyncRemote.class, null, false, null, 0);

        assertTrue(bind.isOASync);
    }

    @Test
    void setObjectUsesPlainWeakReferenceWhenQueueIsNull() {
        BindInfo bind = new BindInfo("name", null, Remote.class, null, false, null, 0);
        Object obj = new Object();

        bind.setObject(obj, null);

        assertSame(obj, bind.getObject());
        assertNotNull(bind.weakRef);
    }

    @Test
    void setObjectUsesReferenceQueueWhenSupplied() {
        BindInfo bind = new BindInfo("name", null, Remote.class, null, false, null, 0);
        ReferenceQueue<Object> queue = new ReferenceQueue<>();
        Object obj = new Object();

        bind.setObject(obj, queue);

        assertSame(obj, bind.getObject());
        assertInstanceOf(WeakReference.class, bind.weakRef);
    }

    @Test
    void getObjectReturnsNullWhenNoObjectHasBeenAssigned() {
        BindInfo bind = new BindInfo("name", null, Remote.class, null, false, null, 0);

        assertNull(bind.getObject());
    }

    @Test
    void getMethodInfoBySignatureLoadsAnnotatedMethodMetadata() throws Exception {
        BindInfo bind = new BindInfo("name", new RemoteImpl(), Remote.class, null, false, null, 0);
        Method method = Remote.class.getMethod("annotated", String.class, RemoteParam.class);

        MethodInfo info = bind.getMethodInfo(method);
        MethodInfo same = bind.getMethodInfo(info.methodNameSignature);

        assertSame(info, same);
        assertEquals(method, info.method);
        assertTrue(info.compressedReturn);
        assertTrue(info.noReturnValue);
        assertEquals(3, info.timeoutSeconds);
        assertTrue(info.dontUseQueue);
        assertTrue(info.dontUseQueueForReturnValue);
        assertTrue(info.returnOnQueueSocket);
        assertTrue(info.runInRemoteThread);
        assertArrayEquals(new boolean[] { true, false }, info.compressedParams);
        assertArrayEquals(new boolean[] { true, false }, info.dontUseQueues);
        assertArrayEquals(new Class[] { null, RemoteParam.class }, info.remoteParams);
    }

    @Test
    void getMethodInfoByMethodReturnsNullForUnknownMethod() throws Exception {
        BindInfo bind = new BindInfo("name", new RemoteImpl(), Remote.class, null, false, null, 0);
        Method method = Object.class.getMethod("toString");

        assertNull(bind.getMethodInfo(method));
    }

    @Test
    void loadMethodInfoIsIdempotent() throws Exception {
        BindInfo bind = new BindInfo("name", new RemoteImpl(), Remote.class, null, false, null, 0);
        Method method = Remote.class.getMethod("simple");

        bind.loadMethodInfo();
        MethodInfo info1 = bind.getMethodInfo(method);
        bind.loadMethodInfo();
        MethodInfo info2 = bind.getMethodInfo(method);

        assertNotNull(info1);
        assertNotNull(info2);
        assertNotSame(info1, info2);
        assertEquals(info1.methodNameSignature, info2.methodNameSignature);
    }

    @Retention(RetentionPolicy.RUNTIME)
    private @interface OtherAnnotation {
    }

    @OARemoteInterface
    private interface RemoteParam {
    }

    @OARemoteInterface
    private interface Remote {
        void simple();

        @OARemoteMethod(compressedReturnValue = true, noReturnValue = true, timeoutSeconds = 3,
                dontUseQueueForReturnValue = true, dontUseQueue = true, returnOnQueueSocket = true,
                runInRemoteThread = true)
        String annotated(@OARemoteParameter(compressed = true, dontUseQueue = true) String value, RemoteParam remoteParam);
    }

    @OARemoteInterface(isOASync = true)
    private interface SyncRemote {
        void call();
    }

    private static class RemoteImpl implements Remote {
        @Override
        public void simple() {
        }

        @Override
        public String annotated(String value, RemoteParam remoteParam) {
            return value;
        }
    }

    private static class SyncRemoteImpl implements SyncRemote {
        @Override
        public void call() {
        }
    }
}
