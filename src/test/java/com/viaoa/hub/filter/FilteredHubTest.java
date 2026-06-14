package com.viaoa.hub.filter;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.test.pos.model.oa.Register;
import com.viaoa.hub.Hub;

class FilteredHubTest {
    @Test
    void subclassFilterControlsMembershipAndCanRefresh() {
        Hub<Register> master = new Hub<>(Register.class);
        Register a = new Register();
        a.setCode("A");
        Register b = new Register();
        b.setCode("B");

        FilteredHub<Register> filtered = new FilteredHub<>(master) {
            @Override
            protected boolean isUsed(Register obj) {
                return obj != null && "A".equals(obj.getCode());
            }
        };

        master.add(a);
        master.add(b);
        assertEquals(1, filtered.size());
        assertSame(a, filtered.getAt(0));

        b.setCode("A");
        filtered.refresh();
        assertEquals(2, filtered.size());
        assertNotNull(filtered.getFilter());
    }
}
