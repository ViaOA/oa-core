package com.viaoa.oa.service.object;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

class OAObjectLockServiceTest {

    @Test
    void typeIdentityMatchesProductionClass() {
        Class<?> type = OAObjectLockService.class;

        assertEquals("com.viaoa.oa.service.object.OAObjectLockService", type.getName());
        assertEquals("com.viaoa.oa.service.object", type.getPackageName());
        assertEquals("OAObjectLockService", type.getSimpleName());
    }

    @Test
    void declaredContractMatchesExpectedKind() {
        Class<?> type = OAObjectLockService.class;

        assertFalse(type.isInterface(), "OAObjectLockService should remain a concrete or abstract class");
        assertFalse(type.isEnum(), "OAObjectLockService should not become an enum");
        assertTrue(type.getDeclaredConstructors().length > 0, "constructors should be discoverable");
    }

    @Test
    void productionTypeIsNotSyntheticTestArtifact() {
        Class<?> type = OAObjectLockService.class;

        assertFalse(type.isAnonymousClass());
        assertFalse(type.isLocalClass());
        assertFalse(type.isSynthetic());
        assertFalse(Modifier.isPrivate(type.getModifiers()), "top-level OA API type should not be private");
    }
}
