package com.viaoa.sync.remote;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import com.viaoa.remote.multiplexer.annotation.OARemoteInterface;
import com.viaoa.remote.multiplexer.annotation.OARemoteMethod;

class RemoteClientCallbackInterfaceTest {

    @Test
    void interfaceIsRemoteInterface() {
        assertNotNull(RemoteClientCallbackInterface.class.getAnnotation(OARemoteInterface.class));
    }

    @Test
    void stopUsesNoReturnLowLatencyCallbackSettings() throws Exception {
        Method m = RemoteClientCallbackInterface.class.getMethod("stop", String.class, String.class);
        OARemoteMethod ann = m.getAnnotation(OARemoteMethod.class);

        assertNotNull(ann);
        assertTrue(ann.noReturnValue());
        assertEquals(2, ann.timeoutSeconds());
        assertTrue(ann.dontUseQueue());
    }

    @Test
    void pingBypassesQueue() throws Exception {
        Method m = RemoteClientCallbackInterface.class.getMethod("ping", String.class);
        OARemoteMethod ann = m.getAnnotation(OARemoteMethod.class);

        assertNotNull(ann);
        assertTrue(ann.dontUseQueue());
    }

    @Test
    void performThreadDumpBypassesQueue() throws Exception {
        Method m = RemoteClientCallbackInterface.class.getMethod("performThreadDump", String.class);
        OARemoteMethod ann = m.getAnnotation(OARemoteMethod.class);

        assertNotNull(ann);
        assertTrue(ann.dontUseQueue());
    }
}
