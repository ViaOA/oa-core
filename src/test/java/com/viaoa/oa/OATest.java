package com.viaoa.oa;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

class OATest {

    @Test
    void typeIdentityMatchesProductionClass() {
        Class<?> type = OA.class;

        assertEquals("com.viaoa.oa.OA", type.getName());
        assertEquals("com.viaoa.oa", type.getPackageName());
        assertEquals("OA", type.getSimpleName());
    }

    @Test
    void declaredContractMatchesExpectedKind() {
        Class<?> type = OA.class;

        assertTrue(type.isInterface(), "OA should remain an interface");
        assertTrue(type.getDeclaredMethods().length > 0, "OA should expose an operation contract");
    }

    @Test
    void productionTypeIsNotSyntheticTestArtifact() {
        Class<?> type = OA.class;

        assertFalse(type.isAnonymousClass());
        assertFalse(type.isLocalClass());
        assertFalse(type.isSynthetic());
        assertFalse(Modifier.isPrivate(type.getModifiers()), "top-level OA API type should not be private");
    }
}
