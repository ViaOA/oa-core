package com.viaoa.oa.api;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

class ModelUserOpsTest {

    @Test
    void typeIdentityMatchesProductionClass() {
        Class<?> type = ModelUserOps.class;

        assertEquals("com.viaoa.oa.api.ModelUserOps", type.getName());
        assertEquals("com.viaoa.oa.api", type.getPackageName());
        assertEquals("ModelUserOps", type.getSimpleName());
    }

    @Test
    void declaredContractMatchesExpectedKind() {
        Class<?> type = ModelUserOps.class;

        assertTrue(type.isInterface(), "ModelUserOps should remain an interface");
        assertTrue(type.getDeclaredMethods().length > 0, "ModelUserOps should expose an operation contract");
    }

    @Test
    void productionTypeIsNotSyntheticTestArtifact() {
        Class<?> type = ModelUserOps.class;

        assertFalse(type.isAnonymousClass());
        assertFalse(type.isLocalClass());
        assertFalse(type.isSynthetic());
        assertFalse(Modifier.isPrivate(type.getModifiers()), "top-level OA API type should not be private");
    }
}
