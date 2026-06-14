package com.viaoa.hub;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.test.pos.model.oa.Register;

class HubDataActiveTest {
    @Test
    void activeObjectCanBeSetAndCleared() {
        HubDataActive<Register> data = new HubDataActive<>();
        Register register = new Register();

        data.setActiveObject(register);
        assertSame(register, data.getActiveObject());

        data.clear(true);
        assertNull(data.getActiveObject());

        data.setActiveObject(register);
        data.clear();
        assertNull(data.getActiveObject());
    }
}
