package com.viaoa.oa.service.hub;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

class HubAutoMatchServiceTest {

    @Test
    void typeIdentityMatchesProductionClass() {
        Class<?> type = HubAutoMatchService.class;

        assertEquals("com.viaoa.oa.service.hub.HubAutoMatchService", type.getName());
        assertEquals("com.viaoa.oa.service.hub", type.getPackageName());
        assertEquals("HubAutoMatchService", type.getSimpleName());
    }

    @Test
    void declaredContractMatchesExpectedKind() {
        Class<?> type = HubAutoMatchService.class;

        assertFalse(type.isInterface(), "HubAutoMatchService should remain a concrete or abstract class");
        assertFalse(type.isEnum(), "HubAutoMatchService should not become an enum");
        assertTrue(type.getDeclaredConstructors().length > 0, "constructors should be discoverable");
    }

    @Test
    void productionTypeIsNotSyntheticTestArtifact() {
        Class<?> type = HubAutoMatchService.class;

        assertFalse(type.isAnonymousClass());
        assertFalse(type.isLocalClass());
        assertFalse(type.isSynthetic());
        assertFalse(Modifier.isPrivate(type.getModifiers()), "top-level OA API type should not be private");
    }
}
