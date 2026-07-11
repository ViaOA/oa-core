package com.viaoa.oa.internal.facade;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

class InternalOpsImplTest {

    @Test
    void typeIdentityMatchesProductionClass() {
        Class<?> type = InternalOpsImpl.class;

        assertEquals("com.viaoa.oa.internal.facade.InternalOpsImpl", type.getName());
        assertEquals("com.viaoa.oa.internal.facade", type.getPackageName());
        assertEquals("InternalOpsImpl", type.getSimpleName());
    }

    @Test
    void declaredContractMatchesExpectedKind() {
        Class<?> type = InternalOpsImpl.class;

        assertFalse(type.isInterface(), "InternalOpsImpl should remain a concrete or abstract class");
        assertFalse(type.isEnum(), "InternalOpsImpl should not become an enum");
        assertTrue(type.getDeclaredConstructors().length > 0, "constructors should be discoverable");
    }

    @Test
    void productionTypeIsNotSyntheticTestArtifact() {
        Class<?> type = InternalOpsImpl.class;

        assertFalse(type.isAnonymousClass());
        assertFalse(type.isLocalClass());
        assertFalse(type.isSynthetic());
        assertFalse(Modifier.isPrivate(type.getModifiers()), "top-level OA API type should not be private");
    }
}
