package com.viaoa.sync.remote;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectKey;
import com.viaoa.remote.multiplexer.annotation.OARemoteInterface;
import com.viaoa.remote.multiplexer.annotation.OARemoteMethod;

class RemoteClientInterfaceTest {

    @Test
    void interfaceIsRemoteInterface() {
        assertNotNull(RemoteClientInterface.class.getAnnotation(OARemoteInterface.class));
    }

    @Test
    void createCopyReturnsOnQueueSocket() throws Exception {
        Method m = RemoteClientInterface.class.getMethod("createCopy", Class.class, OAObjectKey.class, String[].class);
        OARemoteMethod ann = m.getAnnotation(OARemoteMethod.class);

        assertNotNull(ann);
        assertTrue(ann.returnOnQueueSocket());
    }

    @Test
    void getDetailNowReturnsOnQueueSocket() throws Exception {
        Method m = RemoteClientInterface.class.getMethod("getDetailNow", int.class, Class.class, OAObjectKey.class, String.class, String[].class, OAObjectKey[].class, boolean.class);
        OARemoteMethod ann = m.getAnnotation(OARemoteMethod.class);

        assertNotNull(ann);
        assertTrue(ann.returnOnQueueSocket());
    }

    @Test
    void datasourceImmediateReturnUsesQueueSocket() throws Exception {
        Method m = RemoteClientInterface.class.getMethod("datasource", int.class, Object[].class);
        OARemoteMethod ann = m.getAnnotation(OARemoteMethod.class);

        assertNotNull(ann);
        assertTrue(ann.returnOnQueueSocket());
    }

    @Test
    void datasourceReturnOnQueueUsesDefaultQueueBehavior() throws Exception {
        Method m = RemoteClientInterface.class.getMethod("datasourceReturnOnQueue", int.class, Object[].class);
        OARemoteMethod ann = m.getAnnotation(OARemoteMethod.class);

        assertNotNull(ann);
        assertFalse(ann.returnOnQueueSocket());
        assertFalse(ann.noReturnValue());
    }

    @Test
    void datasourceNoReturnHasNoReturnValue() throws Exception {
        Method m = RemoteClientInterface.class.getMethod("datasourceNoReturn", int.class, Object[].class);
        OARemoteMethod ann = m.getAnnotation(OARemoteMethod.class);

        assertNotNull(ann);
        assertTrue(ann.noReturnValue());
    }

    @Test
    void unannotatedStateChangingMethodsUseDefaultRemoteSettings() throws Exception {
        assertNull(RemoteClientInterface.class.getMethod("deleteAll", Class.class, OAObjectKey.class, String.class).getAnnotation(OARemoteMethod.class));
        assertNull(RemoteClientInterface.class.getMethod("refresh", Class.class, OAObjectKey.class).getAnnotation(OARemoteMethod.class));
        assertNull(RemoteClientInterface.class.getMethod("refresh", Class.class, OAObjectKey.class, String.class).getAnnotation(OARemoteMethod.class));
    }
}
