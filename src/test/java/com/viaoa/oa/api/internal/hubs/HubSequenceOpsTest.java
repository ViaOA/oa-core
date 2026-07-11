package com.viaoa.oa.api.internal.hubs;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

class HubSequenceOpsTest {

    @Test
    void typeIdentityMatchesProductionClass() {
        Class<?> type = HubSequenceOps.class;

        assertEquals("com.viaoa.oa.api.internal.hubs.HubSequenceOps", type.getName());
        assertEquals("com.viaoa.oa.api.internal.hubs", type.getPackageName());
        assertEquals("HubSequenceOps", type.getSimpleName());
    }

    @Test
    void declaredContractMatchesExpectedKind() {
        Class<?> type = HubSequenceOps.class;

        assertTrue(type.isInterface(), "HubSequenceOps should remain an interface");
        assertTrue(type.getDeclaredMethods().length > 0, "HubSequenceOps should expose an operation contract");
    }

    @Test
    void productionTypeIsNotSyntheticTestArtifact() {
        Class<?> type = HubSequenceOps.class;

        assertFalse(type.isAnonymousClass());
        assertFalse(type.isLocalClass());
        assertFalse(type.isSynthetic());
        assertFalse(Modifier.isPrivate(type.getModifiers()), "top-level OA API type should not be private");
    }
}
