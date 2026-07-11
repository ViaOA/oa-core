package com.viaoa.oa.service;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

class OAConfigServiceTest {

    @Test
    void typeIdentityMatchesProductionClass() {
        Class<?> type = OAConfigService.class;

        assertEquals("com.viaoa.oa.service.OAConfigService", type.getName());
        assertEquals("com.viaoa.oa.service", type.getPackageName());
        assertEquals("OAConfigService", type.getSimpleName());
    }

    @Test
    void declaredContractMatchesExpectedKind() {
        Class<?> type = OAConfigService.class;

        assertFalse(type.isInterface(), "OAConfigService should remain a concrete or abstract class");
        assertFalse(type.isEnum(), "OAConfigService should not become an enum");
        assertTrue(type.getDeclaredConstructors().length > 0, "constructors should be discoverable");
    }

    @Test
    void productionTypeIsNotSyntheticTestArtifact() {
        Class<?> type = OAConfigService.class;

        assertFalse(type.isAnonymousClass());
        assertFalse(type.isLocalClass());
        assertFalse(type.isSynthetic());
        assertFalse(Modifier.isPrivate(type.getModifiers()), "top-level OA API type should not be private");
    }
}
