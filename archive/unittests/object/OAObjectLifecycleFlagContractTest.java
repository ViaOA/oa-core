package com.viaoa.object;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OAObjectLifecycleFlagContractTest {

    static class Item extends OAObject {
    }

    @Test
    void changedFlagCanBeToggledWithoutChangingNewFlag() {
        Item item = new Item();
        assertTrue(item.isNew());

        item.setChanged(false);

        assertFalse(item.isChanged());
        assertTrue(item.isNew());

        item.setChanged(true);

        assertTrue(item.isChanged());
        assertTrue(item.isNew());
    }

    @Test
    void newFlagCanBeToggledWithoutChangingChangedFlag() {
        Item item = new Item();
        assertTrue(item.isChanged());

        item.setNew(false);

        assertFalse(item.isNew());
        assertTrue(item.isChanged());

        item.setNew(true);

        assertTrue(item.isNew());
        assertTrue(item.isChanged());
    }

    @Test
    void deletedFlagCanBeToggledWithoutChangingGuidOrObjectKey() {
        Item item = new Item();
        var guid = item.getGuid();
        var key = item.getObjectKey();

        item.setDeleted(true);

        assertTrue(item.isDeleted());
        assertEquals(guid, item.getGuid());
        assertEquals(key, item.getObjectKey());

        item.setDeleted(false);

        assertFalse(item.isDeleted());
        assertEquals(guid, item.getGuid());
        assertEquals(key, item.getObjectKey());
    }

    @Test
    void deletedAndChangedAreIndependentFlagsCurrentContract() {
        Item item = new Item();
        item.setChanged(false);

        item.setDeleted(true);

        assertTrue(item.isDeleted());
        assertFalse(item.isChanged(),
            "setDeleted alone should not silently claim changed unless lifecycle service explicitly does so");
    }

    @Test
    void submitStateDefaultsFalseAndCanBeResetSafely() {
        Item item = new Item();

        assertFalse(item._isSubmitted());

        item._setSubmitted(true);
        assertTrue(item._isSubmitted());

        item._setSubmitted(false);
        assertFalse(item._isSubmitted());
    }

    @Test
    void loadingStateDefaultsFalseAndCanBeSetSafely() {
        Item item = new Item();

        assertFalse(item.isLoading());

        item.setLoading(true);
        assertTrue(item.isLoading());

        item.setLoading(false);
        assertFalse(item.isLoading());
    }
}
