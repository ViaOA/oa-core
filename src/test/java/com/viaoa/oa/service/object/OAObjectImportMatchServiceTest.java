package com.viaoa.oa.service.object;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

class OAObjectImportMatchServiceTest {

    @Test
    void typeIdentityMatchesProductionClass() {
        Class<?> type = OAObjectImportMatchService.class;

        assertEquals("com.viaoa.oa.service.object.OAObjectImportMatchService", type.getName());
        assertEquals("com.viaoa.oa.service.object", type.getPackageName());
        assertEquals("OAObjectImportMatchService", type.getSimpleName());
    }

    @Test
    void declaredContractMatchesExpectedKind() {
        Class<?> type = OAObjectImportMatchService.class;

        assertFalse(type.isInterface(), "OAObjectImportMatchService should remain a concrete or abstract class");
        assertFalse(type.isEnum(), "OAObjectImportMatchService should not become an enum");
        assertTrue(type.getDeclaredConstructors().length > 0, "constructors should be discoverable");
    }

    @Test
    void productionTypeIsNotSyntheticTestArtifact() {
        Class<?> type = OAObjectImportMatchService.class;

        assertFalse(type.isAnonymousClass());
        assertFalse(type.isLocalClass());
        assertFalse(type.isSynthetic());
        assertFalse(Modifier.isPrivate(type.getModifiers()), "top-level OA API type should not be private");
    }
}
