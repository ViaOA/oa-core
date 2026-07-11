package com.viaoa.oa.service.hub;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

class HubAddRemoveServiceTest {

    @Test
    void typeIdentityMatchesProductionClass() {
        Class<?> type = HubAddRemoveService.class;

        assertEquals("com.viaoa.oa.service.hub.HubAddRemoveService", type.getName());
        assertEquals("com.viaoa.oa.service.hub", type.getPackageName());
        assertEquals("HubAddRemoveService", type.getSimpleName());
    }

    @Test
    void declaredContractMatchesExpectedKind() {
        Class<?> type = HubAddRemoveService.class;

        assertFalse(type.isInterface(), "HubAddRemoveService should remain a concrete or abstract class");
        assertFalse(type.isEnum(), "HubAddRemoveService should not become an enum");
        assertTrue(type.getDeclaredConstructors().length > 0, "constructors should be discoverable");
    }

    @Test
    void productionTypeIsNotSyntheticTestArtifact() {
        Class<?> type = HubAddRemoveService.class;

        assertFalse(type.isAnonymousClass());
        assertFalse(type.isLocalClass());
        assertFalse(type.isSynthetic());
        assertFalse(Modifier.isPrivate(type.getModifiers()), "top-level OA API type should not be private");
    }
}
