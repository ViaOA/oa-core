package com.viaoa.sync.remote;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectKey;
import com.viaoa.remote.multiplexer.annotation.OARemoteInterface;
import com.viaoa.remote.multiplexer.annotation.OARemoteMethod;
import com.viaoa.sync.model.ClientInfo;

class RemoteSessionInterfaceTest {

    @Test
    void interfaceIsRemoteInterface() {
        assertNotNull(RemoteSessionInterface.class.getAnnotation(OARemoteInterface.class));
    }

    @Test
    void objectLifecycleMethodsAreNoReturnAndBypassQueue() throws Exception {
        assertNoReturnDontUseQueue("objectCreated", UUID.class);
        assertNoReturnDontUseQueue("objectsFinalized", UUID[].class);
        assertNoReturnDontUseQueue("updateObjectsWithoutHubs", Class.class, OAObjectKey.class, boolean.class);
    }

    @Test
    void lockMethodsUseDefaultRemoteSettings() throws Exception {
        assertNull(RemoteSessionInterface.class.getMethod("setLock", Class.class, OAObjectKey.class, boolean.class).getAnnotation(OARemoteMethod.class));
        assertNull(RemoteSessionInterface.class.getMethod("isLocked", Class.class, OAObjectKey.class).getAnnotation(OARemoteMethod.class));
        assertNull(RemoteSessionInterface.class.getMethod("isLockedByAnotherClient", Class.class, OAObjectKey.class).getAnnotation(OARemoteMethod.class));
        assertNull(RemoteSessionInterface.class.getMethod("isLockedByThisClient", Class.class, OAObjectKey.class).getAnnotation(OARemoteMethod.class));
    }

    @Test
    void updateAndSendExceptionAreNoReturnAndBypassQueue() throws Exception {
        assertNoReturnDontUseQueue("update", ClientInfo.class);
        assertNoReturnDontUseQueue("sendException", String.class, Throwable.class);
    }

    @Test
    void pingBypassesQueueAndPing2HasNoReturn() throws Exception {
        OARemoteMethod ping = RemoteSessionInterface.class.getMethod("ping", String.class).getAnnotation(OARemoteMethod.class);
        assertNotNull(ping);
        assertTrue(ping.dontUseQueue());

        assertNoReturnDontUseQueue("ping2", String.class);
    }

    private void assertNoReturnDontUseQueue(String name, Class<?>... types) throws Exception {
        OARemoteMethod ann = RemoteSessionInterface.class.getMethod(name, types).getAnnotation(OARemoteMethod.class);
        assertNotNull(ann, name + " should have OARemoteMethod");
        assertTrue(ann.noReturnValue());
        assertTrue(ann.dontUseQueue());
    }
}
