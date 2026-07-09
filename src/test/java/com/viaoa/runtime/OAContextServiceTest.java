package com.viaoa.runtime;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.test.pos.model.oa.Register;
import com.viaoa.session.OAContext;
import com.viaoa.session.OASessionAccess;
import com.viaoa.session.OASessionUser;

class OAContextServiceTest {
    @Test
    void registerGetAndUnregisterAreKeyBasedAndNullSafe() {
        OAContextService service = new OAContextService();
        OAContext<String, Register> context = new OAContext<>("register", new OASessionAccess());

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
        OASessionUser<?> user = new OAContextService().getDefaultContextUser();

        assertNotNull(user);
        assertFalse(user.isAdmin());
        assertFalse(user.isSuperAdmin());
        assertTrue(user.isEnabled("anything", true, true));
    }
}
