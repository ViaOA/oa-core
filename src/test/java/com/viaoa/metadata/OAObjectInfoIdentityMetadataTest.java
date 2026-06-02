package com.viaoa.metadata;

import static org.junit.jupiter.api.Assertions.*;

import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OAObjectInfoIdentityMetadataTest {

    public static class Order extends OAObject {
    }

    @Test
    void idPropertiesAndKeyPropertiesShareSameContractByDefault() {
        OAObjectInfo oi = new OAObjectInfo(new String[] { "Id", "StoreId" });

        assertArrayEquals(new String[] { "Id", "StoreId" }, oi.getIdProperties());
        assertArrayEquals(new String[] { "Id", "StoreId" }, oi.getKeyProperties());

        assertTrue(oi.isIdProperty("id"));
        assertTrue(oi.isIdProperty("STOREID"));
        assertTrue(oi.isKeyProperty("id"));
        assertTrue(oi.isKeyProperty("storeid"));
    }

    @Test
    void settingIdPropertiesUpdatesIdentityChecks() {
        OAObjectInfo oi = new OAObjectInfo();

        assertFalse(oi.isIdProperty("id"));
        assertFalse(oi.isKeyProperty("id"));

        oi.setIdProperties(new String[] { "Id" });

        assertTrue(oi.isIdProperty("id"));
        assertTrue(oi.isKeyProperty("ID"));
        assertArrayEquals(new String[] { "Id" }, oi.getIdProperties());
        assertArrayEquals(new String[] { "Id" }, oi.getKeyProperties());
    }

    @Test
    void propertyInfoIdFlagCanBeQueriedIndependentlyOfIdProperties() {
        OAObjectInfo oi = new OAObjectInfo(new String[] { "id" });

        OAPropertyInfo id = new OAPropertyInfo();
        id.setName("id");
        id.setId(true);
        oi.addPropertyInfo(id);

        OAPropertyInfo name = new OAPropertyInfo();
        name.setName("name");
        oi.addPropertyInfo(name);

        assertTrue(oi.getPropertyInfo("id").getId());
        assertFalse(oi.getPropertyInfo("name").getId());
        assertTrue(oi.isIdProperty("id"));
    }

    @Test
    void emptyAndNullIdPropertiesAreSafe() {
        OAObjectInfo oi = new OAObjectInfo();

        oi.setIdProperties(null);

        assertNull(oi.getIdProperties());
        assertNull(oi.getKeyProperties());
        assertFalse(oi.isIdProperty("id"));
        assertFalse(oi.isKeyProperty("id"));
        assertFalse(oi.isIdProperty(null));

        oi.setIdProperties(new String[0]);

        assertArrayEquals(new String[0], oi.getIdProperties());
        assertFalse(oi.isIdProperty("id"));
    }

    @Test
    void guidMetadataFlagsRoundTrip() {
        OAObjectInfo oi = new OAObjectInfo();

        assertFalse(oi.getGuidIsStored());

        oi.setGuidIsStored(true);
        assertTrue(oi.getGuidIsStored());

        oi.setGuidIsStored(false);
        assertFalse(oi.getGuidIsStored());
    }

    @Test
    void datasourceAndCacheAuthorityFlagsRoundTrip() {
        OAObjectInfo oi = new OAObjectInfo();

        assertTrue(oi.getUseDataSource());
        assertTrue(oi.getAddToCache());
        assertTrue(oi.getInitializeNewObjects());
        assertFalse(oi.getLocalOnly());

        oi.setUseDataSource(false);
        oi.setAddToCache(false);
        oi.setInitializeNewObjects(false);
        oi.setLocalOnly(true);

        assertFalse(oi.getUseDataSource());
        assertFalse(oi.getAddToCache());
        assertFalse(oi.getInitializeNewObjects());
        assertTrue(oi.getLocalOnly());
    }

    @Test
    void classIdentityAndNameDefaultsAreStable() {
        OAObjectInfo oi = new OAObjectInfo();
        oi.setForClass(Order.class);

        assertEquals(Order.class, oi.getForClass());

        oi.setName("Order");
        assertEquals("Order", oi.getName());
        assertEquals("order", oi.getLowerName());

        assertNotNull(oi.getDisplayName());
        assertNotNull(oi.getPluralName());
    }
}
