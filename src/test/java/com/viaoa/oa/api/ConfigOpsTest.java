package com.viaoa.oa.api;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

class ConfigOpsTest {

    @Test
    void typeIdentityMatchesProductionClass() {
        Class<?> type = ConfigOps.class;

        assertEquals("com.viaoa.oa.api.ConfigOps", type.getName());
        assertEquals("com.viaoa.oa.api", type.getPackageName());
        assertEquals("ConfigOps", type.getSimpleName());
    }

    @Test
    void declaredContractMatchesExpectedKind() {
        Class<?> type = ConfigOps.class;

        assertTrue(type.isInterface(), "ConfigOps should remain an interface");
        assertTrue(type.getDeclaredMethods().length > 0, "ConfigOps should expose an operation contract");
    }

    @Test
    void productionTypeIsNotSyntheticTestArtifact() {
        Class<?> type = ConfigOps.class;

        assertFalse(type.isAnonymousClass());
        assertFalse(type.isLocalClass());
        assertFalse(type.isSynthetic());
        assertFalse(Modifier.isPrivate(type.getModifiers()), "top-level OA API type should not be private");
    }
}
