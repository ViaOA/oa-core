package com.viaoa.oa.service.object;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

class OAObjectEmptyHubServiceTest {

    @Test
    void typeIdentityMatchesProductionClass() {
        Class<?> type = OAObjectEmptyHubService.class;

        assertEquals("com.viaoa.oa.service.object.OAObjectEmptyHubService", type.getName());
        assertEquals("com.viaoa.oa.service.object", type.getPackageName());
        assertEquals("OAObjectEmptyHubService", type.getSimpleName());
    }

    @Test
    void declaredContractMatchesExpectedKind() {
        Class<?> type = OAObjectEmptyHubService.class;

        assertFalse(type.isInterface(), "OAObjectEmptyHubService should remain a concrete or abstract class");
        assertFalse(type.isEnum(), "OAObjectEmptyHubService should not become an enum");
        assertTrue(type.getDeclaredConstructors().length > 0, "constructors should be discoverable");
    }

    @Test
    void productionTypeIsNotSyntheticTestArtifact() {
        Class<?> type = OAObjectEmptyHubService.class;

        assertFalse(type.isAnonymousClass());
        assertFalse(type.isLocalClass());
        assertFalse(type.isSynthetic());
        assertFalse(Modifier.isPrivate(type.getModifiers()), "top-level OA API type should not be private");
    }
}
