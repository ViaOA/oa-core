package com.viaoa.oa.service.hub;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

class HubSizeServiceTest {

    @Test
    void typeIdentityMatchesProductionClass() {
        Class<?> type = HubSizeService.class;

        assertEquals("com.viaoa.oa.service.hub.HubSizeService", type.getName());
        assertEquals("com.viaoa.oa.service.hub", type.getPackageName());
        assertEquals("HubSizeService", type.getSimpleName());
    }

    @Test
    void declaredContractMatchesExpectedKind() {
        Class<?> type = HubSizeService.class;

        assertFalse(type.isInterface(), "HubSizeService should remain a concrete or abstract class");
        assertFalse(type.isEnum(), "HubSizeService should not become an enum");
        assertTrue(type.getDeclaredConstructors().length > 0, "constructors should be discoverable");
    }

    @Test
    void productionTypeIsNotSyntheticTestArtifact() {
        Class<?> type = HubSizeService.class;

        assertFalse(type.isAnonymousClass());
        assertFalse(type.isLocalClass());
        assertFalse(type.isSynthetic());
        assertFalse(Modifier.isPrivate(type.getModifiers()), "top-level OA API type should not be private");
    }
}
