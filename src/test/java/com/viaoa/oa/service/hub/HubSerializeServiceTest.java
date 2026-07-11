package com.viaoa.oa.service.hub;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

class HubSerializeServiceTest {

    @Test
    void typeIdentityMatchesProductionClass() {
        Class<?> type = HubSerializeService.class;

        assertEquals("com.viaoa.oa.service.hub.HubSerializeService", type.getName());
        assertEquals("com.viaoa.oa.service.hub", type.getPackageName());
        assertEquals("HubSerializeService", type.getSimpleName());
    }

    @Test
    void declaredContractMatchesExpectedKind() {
        Class<?> type = HubSerializeService.class;

        assertFalse(type.isInterface(), "HubSerializeService should remain a concrete or abstract class");
        assertFalse(type.isEnum(), "HubSerializeService should not become an enum");
        assertTrue(type.getDeclaredConstructors().length > 0, "constructors should be discoverable");
    }

    @Test
    void productionTypeIsNotSyntheticTestArtifact() {
        Class<?> type = HubSerializeService.class;

        assertFalse(type.isAnonymousClass());
        assertFalse(type.isLocalClass());
        assertFalse(type.isSynthetic());
        assertFalse(Modifier.isPrivate(type.getModifiers()), "top-level OA API type should not be private");
    }
}
