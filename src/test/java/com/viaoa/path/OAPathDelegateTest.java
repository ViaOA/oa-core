package com.viaoa.path;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

import com.test.pos.model.oa.Address;
import com.test.pos.model.oa.Store;
import com.viaoa.hub.Hub;

class OAPathDelegateTest {

    @Test
    void typeIdentityMatchesProductionClass() {
        Class<?> type = OAPathDelegate.class;

        assertEquals("com.viaoa.path.OAPathDelegate", type.getName());
        assertEquals("com.viaoa.path", type.getPackageName());
        assertEquals("OAPathDelegate", type.getSimpleName());
    }

    @Test
    void declaredContractMatchesExpectedKind() {
        Class<?> type = OAPathDelegate.class;

        assertFalse(type.isInterface(), "OAPathDelegate should remain a concrete or abstract class");
        assertFalse(type.isEnum(), "OAPathDelegate should not become an enum");
        assertTrue(type.getDeclaredConstructors().length > 0, "constructors should be discoverable");
    }

    @Test
    void productionTypeIsNotSyntheticTestArtifact() {
        Class<?> type = OAPathDelegate.class;

        assertFalse(type.isAnonymousClass());
        assertFalse(type.isLocalClass());
        assertFalse(type.isSynthetic());
        assertFalse(Modifier.isPrivate(type.getModifiers()), "top-level OA API type should not be private");
    }

    @Test
    void getPathForClassesUsesPosModelLinkMetadata() {
        Hub<Store> stores = new Hub<>(Store.class);

        String path = OAPathDelegate.getPathforClasses(stores, new Class[] { Address.class });

        assertEquals(Store.P_Address, path);
    }

    @Test
    void getPathForClassesReturnsNullForMissingLink() {
        Hub<Address> addresses = new Hub<>(Address.class);

        String path = OAPathDelegate.getPathforClasses(addresses, new Class[] { Store.class, Store.class });

        assertNull(path);
    }
}
