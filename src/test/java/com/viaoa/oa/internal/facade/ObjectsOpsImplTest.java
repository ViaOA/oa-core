package com.viaoa.oa.internal.facade;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

class ObjectsOpsImplTest {

    @Test
    void typeIdentityMatchesProductionClass() {
        Class<?> type = ObjectsOpsImpl.class;

        assertEquals("com.viaoa.oa.internal.facade.ObjectsOpsImpl", type.getName());
        assertEquals("com.viaoa.oa.internal.facade", type.getPackageName());
        assertEquals("ObjectsOpsImpl", type.getSimpleName());
    }

    @Test
    void declaredContractMatchesExpectedKind() {
        Class<?> type = ObjectsOpsImpl.class;

        assertFalse(type.isInterface(), "ObjectsOpsImpl should remain a concrete or abstract class");
        assertFalse(type.isEnum(), "ObjectsOpsImpl should not become an enum");
        assertTrue(type.getDeclaredConstructors().length > 0, "constructors should be discoverable");
    }

    @Test
    void productionTypeIsNotSyntheticTestArtifact() {
        Class<?> type = ObjectsOpsImpl.class;

        assertFalse(type.isAnonymousClass());
        assertFalse(type.isLocalClass());
        assertFalse(type.isSynthetic());
        assertFalse(Modifier.isPrivate(type.getModifiers()), "top-level OA API type should not be private");
    }
}
