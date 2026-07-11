package com.viaoa.oa.api.services;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

class RulesOpsTest {

    @Test
    void typeIdentityMatchesProductionClass() {
        Class<?> type = RulesOps.class;

        assertEquals("com.viaoa.oa.api.services.RulesOps", type.getName());
        assertEquals("com.viaoa.oa.api.services", type.getPackageName());
        assertEquals("RulesOps", type.getSimpleName());
    }

    @Test
    void declaredContractMatchesExpectedKind() {
        Class<?> type = RulesOps.class;

        assertTrue(type.isInterface(), "RulesOps should remain an interface");
        assertTrue(type.getDeclaredMethods().length > 0, "RulesOps should expose an operation contract");
    }

    @Test
    void productionTypeIsNotSyntheticTestArtifact() {
        Class<?> type = RulesOps.class;

        assertFalse(type.isAnonymousClass());
        assertFalse(type.isLocalClass());
        assertFalse(type.isSynthetic());
        assertFalse(Modifier.isPrivate(type.getModifiers()), "top-level OA API type should not be private");
    }
}
