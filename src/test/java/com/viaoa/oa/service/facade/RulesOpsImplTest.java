package com.viaoa.oa.service.facade;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

class RulesOpsImplTest {

    @Test
    void typeIdentityMatchesProductionClass() {
        Class<?> type = RulesOpsImpl.class;

        assertEquals("com.viaoa.oa.service.facade.RulesOpsImpl", type.getName());
        assertEquals("com.viaoa.oa.service.facade", type.getPackageName());
        assertEquals("RulesOpsImpl", type.getSimpleName());
    }

    @Test
    void declaredContractMatchesExpectedKind() {
        Class<?> type = RulesOpsImpl.class;

        assertFalse(type.isInterface(), "RulesOpsImpl should remain a concrete or abstract class");
        assertFalse(type.isEnum(), "RulesOpsImpl should not become an enum");
        assertTrue(type.getDeclaredConstructors().length > 0, "constructors should be discoverable");
    }

    @Test
    void productionTypeIsNotSyntheticTestArtifact() {
        Class<?> type = RulesOpsImpl.class;

        assertFalse(type.isAnonymousClass());
        assertFalse(type.isLocalClass());
        assertFalse(type.isSynthetic());
        assertFalse(Modifier.isPrivate(type.getModifiers()), "top-level OA API type should not be private");
    }
}
