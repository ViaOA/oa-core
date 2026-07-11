package com.viaoa.oa.service.hub;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

class HubPropertyServiceTest {

    @Test
    void typeIdentityMatchesProductionClass() {
        Class<?> type = HubPropertyService.class;

        assertEquals("com.viaoa.oa.service.hub.HubPropertyService", type.getName());
        assertEquals("com.viaoa.oa.service.hub", type.getPackageName());
        assertEquals("HubPropertyService", type.getSimpleName());
    }

    @Test
    void declaredContractMatchesExpectedKind() {
        Class<?> type = HubPropertyService.class;

        assertFalse(type.isInterface(), "HubPropertyService should remain a concrete or abstract class");
        assertFalse(type.isEnum(), "HubPropertyService should not become an enum");
        assertTrue(type.getDeclaredConstructors().length > 0, "constructors should be discoverable");
    }

    @Test
    void productionTypeIsNotSyntheticTestArtifact() {
        Class<?> type = HubPropertyService.class;

        assertFalse(type.isAnonymousClass());
        assertFalse(type.isLocalClass());
        assertFalse(type.isSynthetic());
        assertFalse(Modifier.isPrivate(type.getModifiers()), "top-level OA API type should not be private");
    }
}
