package com.viaoa.oa;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

class OAImplTest {

    @Test
    void typeIdentityMatchesProductionClass() {
        Class<?> type = OAImpl.class;

        assertEquals("com.viaoa.oa.OAImpl", type.getName());
        assertEquals("com.viaoa.oa", type.getPackageName());
        assertEquals("OAImpl", type.getSimpleName());
    }

    @Test
    void declaredContractMatchesExpectedKind() {
        Class<?> type = OAImpl.class;

        assertFalse(type.isInterface(), "OAImpl should remain a concrete or abstract class");
        assertFalse(type.isEnum(), "OAImpl should not become an enum");
        assertTrue(type.getDeclaredConstructors().length > 0, "constructors should be discoverable");
    }

    @Test
    void productionTypeIsNotSyntheticTestArtifact() {
        Class<?> type = OAImpl.class;

        assertFalse(type.isAnonymousClass());
        assertFalse(type.isLocalClass());
        assertFalse(type.isSynthetic());
        assertFalse(Modifier.isPrivate(type.getModifiers()), "top-level OA API type should not be private");
    }
}
