package com.viaoa.oa.service;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

class OASyncServiceTest {

    @Test
    void typeIdentityMatchesProductionClass() {
        Class<?> type = OASyncService.class;

        assertEquals("com.viaoa.oa.service.OASyncService", type.getName());
        assertEquals("com.viaoa.oa.service", type.getPackageName());
        assertEquals("OASyncService", type.getSimpleName());
    }

    @Test
    void declaredContractMatchesExpectedKind() {
        Class<?> type = OASyncService.class;

        assertFalse(type.isInterface(), "OASyncService should remain a concrete or abstract class");
        assertFalse(type.isEnum(), "OASyncService should not become an enum");
        assertTrue(type.getDeclaredConstructors().length > 0, "constructors should be discoverable");
    }

    @Test
    void productionTypeIsNotSyntheticTestArtifact() {
        Class<?> type = OASyncService.class;

        assertFalse(type.isAnonymousClass());
        assertFalse(type.isLocalClass());
        assertFalse(type.isSynthetic());
        assertFalse(Modifier.isPrivate(type.getModifiers()), "top-level OA API type should not be private");
    }
}
