package com.viaoa.runtime.context;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.test.pos.model.oa.Item;
import com.viaoa.hub.Hub;
import com.viaoa.session.OAContext;
import com.viaoa.session.OASessionAccess;
import com.viaoa.session.OASessionUser;

class OAContextUserTest {
    @Test
    void constructorsExposeContextObjectAndHubSources() {
        OAContext<String, Item> context = new OAContext<>("item", new OASessionAccess());
        Item item = new Item();

        OASessionUser<Item> objectUser = new OASessionUser<>(context, item);
        assertSame(context, objectUser.getContext());
        assertSame(item, objectUser.getUserObject());
        assertSame(item, objectUser.getCurrentUserObject());
        assertNull(objectUser.getUserHub());

        Hub<Item> hub = new Hub<>(Item.class);
        Item item2 = new Item();
        hub.add(item2);
        hub.setAO(item2);

        OASessionUser<Item> hubUser = new OASessionUser<>(context, hub);
        assertSame(hub, hubUser.getUserHub());
        assertNull(hubUser.getUserObject());
        assertSame(item2, hubUser.getCurrentUserObject());

        OASessionUser<Item> empty = new OASessionUser<>(context);
        assertNull(empty.getCurrentUserObject());
    }

    @Test
    void booleanPermissionHelpersReadConfiguredUserPropertyPaths() {
        OAContext<String, Item> context = new OAContext<>("item", new OASessionAccess());
        context.setAdminPath(Item.P_Stocking);
        context.setSuperAdminPath(Item.P_AgeRestricted);
        context.setAllowEditProcessedPath(Item.P_NotReturnable);

        Item item = new Item();
        item.setStocking(true);
        item.setAgeRestricted(false);
        item.setNotReturnable(true);
        OASessionUser<Item> user = new OASessionUser<>(context, item);

        assertTrue(user.isAdmin());
        assertFalse(user.isSuperAdmin());
        assertTrue(user.getAllowEditProcessed());
        assertTrue(user.isEnabled(Item.P_Stocking, true));
        assertFalse(user.isEnabled(Item.P_AgeRestricted, true));
    }
}
