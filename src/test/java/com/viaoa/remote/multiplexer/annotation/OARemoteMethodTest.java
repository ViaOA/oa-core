package com.viaoa.remote.multiplexer.annotation;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

class OARemoteMethodTest {

    @Test
    void compressedReturnValueDefaultIsFalse() throws Exception {
        OARemoteMethod annotation = method("defaults").getAnnotation(OARemoteMethod.class);

        assertFalse(annotation.compressedReturnValue());
    }

    @Test
    void noReturnValueDefaultIsFalse() throws Exception {
        OARemoteMethod annotation = method("defaults").getAnnotation(OARemoteMethod.class);

        assertFalse(annotation.noReturnValue());
    }

    @Test
    void timeoutSecondsDefaultIsZero() throws Exception {
        OARemoteMethod annotation = method("defaults").getAnnotation(OARemoteMethod.class);

        assertEquals(0, annotation.timeoutSeconds());
    }

    @Test
    void dontUseQueueForReturnValueDefaultIsFalse() throws Exception {
        OARemoteMethod annotation = method("defaults").getAnnotation(OARemoteMethod.class);

        assertFalse(annotation.dontUseQueueForReturnValue());
    }

    @Test
    void dontUseQueueDefaultIsFalse() throws Exception {
        OARemoteMethod annotation = method("defaults").getAnnotation(OARemoteMethod.class);

        assertFalse(annotation.dontUseQueue());
    }

    @Test
    void returnOnQueueSocketDefaultIsFalse() throws Exception {
        OARemoteMethod annotation = method("defaults").getAnnotation(OARemoteMethod.class);

        assertFalse(annotation.returnOnQueueSocket());
    }

    @Test
    void runInRemoteThreadDefaultIsFalse() throws Exception {
        OARemoteMethod annotation = method("defaults").getAnnotation(OARemoteMethod.class);

        assertFalse(annotation.runInRemoteThread());
    }

    @Test
    void explicitValuesAreAvailableAtRuntime() throws Exception {
        OARemoteMethod annotation = method("explicit").getAnnotation(OARemoteMethod.class);

        assertTrue(annotation.compressedReturnValue());
        assertTrue(annotation.noReturnValue());
        assertEquals(12, annotation.timeoutSeconds());
        assertTrue(annotation.dontUseQueueForReturnValue());
        assertTrue(annotation.dontUseQueue());
        assertTrue(annotation.returnOnQueueSocket());
        assertTrue(annotation.runInRemoteThread());
    }

    @Test
    void metadataUsesRuntimeRetentionMethodTargetAndDocumented() {
        Retention retention = OARemoteMethod.class.getAnnotation(Retention.class);
        Target target = OARemoteMethod.class.getAnnotation(Target.class);

        assertEquals(RetentionPolicy.RUNTIME, retention.value());
        assertArrayEquals(new ElementType[] { ElementType.METHOD }, target.value());
        assertNotNull(OARemoteMethod.class.getAnnotation(Documented.class));
    }

    private static Method method(String name) throws Exception {
        return Remote.class.getMethod(name);
    }

    private interface Remote {
        @OARemoteMethod
        void defaults();

        @OARemoteMethod(compressedReturnValue = true, noReturnValue = true, timeoutSeconds = 12,
                dontUseQueueForReturnValue = true, dontUseQueue = true, returnOnQueueSocket = true,
                runInRemoteThread = true)
        String explicit();
    }
}
