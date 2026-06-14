package com.viaoa.remote.multiplexer.annotation;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Parameter;

import org.junit.jupiter.api.Test;

class OARemoteParameterTest {

    @Test
    void compressedDefaultIsFalse() throws Exception {
        OARemoteParameter annotation = parameter("defaults").getAnnotation(OARemoteParameter.class);

        assertFalse(annotation.compressed());
    }

    @Test
    void dontUseQueueDefaultIsFalse() throws Exception {
        OARemoteParameter annotation = parameter("defaults").getAnnotation(OARemoteParameter.class);

        assertFalse(annotation.dontUseQueue());
    }

    @Test
    void explicitValuesAreAvailableAtRuntime() throws Exception {
        OARemoteParameter annotation = parameter("explicit").getAnnotation(OARemoteParameter.class);

        assertTrue(annotation.compressed());
        assertTrue(annotation.dontUseQueue());
    }

    @Test
    void metadataUsesRuntimeRetentionParameterTargetAndDocumented() {
        Retention retention = OARemoteParameter.class.getAnnotation(Retention.class);
        Target target = OARemoteParameter.class.getAnnotation(Target.class);

        assertEquals(RetentionPolicy.RUNTIME, retention.value());
        assertArrayEquals(new ElementType[] { ElementType.PARAMETER }, target.value());
        assertNotNull(OARemoteParameter.class.getAnnotation(Documented.class));
    }

    private static Parameter parameter(String name) throws Exception {
        return Remote.class.getMethod(name, String.class).getParameters()[0];
    }

    private interface Remote {
        void defaults(@OARemoteParameter String value);

        void explicit(@OARemoteParameter(compressed = true, dontUseQueue = true) String value);
    }
}
