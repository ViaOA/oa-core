package com.viaoa.oa.service.hub;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

class HubDeleteServiceTest {

    @Test
    void typeIdentityMatchesProductionClass() {
        Class<?> type = HubDeleteService.class;

        assertEquals("com.viaoa.oa.service.hub.HubDeleteService", type.getName());
        assertEquals("com.viaoa.oa.service.hub", type.getPackageName());
        assertEquals("HubDeleteService", type.getSimpleName());
    }

    @Test
    void declaredContractMatchesExpectedKind() {
        Class<?> type = HubDeleteService.class;

        assertFalse(type.isInterface(), "HubDeleteService should remain a concrete or abstract class");
        assertFalse(type.isEnum(), "HubDeleteService should not become an enum");
        assertTrue(type.getDeclaredConstructors().length > 0, "constructors should be discoverable");
    }

    @Test
    void productionTypeIsNotSyntheticTestArtifact() {
        Class<?> type = HubDeleteService.class;

        assertFalse(type.isAnonymousClass());
        assertFalse(type.isLocalClass());
        assertFalse(type.isSynthetic());
        assertFalse(Modifier.isPrivate(type.getModifiers()), "top-level OA API type should not be private");
    }
}
