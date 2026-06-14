package com.viaoa.runtime.context;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.test.pos.model.oa.Register;
import com.viaoa.select.OASelect;

class OAContextAccessTest {
    @Test
    void queryHooksAreNoOpsByDefault() {
        OAContextAccess access = new OAContextAccess();

        assertFalse(access.updateSelect(new OASelect<>(Register.class)));
        assertNull(access.getExtraWhereClause(Register.class));
    }

    @Test
    void classAndPropertyVisibilityRulesCanAllowAndDenyAccess() {
        OAContextAccess access = new OAContextAccess();
        Register register = new Register();

        assertTrue(access.getVisible(register, Register.class, Register.P_Code, true));
        assertTrue(access.getEnabled(register, Register.class, Register.P_Code, true));

        access.addNotVisible(Register.class, Register.P_Code);
        access.addNotEnabled(Register.class, Register.P_Code);

        assertFalse(access.getVisible(register, Register.class, Register.P_Code, true));
        assertFalse(access.getEnabled(register, Register.class, Register.P_Code, true));

        access.addVisible(Register.class, Register.P_Code);
        access.addEnabled(Register.class, Register.P_Code);

        assertTrue(access.getVisible(register, Register.class, Register.P_Code, false));
        assertTrue(access.getEnabled(register, Register.class, Register.P_Code, false));
    }
}
