package com.viaoa.oa.sibling;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

class OASiblingHelperTest {

    @Test
    void typeIdentityMatchesProductionClass() {
        Class<?> type = OASiblingHelper.class;

        assertEquals("com.viaoa.oa.sibling.OASiblingHelper", type.getName());
        assertEquals("com.viaoa.oa.sibling", type.getPackageName());
        assertEquals("OASiblingHelper", type.getSimpleName());
    }

    @Test
    void declaredContractMatchesExpectedKind() {
        Class<?> type = OASiblingHelper.class;

        assertFalse(type.isInterface(), "OASiblingHelper should remain a concrete or abstract class");
        assertFalse(type.isEnum(), "OASiblingHelper should not become an enum");
        assertTrue(type.getDeclaredConstructors().length > 0, "constructors should be discoverable");
    }

    @Test
    void productionTypeIsNotSyntheticTestArtifact() {
        Class<?> type = OASiblingHelper.class;

        assertFalse(type.isAnonymousClass());
        assertFalse(type.isLocalClass());
        assertFalse(type.isSynthetic());
        assertFalse(Modifier.isPrivate(type.getModifiers()), "top-level OA API type should not be private");
    }
}
