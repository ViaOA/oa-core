package com.viaoa.runtime.context;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.test.pos.model.oa.Register;

class OAContextTest {
    @Test
    void constructorAndAccessorsExposeKeyAccessAndDefaultPaths() {
        OAContextAccess access = new OAContextAccess();
        OAContext<String, Register> context = new OAContext<>("register", access);

        assertEquals("register", context.getKey());
        assertSame(access, context.getContextAccess());
        assertEquals("Admin", context.getAdminPath());
        assertEquals("SuperAdmin", context.getSuperAdminPath());
        assertEquals("EditProcessed", context.getAllowEditProcessedPath());
    }

    @Test
    void contextUserMapIgnoresNullKeysAndRemovesNullUsers() {
        OAContext<String, Register> context = new OAContext<>("register", new OAContextAccess());
        OAContextUser<Register> user = new OAContextUser<>(context, new Register());

        context.addContextUser(null, user);
        assertNull(context.getContextUser(null));

        context.addContextUser("terminal-1", user);
        assertSame(user, context.getContextUser("terminal-1"));

        context.addContextUser("terminal-1", null);
        assertNull(context.getContextUser("terminal-1"));
    }

    @Test
    void permissionPathNamesCanBeChanged() {
        OAContext<String, Register> context = new OAContext<>("register", new OAContextAccess());

        context.setAdminPath("isAdmin");
        context.setSuperAdminPath("isSuper");
        context.setAllowEditProcessedPath("canEdit");

        assertEquals("isAdmin", context.getAdminPath());
        assertEquals("isSuper", context.getSuperAdminPath());
        assertEquals("canEdit", context.getAllowEditProcessedPath());
    }
}
