package com.viaoa.path;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

import com.test.pos.model.oa.Address;
import com.test.pos.model.oa.Store;

class OAPathTest {

    @Test
    void typeIdentityMatchesProductionClass() {
        Class<?> type = OAPath.class;

        assertEquals("com.viaoa.path.OAPath", type.getName());
        assertEquals("com.viaoa.path", type.getPackageName());
        assertEquals("OAPath", type.getSimpleName());
    }

    @Test
    void declaredContractMatchesExpectedKind() {
        Class<?> type = OAPath.class;

        assertFalse(type.isInterface(), "OAPath should remain a concrete or abstract class");
        assertFalse(type.isEnum(), "OAPath should not become an enum");
        assertTrue(type.getDeclaredConstructors().length > 0, "constructors should be discoverable");
    }

    @Test
    void productionTypeIsNotSyntheticTestArtifact() {
        Class<?> type = OAPath.class;

        assertFalse(type.isAnonymousClass());
        assertFalse(type.isLocalClass());
        assertFalse(type.isSynthetic());
        assertFalse(Modifier.isPrivate(type.getModifiers()), "top-level OA API type should not be private");
    }

    @Test
    void resolvesNestedValueUsingPosModelPath() {
        Store store = new Store();
        Address address = store.getAddress();
        address.setCity("Austin");

        OAPath<Store> path = new OAPath<>(Store.class, Store.P_Address + "." + Address.P_City);

        assertEquals("address.city", path.getPath());
        assertEquals("Austin", path.getValue(store));
        assertSame(Address.class, path.getEndPropertyInfo().getClassType());
    }

    @Test
    void nullRootReturnsNullForValueLookup() {
        OAPath<Store> path = new OAPath<>(Store.class, Store.P_Address + "." + Address.P_City);

        assertNull(path.getValue(null));
    }
}
