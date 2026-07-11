package com.viaoa.oa.service.hub;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

class HubSortServiceTest {

    @Test
    void typeIdentityMatchesProductionClass() {
        Class<?> type = HubSortService.class;

        assertEquals("com.viaoa.oa.service.hub.HubSortService", type.getName());
        assertEquals("com.viaoa.oa.service.hub", type.getPackageName());
        assertEquals("HubSortService", type.getSimpleName());
    }

    @Test
    void declaredContractMatchesExpectedKind() {
        Class<?> type = HubSortService.class;

        assertFalse(type.isInterface(), "HubSortService should remain a concrete or abstract class");
        assertFalse(type.isEnum(), "HubSortService should not become an enum");
        assertTrue(type.getDeclaredConstructors().length > 0, "constructors should be discoverable");
    }

    @Test
    void productionTypeIsNotSyntheticTestArtifact() {
        Class<?> type = HubSortService.class;

        assertFalse(type.isAnonymousClass());
        assertFalse(type.isLocalClass());
        assertFalse(type.isSynthetic());
        assertFalse(Modifier.isPrivate(type.getModifiers()), "top-level OA API type should not be private");
    }
}
