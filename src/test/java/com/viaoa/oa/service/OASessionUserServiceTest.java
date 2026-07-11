package com.viaoa.oa.service;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

class OASessionUserServiceTest {

    @Test
    void typeIdentityMatchesProductionClass() {
        Class<?> type = OASessionUserService.class;

        assertEquals("com.viaoa.oa.service.OASessionUserService", type.getName());
        assertEquals("com.viaoa.oa.service", type.getPackageName());
        assertEquals("OASessionUserService", type.getSimpleName());
    }

    @Test
    void declaredContractMatchesExpectedKind() {
        Class<?> type = OASessionUserService.class;

        assertFalse(type.isInterface(), "OASessionUserService should remain a concrete or abstract class");
        assertFalse(type.isEnum(), "OASessionUserService should not become an enum");
        assertTrue(type.getDeclaredConstructors().length > 0, "constructors should be discoverable");
    }

    @Test
    void productionTypeIsNotSyntheticTestArtifact() {
        Class<?> type = OASessionUserService.class;

        assertFalse(type.isAnonymousClass());
        assertFalse(type.isLocalClass());
        assertFalse(type.isSynthetic());
        assertFalse(Modifier.isPrivate(type.getModifiers()), "top-level OA API type should not be private");
    }
}
