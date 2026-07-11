package com.viaoa.oa.service;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

class OAReplicationServiceTest {

    @Test
    void typeIdentityMatchesProductionClass() {
        Class<?> type = OAReplicationService.class;

        assertEquals("com.viaoa.oa.service.OAReplicationService", type.getName());
        assertEquals("com.viaoa.oa.service", type.getPackageName());
        assertEquals("OAReplicationService", type.getSimpleName());
    }

    @Test
    void declaredContractMatchesExpectedKind() {
        Class<?> type = OAReplicationService.class;

        assertFalse(type.isInterface(), "OAReplicationService should remain a concrete or abstract class");
        assertFalse(type.isEnum(), "OAReplicationService should not become an enum");
        assertTrue(type.getDeclaredConstructors().length > 0, "constructors should be discoverable");
    }

    @Test
    void productionTypeIsNotSyntheticTestArtifact() {
        Class<?> type = OAReplicationService.class;

        assertFalse(type.isAnonymousClass());
        assertFalse(type.isLocalClass());
        assertFalse(type.isSynthetic());
        assertFalse(Modifier.isPrivate(type.getModifiers()), "top-level OA API type should not be private");
    }
}
