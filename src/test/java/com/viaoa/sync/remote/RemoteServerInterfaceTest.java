package com.viaoa.sync.remote;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectKey;
import com.viaoa.remote.multiplexer.annotation.OARemoteInterface;
import com.viaoa.remote.multiplexer.annotation.OARemoteMethod;
import com.viaoa.remote.multiplexer.annotation.OARemoteParameter;
import com.viaoa.sync.model.ClientInfo;

class RemoteServerInterfaceTest {

    @Test
    void interfaceIsRemoteInterface() {
        assertNotNull(RemoteServerInterface.class.getAnnotation(OARemoteInterface.class));
    }

    @Test
    void getObjectMethodsReturnOnQueueSocket() throws Exception {
        assertTrue(method("getObject", Class.class, OAObjectKey.class).returnOnQueueSocket());
        assertTrue(method("getObjectUsingPkey", Class.class, OAObjectKey.class).returnOnQueueSocket());
    }

    @Test
    void getRemoteSessionCallbackParameterBypassesQueue() throws Exception {
        Method m = RemoteServerInterface.class.getMethod("getRemoteSession", ClientInfo.class, RemoteClientCallbackInterface.class);
        Annotation[][] anns = m.getParameterAnnotations();
        OARemoteParameter param = null;
        for (Annotation ann : anns[1]) {
            if (ann instanceof OARemoteParameter) param = (OARemoteParameter) ann;
        }

        assertNotNull(param);
        assertTrue(param.dontUseQueue());
    }

    @Test
    void lowLatencyServerMethodsBypassQueue() throws Exception {
        assertTrue(method("ping", String.class).dontUseQueue());
        OARemoteMethod ping2 = method("ping2", String.class);
        assertTrue(ping2.noReturnValue());
        assertTrue(ping2.dontUseQueue());
        assertTrue(method("getDisplayMessage").dontUseQueue());
        assertTrue(method("getNextFiftyObjectGuids").dontUseQueue());
    }

    @Test
    void cacheRefreshIsNoReturnAndBypassesQueue() throws Exception {
        OARemoteMethod ann = method("refreshCache", Class.class);

        assertTrue(ann.noReturnValue());
        assertTrue(ann.dontUseQueue());
    }

    @Test
    void remoteMethodInvocationMethodsReturnOnQueueSocket() throws Exception {
        assertTrue(method("runRemoteMethod", Class.class, OAObjectKey.class, String.class, Object[].class).returnOnQueueSocket());
        assertTrue(method("runRemoteMethod2", OAObject.class, String.class, Object[].class).returnOnQueueSocket());
        assertTrue(method("runRemoteMethod", Hub.class, String.class, Object[].class).returnOnQueueSocket());
    }

    @Test
    void performThreadDumpBypassesQueueAndGetUniqueReturnsOnQueueSocket() throws Exception {
        assertTrue(method("performThreadDump", String.class).dontUseQueue());
        assertTrue(method("getUnique", Class.class, String.class, Object.class, boolean.class).returnOnQueueSocket());
    }

    private OARemoteMethod method(String name, Class<?>... types) throws Exception {
        OARemoteMethod ann = RemoteServerInterface.class.getMethod(name, types).getAnnotation(OARemoteMethod.class);
        assertNotNull(ann, name + " should have OARemoteMethod");
        return ann;
    }
}
