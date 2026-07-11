package com.viaoa.oa.api.internal;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

class ObjectsOpsTest {

    @Test
    void typeIdentityMatchesProductionClass() {
        Class<?> type = ObjectsOps.class;

        assertEquals("com.viaoa.oa.api.internal.ObjectsOps", type.getName());
        assertEquals("com.viaoa.oa.api.internal", type.getPackageName());
        assertEquals("ObjectsOps", type.getSimpleName());
    }

    @Test
    void declaredContractMatchesExpectedKind() {
        Class<?> type = ObjectsOps.class;

        assertTrue(type.isInterface(), "ObjectsOps should remain an interface");
        assertTrue(type.getDeclaredMethods().length > 0, "ObjectsOps should expose an operation contract");
    }

    @Test
    void productionTypeIsNotSyntheticTestArtifact() {
        Class<?> type = ObjectsOps.class;

        assertFalse(type.isAnonymousClass());
        assertFalse(type.isLocalClass());
        assertFalse(type.isSynthetic());
        assertFalse(Modifier.isPrivate(type.getModifiers()), "top-level OA API type should not be private");
    }
}
