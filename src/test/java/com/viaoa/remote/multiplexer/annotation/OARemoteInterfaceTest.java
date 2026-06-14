package com.viaoa.remote.multiplexer.annotation;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.Test;

class OARemoteInterfaceTest {

    @Test
    void isOASyncDefaultIsFalse() throws Exception {
        OARemoteInterface annotation = DefaultRemote.class.getAnnotation(OARemoteInterface.class);

        assertFalse(annotation.isOASync());
    }

    @Test
    void isOASyncExplicitValueIsAvailableAtRuntime() throws Exception {
        OARemoteInterface annotation = SyncRemote.class.getAnnotation(OARemoteInterface.class);

        assertTrue(annotation.isOASync());
    }

    @Test
    void metadataUsesRuntimeRetentionTypeTargetAndDocumented() {
        Retention retention = OARemoteInterface.class.getAnnotation(Retention.class);
        Target target = OARemoteInterface.class.getAnnotation(Target.class);

        assertEquals(RetentionPolicy.RUNTIME, retention.value());
        assertArrayEquals(new ElementType[] { ElementType.TYPE }, target.value());
        assertNotNull(OARemoteInterface.class.getAnnotation(Documented.class));
    }

    @OARemoteInterface
    private interface DefaultRemote {
    }

    @OARemoteInterface(isOASync = true)
    private interface SyncRemote {
    }
}
