package com.viaoa.sync.remote;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.viaoa.object.OAObjectKey;
import com.viaoa.remote.multiplexer.annotation.OARemoteInterface;
import com.viaoa.remote.multiplexer.annotation.OARemoteMethod;

class RemoteSyncInterfaceTest {

    @Test
    void interfaceIsRemoteSyncInterface() {
        OARemoteInterface ann = RemoteSyncInterface.class.getAnnotation(OARemoteInterface.class);

        assertNotNull(ann);
        assertTrue(ann.isOASync());
    }

    @Test
    void regularSyncMethodsUseInterfaceLevelQueueSettings() throws Exception {
        assertNull(RemoteSyncInterface.class.getMethod("propertyChange", Class.class, OAObjectKey.class, String.class, Object.class, boolean.class).getAnnotation(OARemoteMethod.class));
        assertNull(RemoteSyncInterface.class.getMethod("addToHub", Class.class, OAObjectKey.class, String.class, Object.class).getAnnotation(OARemoteMethod.class));
        assertNull(RemoteSyncInterface.class.getMethod("removeAllFromHub", Class.class, OAObjectKey.class, String.class).getAnnotation(OARemoteMethod.class));
    }

    @Test
    void serverDeleteRunsInRemoteThread() throws Exception {
        OARemoteMethod ann = RemoteSyncInterface.class.getMethod("serverDelete", Class.class, OAObjectKey.class).getAnnotation(OARemoteMethod.class);

        assertNotNull(ann);
        assertTrue(ann.runInRemoteThread());
    }
}
