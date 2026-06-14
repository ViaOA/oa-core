package com.viaoa.runtime.context;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.test.pos.model.oa.Item;
import com.viaoa.hub.Hub;

class OAContextUserTest {
    @Test
    void constructorsExposeContextObjectAndHubSources() {
        OAContext<String, Item> context = new OAContext<>("item", new OAContextAccess());
        Item item = new Item();

        OAContextUser<Item> objectUser = new OAContextUser<>(context, item);
        assertSame(context, objectUser.getContext());
        assertSame(item, objectUser.getUserObject());
        assertSame(item, objectUser.getCurrentUserObject());
        assertNull(objectUser.getUserHub());

        Hub<Item> hub = new Hub<>(Item.class);
        Item item2 = new Item();
        hub.add(item2);
        hub.setAO(item2);

        OAContextUser<Item> hubUser = new OAContextUser<>(context, hub);
        assertSame(hub, hubUser.getUserHub());
        assertNull(hubUser.getUserObject());
        assertSame(item2, hubUser.getCurrentUserObject());

        OAContextUser<Item> empty = new OAContextUser<>(context);
        assertNull(empty.getCurrentUserObject());
    }

    @Test
    void booleanPermissionHelpersReadConfiguredUserPropertyPaths() {
        OAContext<String, Item> context = new OAContext<>("item", new OAContextAccess());
        context.setAdminPath(Item.P_Stocking);
        context.setSuperAdminPath(Item.P_AgeRestricted);
        context.setAllowEditProcessedPath(Item.P_NotReturnable);

        Item item = new Item();
        item.setStocking(true);
        item.setAgeRestricted(false);
        item.setNotReturnable(true);
        OAContextUser<Item> user = new OAContextUser<>(context, item);

        assertTrue(user.isAdmin());
        assertFalse(user.isSuperAdmin());
        assertTrue(user.getAllowEditProcessed());
        assertTrue(user.isEnabled(Item.P_Stocking, true));
        assertFalse(user.isEnabled(Item.P_AgeRestricted, true));
    }
}
