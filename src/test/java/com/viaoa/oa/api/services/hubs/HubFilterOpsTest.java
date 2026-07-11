package com.viaoa.oa.api.services.hubs;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

class HubFilterOpsTest {

    @Test
    void typeIdentityMatchesProductionClass() {
        Class<?> type = HubFilterOps.class;

        assertEquals("com.viaoa.oa.api.services.hubs.HubFilterOps", type.getName());
        assertEquals("com.viaoa.oa.api.services.hubs", type.getPackageName());
        assertEquals("HubFilterOps", type.getSimpleName());
    }

    @Test
    void declaredContractMatchesExpectedKind() {
        Class<?> type = HubFilterOps.class;

        assertTrue(type.isInterface(), "HubFilterOps should remain an interface");
        assertTrue(type.getDeclaredMethods().length > 0, "HubFilterOps should expose an operation contract");
    }

    @Test
    void productionTypeIsNotSyntheticTestArtifact() {
        Class<?> type = HubFilterOps.class;

        assertFalse(type.isAnonymousClass());
        assertFalse(type.isLocalClass());
        assertFalse(type.isSynthetic());
        assertFalse(Modifier.isPrivate(type.getModifiers()), "top-level OA API type should not be private");
    }
}
