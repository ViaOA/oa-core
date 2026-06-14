package com.viaoa.object;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OAObjectLifecycleCallbackFinalSmokeTest {

    public static class Item extends OAObject {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            String old = this.name;
            this.name = name;
            firePropertyChange("name", old, name);
        }
    }

    @Test
    void callbackSurfacesDoNotChangeIdentityOrFlags() {
        Item item = new Item();
        var guid = item.getGuid();
        boolean newFlag = item.isNew();
        boolean changedFlag = item.isChanged();

        item.getAllowSubmit();
        item.getAllowSubmitObjectCallback();
        item.getVerifySave();
        item.getVerifySaveObjectCallback();
        item.canSave();
        item.getCanSaveObjectCallback();
        item.canDelete();
        item.getCanDeleteObjectCallback();

        assertEquals(guid, item.getGuid());
        assertEquals(newFlag, item.isNew());
        assertEquals(changedFlag, item.isChanged());
    }

    @Test
    void afterSaveDoesNotChangeFlagsByItselfCurrentContract() {
        Item item = new Item();
        boolean newFlag = item.isNew();
        boolean changedFlag = item.isChanged();

        item.afterSave();

        assertEquals(newFlag, item.isNew());
        assertEquals(changedFlag, item.isChanged());
    }

    @Test
    void afterDeleteDoesNotChangeDeletedFlagByItselfCurrentContract() {
        Item item = new Item();

        item.afterDelete();

        assertFalse(item.isDeleted());
    }

    @Test
    void verifyCommandNullAndBlankAreSafe() {
        Item item = new Item();

        assertDoesNotThrow(() -> item.verifyCommand(null));
        assertDoesNotThrow(() -> item.verifyCommand(""));
        assertNotNull(item.getVerifyCommandObjectCallback(null));
        assertNotNull(item.getVerifyCommandObjectCallback(""));
    }

    @Test
    void failedSaveAndDeleteLeaveIdentityUsable() {
        Item item = new Item();
        var guid = item.getGuid();

        try {
            item.save();
        } catch (RuntimeException ex) {
            // expected in no-datasource/simple model scenarios
        }

        assertEquals(guid, item.getGuid());
        assertEquals(guid, item.getObjectKey().getGuid());

        try {
            item.delete();
        } catch (RuntimeException ex) {
            // expected in no-datasource/simple model scenarios
        }

        assertEquals(guid, item.getGuid());
        assertEquals(guid, item.getObjectKey().getGuid());
    }
}
