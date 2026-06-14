package com.viaoa.hub.filter;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.test.pos.model.oa.Register;
import com.viaoa.hub.Hub;

class HubMinusHubFilterTest {
    @Test
    void filteredHubContainsMasterObjectsNotInMinusHub() {
        Hub<Register> master = new Hub<>(Register.class);
        Hub<Register> minus = new Hub<>(Register.class);
        Hub<Register> result = new Hub<>(Register.class);
        Register r1 = new Register();
        Register r2 = new Register();

        master.add(r1);
        master.add(r2);
        minus.add(r2);

        new HubMinusHubFilter(master, minus, result);

        assertEquals(1, result.size());
        assertSame(r1, result.getAt(0));

        minus.remove(r2);
        assertTrue(result.contains(r2));

        master.remove(r1);
        assertFalse(result.contains(r1));
    }
}
