package com.viaoa.oa.service.object;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

class OAObjectSiblingServiceTest {

    @Test
    void typeIdentityMatchesProductionClass() {
        Class<?> type = OAObjectSiblingService.class;

        assertEquals("com.viaoa.oa.service.object.OAObjectSiblingService", type.getName());
        assertEquals("com.viaoa.oa.service.object", type.getPackageName());
        assertEquals("OAObjectSiblingService", type.getSimpleName());
    }

    @Test
    void declaredContractMatchesExpectedKind() {
        Class<?> type = OAObjectSiblingService.class;

        assertFalse(type.isInterface(), "OAObjectSiblingService should remain a concrete or abstract class");
        assertFalse(type.isEnum(), "OAObjectSiblingService should not become an enum");
        assertTrue(type.getDeclaredConstructors().length > 0, "constructors should be discoverable");
    }

    @Test
    void productionTypeIsNotSyntheticTestArtifact() {
        Class<?> type = OAObjectSiblingService.class;

        assertFalse(type.isAnonymousClass());
        assertFalse(type.isLocalClass());
        assertFalse(type.isSynthetic());
        assertFalse(Modifier.isPrivate(type.getModifiers()), "top-level OA API type should not be private");
    }
}
