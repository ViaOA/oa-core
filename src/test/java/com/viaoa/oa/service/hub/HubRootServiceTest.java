package com.viaoa.oa.service.hub;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

class HubRootServiceTest {

    @Test
    void typeIdentityMatchesProductionClass() {
        Class<?> type = HubRootService.class;

        assertEquals("com.viaoa.oa.service.hub.HubRootService", type.getName());
        assertEquals("com.viaoa.oa.service.hub", type.getPackageName());
        assertEquals("HubRootService", type.getSimpleName());
    }

    @Test
    void declaredContractMatchesExpectedKind() {
        Class<?> type = HubRootService.class;

        assertFalse(type.isInterface(), "HubRootService should remain a concrete or abstract class");
        assertFalse(type.isEnum(), "HubRootService should not become an enum");
        assertTrue(type.getDeclaredConstructors().length > 0, "constructors should be discoverable");
    }

    @Test
    void productionTypeIsNotSyntheticTestArtifact() {
        Class<?> type = HubRootService.class;

        assertFalse(type.isAnonymousClass());
        assertFalse(type.isLocalClass());
        assertFalse(type.isSynthetic());
        assertFalse(Modifier.isPrivate(type.getModifiers()), "top-level OA API type should not be private");
    }
}
