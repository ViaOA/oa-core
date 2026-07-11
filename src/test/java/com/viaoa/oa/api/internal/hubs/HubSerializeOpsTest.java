package com.viaoa.oa.api.internal.hubs;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

class HubSerializeOpsTest {

    @Test
    void typeIdentityMatchesProductionClass() {
        Class<?> type = HubSerializeOps.class;

        assertEquals("com.viaoa.oa.api.internal.hubs.HubSerializeOps", type.getName());
        assertEquals("com.viaoa.oa.api.internal.hubs", type.getPackageName());
        assertEquals("HubSerializeOps", type.getSimpleName());
    }

    @Test
    void declaredContractMatchesExpectedKind() {
        Class<?> type = HubSerializeOps.class;

        assertTrue(type.isInterface(), "HubSerializeOps should remain an interface");
        assertTrue(type.getDeclaredMethods().length > 0, "HubSerializeOps should expose an operation contract");
    }

    @Test
    void productionTypeIsNotSyntheticTestArtifact() {
        Class<?> type = HubSerializeOps.class;

        assertFalse(type.isAnonymousClass());
        assertFalse(type.isLocalClass());
        assertFalse(type.isSynthetic());
        assertFalse(Modifier.isPrivate(type.getModifiers()), "top-level OA API type should not be private");
    }
}
