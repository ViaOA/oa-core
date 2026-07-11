package com.viaoa.oa.api.internal.hubs;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

class HubAutoMatchOpsTest {

    @Test
    void typeIdentityMatchesProductionClass() {
        Class<?> type = HubAutoMatchOps.class;

        assertEquals("com.viaoa.oa.api.internal.hubs.HubAutoMatchOps", type.getName());
        assertEquals("com.viaoa.oa.api.internal.hubs", type.getPackageName());
        assertEquals("HubAutoMatchOps", type.getSimpleName());
    }

    @Test
    void declaredContractMatchesExpectedKind() {
        Class<?> type = HubAutoMatchOps.class;

        assertTrue(type.isInterface(), "HubAutoMatchOps should remain an interface");
        assertTrue(type.getDeclaredMethods().length > 0, "HubAutoMatchOps should expose an operation contract");
    }

    @Test
    void productionTypeIsNotSyntheticTestArtifact() {
        Class<?> type = HubAutoMatchOps.class;

        assertFalse(type.isAnonymousClass());
        assertFalse(type.isLocalClass());
        assertFalse(type.isSynthetic());
        assertFalse(Modifier.isPrivate(type.getModifiers()), "top-level OA API type should not be private");
    }
}
