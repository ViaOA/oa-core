package com.viaoa.oa.api.internal.hubs;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

class HubMergeOpsTest {

    @Test
    void typeIdentityMatchesProductionClass() {
        Class<?> type = HubMergeOps.class;

        assertEquals("com.viaoa.oa.api.internal.hubs.HubMergeOps", type.getName());
        assertEquals("com.viaoa.oa.api.internal.hubs", type.getPackageName());
        assertEquals("HubMergeOps", type.getSimpleName());
    }

    @Test
    void declaredContractMatchesExpectedKind() {
        Class<?> type = HubMergeOps.class;

        assertTrue(type.isInterface(), "HubMergeOps should remain an interface");
        assertTrue(type.getDeclaredMethods().length > 0, "HubMergeOps should expose an operation contract");
    }

    @Test
    void productionTypeIsNotSyntheticTestArtifact() {
        Class<?> type = HubMergeOps.class;

        assertFalse(type.isAnonymousClass());
        assertFalse(type.isLocalClass());
        assertFalse(type.isSynthetic());
        assertFalse(Modifier.isPrivate(type.getModifiers()), "top-level OA API type should not be private");
    }
}
