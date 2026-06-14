package com.viaoa.hub.filter;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.test.pos.model.oa.Register;
import com.viaoa.hub.Hub;

class HubFilterTest {
    private static Register register(int id, String code) {
        Register r = new Register();
        r.setId(id);
        r.setCode(code);
        return r;
    }

    @Test
    void constructorInitializesFilteredHubAndGetters() {
        Hub<Register> master = new Hub<>(Register.class);
        Hub<Register> filtered = new Hub<>(Register.class);
        Register keep = register(1, "KEEP");
        Register drop = register(2, "DROP");
        master.add(keep);
        master.add(drop);

        HubFilter<Register> filter = new HubFilter<>(master, filtered, r -> "KEEP".equals(r.getCode()));

        assertSame(filtered, filter.getHub());
        assertSame(master, filter.getMasterHub());
        assertFalse(filter.isSharingAO());
        assertTrue(filter.isUsed(keep));
        assertFalse(filter.isUsed(drop));
        assertEquals(1, filtered.size());
        assertSame(keep, filtered.getAt(0));

        filter.close();
    }

    @Test
    void addFiltersRefreshAndCloseDriveMembership() {
        Hub<Register> master = new Hub<>(Register.class);
        Hub<Register> filtered = new Hub<>(Register.class);
        Register r1 = register(1, "A");
        Register r2 = register(2, "B");

        HubFilter<Register> filter = new HubFilter<>(master, filtered);
        filter.addEqualFilter(Register.P_Code, "A");
        master.add(r1);
        master.add(r2);

        assertEquals(1, filtered.size());
        assertSame(r1, filtered.getAt(0));

        r2.setCode("A");
        filter.refresh(r2);
        assertTrue(filtered.contains(r2));

        filter.close();
        master.add(register(3, "A"));
        assertEquals(2, filtered.size());
    }
}
