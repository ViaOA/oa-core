package com.viaoa.oa.api.services;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

class ServicesOpsTest {

    @Test
    void typeIdentityMatchesProductionClass() {
        Class<?> type = ServicesOps.class;

        assertEquals("com.viaoa.oa.api.services.ServicesOps", type.getName());
        assertEquals("com.viaoa.oa.api.services", type.getPackageName());
        assertEquals("ServicesOps", type.getSimpleName());
    }

    @Test
    void declaredContractMatchesExpectedKind() {
        Class<?> type = ServicesOps.class;

        assertTrue(type.isInterface(), "ServicesOps should remain an interface");
        assertTrue(type.getDeclaredMethods().length > 0, "ServicesOps should expose an operation contract");
    }

    @Test
    void productionTypeIsNotSyntheticTestArtifact() {
        Class<?> type = ServicesOps.class;

        assertFalse(type.isAnonymousClass());
        assertFalse(type.isLocalClass());
        assertFalse(type.isSynthetic());
        assertFalse(Modifier.isPrivate(type.getModifiers()), "top-level OA API type should not be private");
    }
}
