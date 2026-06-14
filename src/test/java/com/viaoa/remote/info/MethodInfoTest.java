package com.viaoa.remote.info;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

class MethodInfoTest {

    @Test
    void constructorLeavesFieldsAtDefaults() {
        MethodInfo info = new MethodInfo();

        assertNull(info.method);
        assertNull(info.methodNameSignature);
        assertNull(info.remoteReturn);
        assertFalse(info.compressedReturn);
        assertNull(info.remoteParams);
        assertNull(info.compressedParams);
        assertNull(info.dontUseQueues);
        assertFalse(info.noReturnValue);
        assertFalse(info.dontUseQueueForReturnValue);
        assertFalse(info.returnOnQueueSocket);
        assertFalse(info.dontUseQueue);
        assertEquals(0, info.timeoutSeconds);
        assertFalse(info.runInRemoteThread);
    }

    @Test
    void fieldsCanStoreMethodMetadata() throws Exception {
        Method method = Remote.class.getMethod("call", String.class);
        MethodInfo info = new MethodInfo();

        info.method = method;
        info.methodNameSignature = "call1";
        info.remoteReturn = RemoteReturn.class;
        info.compressedReturn = true;
        info.remoteParams = new Class[] { String.class };
        info.compressedParams = new boolean[] { true };
        info.dontUseQueues = new boolean[] { true };
        info.noReturnValue = true;
        info.dontUseQueueForReturnValue = true;
        info.returnOnQueueSocket = true;
        info.dontUseQueue = true;
        info.timeoutSeconds = 9;
        info.runInRemoteThread = true;

        assertSame(method, info.method);
        assertEquals("call1", info.methodNameSignature);
        assertSame(RemoteReturn.class, info.remoteReturn);
        assertTrue(info.compressedReturn);
        assertArrayEquals(new Class[] { String.class }, info.remoteParams);
        assertArrayEquals(new boolean[] { true }, info.compressedParams);
        assertArrayEquals(new boolean[] { true }, info.dontUseQueues);
        assertTrue(info.noReturnValue);
        assertTrue(info.dontUseQueueForReturnValue);
        assertTrue(info.returnOnQueueSocket);
        assertTrue(info.dontUseQueue);
        assertEquals(9, info.timeoutSeconds);
        assertTrue(info.runInRemoteThread);
    }

    private interface Remote {
        void call(String value);
    }

    private interface RemoteReturn {
    }
}
