package com.viaoa.oa.service;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

class OATriggerServiceTest {

    @Test
    void typeIdentityMatchesProductionClass() {
        Class<?> type = OATriggerService.class;

        assertEquals("com.viaoa.oa.service.OATriggerService", type.getName());
        assertEquals("com.viaoa.oa.service", type.getPackageName());
        assertEquals("OATriggerService", type.getSimpleName());
    }

    @Test
    void declaredContractMatchesExpectedKind() {
        Class<?> type = OATriggerService.class;

        assertFalse(type.isInterface(), "OATriggerService should remain a concrete or abstract class");
        assertFalse(type.isEnum(), "OATriggerService should not become an enum");
        assertTrue(type.getDeclaredConstructors().length > 0, "constructors should be discoverable");
    }

    @Test
    void productionTypeIsNotSyntheticTestArtifact() {
        Class<?> type = OATriggerService.class;

        assertFalse(type.isAnonymousClass());
        assertFalse(type.isLocalClass());
        assertFalse(type.isSynthetic());
        assertFalse(Modifier.isPrivate(type.getModifiers()), "top-level OA API type should not be private");
    }
}
