package com.viaoa.oa.api.internal.objects;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

class OAObjectKeyOpsTest {

    @Test
    void typeIdentityMatchesProductionClass() {
        Class<?> type = OAObjectKeyOps.class;

        assertEquals("com.viaoa.oa.api.internal.objects.OAObjectKeyOps", type.getName());
        assertEquals("com.viaoa.oa.api.internal.objects", type.getPackageName());
        assertEquals("OAObjectKeyOps", type.getSimpleName());
    }

    @Test
    void declaredContractMatchesExpectedKind() {
        Class<?> type = OAObjectKeyOps.class;

        assertTrue(type.isInterface(), "OAObjectKeyOps should remain an interface");
        assertTrue(type.getDeclaredMethods().length > 0, "OAObjectKeyOps should expose an operation contract");
    }

    @Test
    void productionTypeIsNotSyntheticTestArtifact() {
        Class<?> type = OAObjectKeyOps.class;

        assertFalse(type.isAnonymousClass());
        assertFalse(type.isLocalClass());
        assertFalse(type.isSynthetic());
        assertFalse(Modifier.isPrivate(type.getModifiers()), "top-level OA API type should not be private");
    }
}
