package com.viaoa.object;

import static org.junit.jupiter.api.Assertions.*;

import com.viaoa.callback.OAObjectCallback;

import org.junit.jupiter.api.Test;

class OAObjectValidationVisibilityCommandFinalTest {

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
    void defaultValidationDoesNotMutateObjectState() {
        Item item = new Item();
        boolean newFlag = item.isNew();
        boolean changedFlag = item.isChanged();

        assertTrue(item.isValidPropertyChange("name", null, "A"));

        assertNull(item.getName());
        assertEquals(newFlag, item.isNew());
        assertEquals(changedFlag, item.isChanged());
    }

    @Test
    void validationCallbackObjectIsDeterministicAndNonNull() {
        Item item = new Item();

        OAObjectCallback a = item.getIsValidPropertyChangeObjectCallback("name", null, "A");
        OAObjectCallback b = item.getIsValidPropertyChangeObjectCallback("name", null, "A");

        assertNotNull(a);
        assertNotNull(b);
        assertEquals(a.getAllowed(), b.getAllowed());
    }

    @Test
    void enabledVisibleDefaultsAreTrueAndCallbacksNonNull() {
        Item item = new Item();

        assertTrue(item.isEnabled());
        assertTrue(item.isEnabled("name"));
        assertTrue(item.isVisible());
        assertTrue(item.isVisible("name"));

        assertNotNull(item.getIsEnabledObjectCallback());
        assertNotNull(item.getIsEnabledObjectCallback("name", null, "A"));
        assertNotNull(item.getIsVisibleObjectCallback());
        assertNotNull(item.getIsVisibleObjectCallback("name", null, "A"));
    }

    @Test
    void verifyCommandDoesNotMutateObjectState() {
        Item item = new Item();
        boolean newFlag = item.isNew();
        boolean changedFlag = item.isChanged();

        assertDoesNotThrow(() -> item.verifyCommand("doSomething"));
        assertNotNull(item.getVerifyCommandObjectCallback("doSomething"));

        assertEquals(newFlag, item.isNew());
        assertEquals(changedFlag, item.isChanged());
    }

    @Test
    void allowSubmitVerifySaveAndDeleteCallbacksAreNonNullAndSafe() {
        Item item = new Item();

        assertDoesNotThrow(item::getAllowSubmit);
        assertDoesNotThrow(item::getVerifySave);
        assertDoesNotThrow(item::canSave);
        assertDoesNotThrow(item::canDelete);

        assertNotNull(item.getAllowSubmitObjectCallback());
        assertNotNull(item.getVerifySaveObjectCallback());
        assertNotNull(item.getCanSaveObjectCallback());
        assertNotNull(item.getCanDeleteObjectCallback());
    }

    @Test
    void commandCallbackForNullCommandIsSafe() {
        Item item = new Item();

        assertDoesNotThrow(() -> item.verifyCommand(null));
        assertNotNull(item.getVerifyCommandObjectCallback(null));
    }
}
