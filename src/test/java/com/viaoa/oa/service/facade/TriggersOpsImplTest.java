package com.viaoa.oa.service.facade;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

class TriggersOpsImplTest {

    @Test
    void typeIdentityMatchesProductionClass() {
        Class<?> type = TriggersOpsImpl.class;

        assertEquals("com.viaoa.oa.service.facade.TriggersOpsImpl", type.getName());
        assertEquals("com.viaoa.oa.service.facade", type.getPackageName());
        assertEquals("TriggersOpsImpl", type.getSimpleName());
    }

    @Test
    void declaredContractMatchesExpectedKind() {
        Class<?> type = TriggersOpsImpl.class;

        assertFalse(type.isInterface(), "TriggersOpsImpl should remain a concrete or abstract class");
        assertFalse(type.isEnum(), "TriggersOpsImpl should not become an enum");
        assertTrue(type.getDeclaredConstructors().length > 0, "constructors should be discoverable");
    }

    @Test
    void productionTypeIsNotSyntheticTestArtifact() {
        Class<?> type = TriggersOpsImpl.class;

        assertFalse(type.isAnonymousClass());
        assertFalse(type.isLocalClass());
        assertFalse(type.isSynthetic());
        assertFalse(Modifier.isPrivate(type.getModifiers()), "top-level OA API type should not be private");
    }
}
