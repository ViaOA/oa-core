package com.viaoa.oa.api.services.objects;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

class OAObjectReflectOpsTest {

    @Test
    void typeIdentityMatchesProductionClass() {
        Class<?> type = OAObjectReflectOps.class;

        assertEquals("com.viaoa.oa.api.services.objects.OAObjectReflectOps", type.getName());
        assertEquals("com.viaoa.oa.api.services.objects", type.getPackageName());
        assertEquals("OAObjectReflectOps", type.getSimpleName());
    }

    @Test
    void declaredContractMatchesExpectedKind() {
        Class<?> type = OAObjectReflectOps.class;

        assertTrue(type.isInterface(), "OAObjectReflectOps should remain an interface");
        assertTrue(type.getDeclaredMethods().length > 0, "OAObjectReflectOps should expose an operation contract");
    }

    @Test
    void productionTypeIsNotSyntheticTestArtifact() {
        Class<?> type = OAObjectReflectOps.class;

        assertFalse(type.isAnonymousClass());
        assertFalse(type.isLocalClass());
        assertFalse(type.isSynthetic());
        assertFalse(Modifier.isPrivate(type.getModifiers()), "top-level OA API type should not be private");
    }
}
