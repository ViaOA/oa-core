package com.viaoa.oa.api;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

class SessionUserOpsTest {

    @Test
    void typeIdentityMatchesProductionClass() {
        Class<?> type = SessionUserOps.class;

        assertEquals("com.viaoa.oa.api.SessionUserOps", type.getName());
        assertEquals("com.viaoa.oa.api", type.getPackageName());
        assertEquals("SessionUserOps", type.getSimpleName());
    }

    @Test
    void declaredContractMatchesExpectedKind() {
        Class<?> type = SessionUserOps.class;

        assertTrue(type.isInterface(), "SessionUserOps should remain an interface");
        assertTrue(type.getDeclaredMethods().length > 0, "SessionUserOps should expose an operation contract");
    }

    @Test
    void productionTypeIsNotSyntheticTestArtifact() {
        Class<?> type = SessionUserOps.class;

        assertFalse(type.isAnonymousClass());
        assertFalse(type.isLocalClass());
        assertFalse(type.isSynthetic());
        assertFalse(Modifier.isPrivate(type.getModifiers()), "top-level OA API type should not be private");
    }
}
