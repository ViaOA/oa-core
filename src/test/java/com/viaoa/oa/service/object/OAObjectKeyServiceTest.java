package com.viaoa.oa.service.object;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

class OAObjectKeyServiceTest {

    @Test
    void typeIdentityMatchesProductionClass() {
        Class<?> type = OAObjectKeyService.class;

        assertEquals("com.viaoa.oa.service.object.OAObjectKeyService", type.getName());
        assertEquals("com.viaoa.oa.service.object", type.getPackageName());
        assertEquals("OAObjectKeyService", type.getSimpleName());
    }

    @Test
    void declaredContractMatchesExpectedKind() {
        Class<?> type = OAObjectKeyService.class;

        assertFalse(type.isInterface(), "OAObjectKeyService should remain a concrete or abstract class");
        assertFalse(type.isEnum(), "OAObjectKeyService should not become an enum");
        assertTrue(type.getDeclaredConstructors().length > 0, "constructors should be discoverable");
    }

    @Test
    void productionTypeIsNotSyntheticTestArtifact() {
        Class<?> type = OAObjectKeyService.class;

        assertFalse(type.isAnonymousClass());
        assertFalse(type.isLocalClass());
        assertFalse(type.isSynthetic());
        assertFalse(Modifier.isPrivate(type.getModifiers()), "top-level OA API type should not be private");
    }
}
