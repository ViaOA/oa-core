package com.viaoa.hub.view;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.test.pos.model.oa.Register;
import com.test.pos.model.oa.Store;

class OALeftJoinTest {
    @Test
    void constructorsAndAccessorsManageBothSides() {
        Store store = new Store();
        Register register = new Register();

        OALeftJoin<Store, Register> join = new OALeftJoin<>(store, register);
        assertSame(store, join.getA());
        assertSame(register, join.getB());

        Store store2 = new Store();
        Register register2 = new Register();
        join.setA(store2);
        join.setB(register2);
        assertSame(store2, join.getA());
        assertSame(register2, join.getB());
    }
}
