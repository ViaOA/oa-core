package com.viaoa.object;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class OAObjectBaselineStateTest {

    static class Item extends OAObject {
    }

    @Test
    void newObjectHasGuidAndNewChangedLifecycleState() {
        Item item = new Item();

        assertNotNull(item.getGuid());
        assertTrue(item.isNew());
        assertTrue(item.getNew());
        assertTrue(item.isChanged());
        assertTrue(item.getChanged());
        assertFalse(item.isDeleted());
        assertFalse(item.getDeleted());
        assertFalse(item.wasDeleted());
    }

    @Test
    void eachNewObjectGetsDistinctGuid() {
        Item a = new Item();
        Item b = new Item();

        assertNotEquals(a.getGuid(), b.getGuid());
    }

    @Test
    void objectKeyContainsGuidForRuntimeIdentity() {
        Item item = new Item();

        OAObjectKey key = item.getObjectKey();

        assertNotNull(key);
        assertEquals(item.getGuid(), key.getGuid());
    }

    @Test
    void setChangedFlagRoundTrips() {
        Item item = new Item();

        item.setChanged(false);
        assertFalse(item.isChanged());

        item.setChanged(true);
        assertTrue(item.isChanged());
    }

    @Test
    void setNewFlagRoundTrips() {
        Item item = new Item();

        item.setNew(false);
        assertFalse(item.isNew());

        item.setNew(true);
        assertTrue(item.isNew());
    }

    @Test
    void setDeletedFlagRoundTrips() {
        Item item = new Item();

        item.setDeleted(true);
        assertTrue(item.isDeleted());
        assertTrue(item.getDeleted());

        item.setDeleted(false);
        assertFalse(item.isDeleted());
    }

    @Test
    void compareToUsesObjectIdentityOrderingAndHandlesNull() {
        Item a = new Item();
        Item b = new Item();

        assertEquals(0, a.compareTo(a));
        assertTrue(a.compareTo(null) > 0);

        int ab = a.compareTo(b);
        int ba = b.compareTo(a);

        assertTrue(ab != 0);
        assertEquals(Integer.signum(ab), -Integer.signum(ba));
    }

    @Test
    void equalsUsesIdentityByDefault() {
        Item a = new Item();
        Item b = new Item();

        assertEquals(a, a);
        assertNotEquals(a, b);
        assertNotEquals(a, null);
    }

    @Test
    void hashCodeStableForObjectLifetime() {
        Item item = new Item();

        int h1 = item.hashCode();
        int h2 = item.hashCode();

        assertEquals(h1, h2);
    }

    @Test
    void oaVersionIsAvailable() {
        String version = OAObject.getOAVersion();

        assertNotNull(version);
        assertFalse(version.isBlank());
    }

    @Test
    void cntNewIncrementsWhenObjectsAreConstructed() {
        int before = OAObject.cntNew.get();

        new Item();
        new Item();

        assertTrue(OAObject.cntNew.get() >= before + 2);
    }
}
