package com.viaoa.oa.service;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

class OAModelUserServiceTest {

    @Test
    void typeIdentityMatchesProductionClass() {
        Class<?> type = OAModelUserService.class;

        assertEquals("com.viaoa.oa.service.OAModelUserService", type.getName());
        assertEquals("com.viaoa.oa.service", type.getPackageName());
        assertEquals("OAModelUserService", type.getSimpleName());
    }

    @Test
    void declaredContractMatchesExpectedKind() {
        Class<?> type = OAModelUserService.class;

        assertFalse(type.isInterface(), "OAModelUserService should remain a concrete or abstract class");
        assertFalse(type.isEnum(), "OAModelUserService should not become an enum");
        assertTrue(type.getDeclaredConstructors().length > 0, "constructors should be discoverable");
    }

    @Test
    void productionTypeIsNotSyntheticTestArtifact() {
        Class<?> type = OAModelUserService.class;

        assertFalse(type.isAnonymousClass());
        assertFalse(type.isLocalClass());
        assertFalse(type.isSynthetic());
        assertFalse(Modifier.isPrivate(type.getModifiers()), "top-level OA API type should not be private");
    }
}
