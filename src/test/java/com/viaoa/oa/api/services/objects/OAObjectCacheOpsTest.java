package com.viaoa.oa.api.services.objects;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

class OAObjectCacheOpsTest {

    @Test
    void typeIdentityMatchesProductionClass() {
        Class<?> type = OAObjectCacheOps.class;

        assertEquals("com.viaoa.oa.api.services.objects.OAObjectCacheOps", type.getName());
        assertEquals("com.viaoa.oa.api.services.objects", type.getPackageName());
        assertEquals("OAObjectCacheOps", type.getSimpleName());
    }

    @Test
    void declaredContractMatchesExpectedKind() {
        Class<?> type = OAObjectCacheOps.class;

        assertTrue(type.isInterface(), "OAObjectCacheOps should remain an interface");
        assertTrue(type.getDeclaredMethods().length > 0, "OAObjectCacheOps should expose an operation contract");
    }

    @Test
    void productionTypeIsNotSyntheticTestArtifact() {
        Class<?> type = OAObjectCacheOps.class;

        assertFalse(type.isAnonymousClass());
        assertFalse(type.isLocalClass());
        assertFalse(type.isSynthetic());
        assertFalse(Modifier.isPrivate(type.getModifiers()), "top-level OA API type should not be private");
    }
}
