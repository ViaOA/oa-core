package com.viaoa.hub.view;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.test.pos.model.oa.Register;
import com.viaoa.hub.Hub;

class HubShareAOTest {
    @Test
    void activeObjectChangesAreMirroredUntilClosed() {
        Hub<Register> hub1 = new Hub<>(Register.class);
        Hub<Register> hub2 = new Hub<>(Register.class);
        Register r1 = new Register();
        Register r2 = new Register();
        hub1.add(r1);
        hub1.add(r2);
        hub2.add(r1);
        hub2.add(r2);

        HubShareAO<Register> share = new HubShareAO<>(hub1, hub2);
        hub1.setAO(r2);
        assertSame(r2, hub2.getAO());
        assertSame(hub1, share.getHub1());
        assertSame(hub2, share.getHub2());

        share.close();
        hub1.setAO(r1);
        assertSame(r2, hub2.getAO());
    }
}
