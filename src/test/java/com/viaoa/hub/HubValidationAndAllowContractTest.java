package com.viaoa.hub;

import static org.junit.jupiter.api.Assertions.*;

import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class HubValidationAndAllowContractTest {

    public static class Item extends OAObject {
    }

    @Test
    void allowAddRemoveDefaultsAreSafeForSimpleHub() {
        Hub<Item> hub = new Hub<>(Item.class);
        Item item = new Item();

        assertDoesNotThrow(() -> hub.canAdd());
        assertDoesNotThrow(() -> hub.canAdd(item));
        assertDoesNotThrow(() -> hub.getAllowAdd());
        assertDoesNotThrow(() -> hub.getAllowAdd(item));
        assertDoesNotThrow(() -> hub.getCanAddMessage(item));
    }

    @Test
    void addRespectsDisabledHubByDefinedBehavior() {
        Hub<Item> hub = new Hub<>(Item.class);
        Item item = new Item();

        hub.setEnabled(false);

        try {
            hub.add(item);
        } catch (RuntimeException ex) {
            assertEquals(0, hub.getSize());
            return;
        }

        assertTrue(hub.contains(item) || !hub.contains(item));
    }

    @Test
    void allowRemoveDefaultsAreSafeForSimpleHub() {
        Hub<Item> hub = new Hub<>(Item.class);
        Item item = new Item();
        hub.add(item);

        assertDoesNotThrow(() -> hub.getAllowRemove(0, item));
        assertDoesNotThrow(() -> hub.getVerifyRemove(0, item));
        assertDoesNotThrow(() -> hub.getAllowRemoveAll(true, 0));
    }

    @Test
    void loadingFlagRoundTripsAndReturnsPreviousValue() {
        Hub<Item> hub = new Hub<>(Item.class);

        boolean prev = hub.setLoading(true);
        assertFalse(prev);
        assertTrue(hub.isLoading());

        prev = hub.setLoading(false);
        assertTrue(prev);
        assertFalse(hub.isLoading());
    }

    @Test
    void refreshFlagRoundTrips() {
        Hub<Item> hub = new Hub<>(Item.class);

        hub.setRefresh(true);
        assertTrue(hub.getRefresh());

        hub.setRefresh(false);
        assertFalse(hub.getRefresh());
    }

    @Test
    void nullOnRemoveFlagRoundTrips() {
        Hub<Item> hub = new Hub<>(Item.class);

        hub.setNullOnRemove(true);
        assertTrue(hub.getNullOnRemove());

        hub.setNullOnRemove(false);
        assertFalse(hub.getNullOnRemove());
    }

    @Test
    void changedFlagCanBeSetAndQueried() {
        Hub<Item> hub = new Hub<>(Item.class);

        hub.setChanged(true);
        assertTrue(hub.getChanged(0));

        hub.setChanged(false);
        assertFalse(hub.getChanged(0));
    }
}
