package com.viaoa.hub.view;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.test.pos.model.oa.Register;
import com.viaoa.hub.Hub;

class SharedHubTest {
    @Test
    void constructorsCreateSharedOrStandaloneHub() {
        Hub<Register> source = new Hub<>(Register.class);
        Register r1 = new Register();
        Register r2 = new Register();
        source.add(r1);
        source.add(r2);

        SharedHub<Register> shared = new SharedHub<>(source);
        assertEquals(source.toList(), shared.toList());
        assertEquals(Register.class, shared.getObjectClass());

        source.remove(r1);
        assertEquals(source.toList(), shared.toList());

        SharedHub<Register> shareAo = new SharedHub<>(source, true);
        source.setAO(r2);
        assertSame(r2, shareAo.getAO());

        SharedHub<Register> standalone = new SharedHub<>(Register.class);
        assertEquals(Register.class, standalone.getObjectClass());
        assertTrue(standalone.isEmpty());
    }
}
