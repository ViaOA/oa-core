package com.viaoa.oa.api.services;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

class TriggersOpsTest {

    @Test
    void typeIdentityMatchesProductionClass() {
        Class<?> type = TriggersOps.class;

        assertEquals("com.viaoa.oa.api.services.TriggersOps", type.getName());
        assertEquals("com.viaoa.oa.api.services", type.getPackageName());
        assertEquals("TriggersOps", type.getSimpleName());
    }

    @Test
    void declaredContractMatchesExpectedKind() {
        Class<?> type = TriggersOps.class;

        assertTrue(type.isInterface(), "TriggersOps should remain an interface");
        assertTrue(type.getDeclaredMethods().length > 0, "TriggersOps should expose an operation contract");
    }

    @Test
    void productionTypeIsNotSyntheticTestArtifact() {
        Class<?> type = TriggersOps.class;

        assertFalse(type.isAnonymousClass());
        assertFalse(type.isLocalClass());
        assertFalse(type.isSynthetic());
        assertFalse(Modifier.isPrivate(type.getModifiers()), "top-level OA API type should not be private");
    }
}
