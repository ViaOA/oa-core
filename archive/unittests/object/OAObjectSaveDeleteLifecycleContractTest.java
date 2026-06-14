package com.viaoa.object;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OAObjectSaveDeleteLifecycleContractTest {

    static class Item extends OAObject {
    }

    @Test
    void saveWithoutDatasourceFailsOrPreservesNewChangedStateDesiredContract() {
        Item item = new Item();

        boolean wasNew = item.isNew();
        boolean wasChanged = item.isChanged();

        try {
            item.save();
        } catch (RuntimeException ex) {
            assertEquals(wasNew, item.isNew(), "failed save must not clear new flag");
            assertEquals(wasChanged, item.isChanged(), "failed save must not clear changed flag");
            return;
        }

        assertFalse(item.isChanged(), "successful save should clear changed flag");
    }

    @Test
    void saveCascadeNoneHasSameFalseSuccessBoundary() {
        Item item = new Item();

        boolean wasNew = item.isNew();
        boolean wasChanged = item.isChanged();

        try {
            item.save(OAObject.CASCADE_NONE);
        } catch (RuntimeException ex) {
            assertEquals(wasNew, item.isNew());
            assertEquals(wasChanged, item.isChanged());
        }
    }

    @Test
    void deleteWithoutDatasourceFailsOrMarksDeletedConsistentlyDesiredContract() {
        Item item = new Item();

        try {
            item.delete();
        } catch (RuntimeException ex) {
            assertFalse(item.isDeleted(), "failed delete must not report completed deleted state");
            return;
        }

        assertTrue(item.isDeleted(), "successful delete should mark completed deleted state");
    }

    @Test
    void deleteCascadeNoneHasSameFalseSuccessBoundary() {
        Item item = new Item();

        try {
            item.delete(OAObject.CASCADE_NONE);
        } catch (RuntimeException ex) {
            assertFalse(item.isDeleted());
            return;
        }

        assertTrue(item.isDeleted());
    }

    @Test
    void afterSaveAndAfterDeleteCallbacksAreSafeForSimpleObject() {
        Item item = new Item();

        assertDoesNotThrow(item::afterSave);
        assertDoesNotThrow(item::afterDelete);
    }

    @Test
    void canSaveAndCanDeleteSurfacesAreSafe() {
        Item item = new Item();

        assertDoesNotThrow(item::canSave);
        assertDoesNotThrow(item::canDelete);
        assertNotNull(item.getCanSaveObjectCallback());
        assertNotNull(item.getCanDeleteObjectCallback());
    }
}
