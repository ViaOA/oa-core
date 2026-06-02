package com.viaoa.object;

import static org.junit.jupiter.api.Assertions.*;

import com.viaoa.callback.OAObjectCallback;

import org.junit.jupiter.api.Test;

class OAObjectCallbackSurfaceContractTest {

    static class Item extends OAObject {
        private String name;
        public String getName() { return name; }
        public void setName(String name) {
            String old = this.name;
            this.name = name;
            firePropertyChange("name", old, name);
        }
    }

    @Test
    void enabledAndVisibleDefaultTrueForSimpleObject() {
        Item item = new Item();

        assertTrue(item.isEnabled());
        assertTrue(item.isEnabled("name"));
        assertTrue(item.isVisible());
        assertTrue(item.isVisible("name"));
    }

    @Test
    void callbackObjectsAreReturnedForEnableAndVisibleChecks() {
        Item item = new Item();

        assertNotNull(item.getIsEnabledObjectCallback());
        assertNotNull(item.getIsEnabledObjectCallback("name", null, "A"));
        assertNotNull(item.getIsVisibleObjectCallback());
        assertNotNull(item.getIsVisibleObjectCallback("name", null, "A"));
    }

    @Test
    void validPropertyChangeCallbackObjectIsReturned() {
        Item item = new Item();

        OAObjectCallback cb = item.getIsValidPropertyChangeObjectCallback("name", null, "A");

        assertNotNull(cb);
    }

    @Test
    void verifyCommandDefaultsAllowOrReturnsCallbackCurrentContract() {
        Item item = new Item();

        assertDoesNotThrow(() -> item.verifyCommand("test"));
        assertNotNull(item.getVerifyCommandObjectCallback("test"));
    }

    @Test
    void allowSubmitAndVerifySaveSurfacesAreSafeForSimpleObject() {
        Item item = new Item();

        assertDoesNotThrow(item::getAllowSubmit);
        assertNotNull(item.getAllowSubmitObjectCallback());

        assertDoesNotThrow(item::getVerifySave);
        assertNotNull(item.getVerifySaveObjectCallback());
    }
}
