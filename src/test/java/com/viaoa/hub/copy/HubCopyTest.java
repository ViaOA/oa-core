package com.viaoa.hub.copy;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.test.pos.model.oa.Register;
import com.viaoa.hub.Hub;

class HubCopyTest {
    @Test
    void copyHubMirrorsMasterAndCanShareActiveObject() {
        Hub<Register> master = new Hub<>(Register.class);
        Hub<Register> copy = new Hub<>(Register.class);
        Register r1 = new Register();
        Register r2 = new Register();

        HubCopy<Register> hubCopy = new HubCopy<>(master, copy, true);
        master.add(r1);
        master.add(r2);

        assertEquals(master.toList(), copy.toList());
        master.setAO(r2);
        assertSame(r2, copy.getAO());
        assertTrue(hubCopy.isUsed(r1));

        copy.remove(r1);
        assertFalse(master.contains(r1));

        hubCopy.close();
    }
}
