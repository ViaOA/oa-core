package com.viaoa.runtime;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.test.pos.model.oa.Register;
import com.viaoa.runtime.context.OAContext;
import com.viaoa.runtime.context.OAContextAccess;
import com.viaoa.runtime.context.OAContextUser;

class OAContextServiceTest {
    @Test
    void registerGetAndUnregisterAreKeyBasedAndNullSafe() {
        OAContextService service = new OAContextService();
        OAContext<String, Register> context = new OAContext<>("register", new OAContextAccess());

        assertNull(service.get("register"));

        service.register(context);
        assertSame(context, service.get("register"));

        service.register(null);
        assertSame(context, service.get("register"));

        service.unregister("register");
        assertNull(service.get("register"));
    }

    @Test
    void defaultContextUserHasPermissiveSystemDefaults() {
        OAContextUser<?> user = new OAContextService().getDefaultContextUser();

        assertNotNull(user);
        assertFalse(user.isAdmin());
        assertFalse(user.isSuperAdmin());
        assertTrue(user.isEnabled("anything", true, true));
    }
}
