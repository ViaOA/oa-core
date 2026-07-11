package com.viaoa.oa.internal.facade;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

class HubsOpsImplTest {

    @Test
    void typeIdentityMatchesProductionClass() {
        Class<?> type = HubsOpsImpl.class;

        assertEquals("com.viaoa.oa.internal.facade.HubsOpsImpl", type.getName());
        assertEquals("com.viaoa.oa.internal.facade", type.getPackageName());
        assertEquals("HubsOpsImpl", type.getSimpleName());
    }

    @Test
    void declaredContractMatchesExpectedKind() {
        Class<?> type = HubsOpsImpl.class;

        assertFalse(type.isInterface(), "HubsOpsImpl should remain a concrete or abstract class");
        assertFalse(type.isEnum(), "HubsOpsImpl should not become an enum");
        assertTrue(type.getDeclaredConstructors().length > 0, "constructors should be discoverable");
    }

    @Test
    void productionTypeIsNotSyntheticTestArtifact() {
        Class<?> type = HubsOpsImpl.class;

        assertFalse(type.isAnonymousClass());
        assertFalse(type.isLocalClass());
        assertFalse(type.isSynthetic());
        assertFalse(Modifier.isPrivate(type.getModifiers()), "top-level OA API type should not be private");
    }
}
