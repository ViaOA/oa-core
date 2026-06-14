package com.viaoa.hub.index;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.test.pos.model.oa.Register;
import com.viaoa.hub.Hub;

class HubUniqueIndexTest {
    private static Register register(int id, String code) {
        Register r = new Register();
        r.setId(id);
        r.setCode(code);
        return r;
    }

    @Test
    void constructorIndexesExistingObjectsAndGetHonorsCaseSensitivity() {
        Hub<Register> hub = new Hub<>(Register.class);
        Register r1 = register(1, "abc");
        hub.add(r1);

        HubUniqueIndex<Register> insensitive = new HubUniqueIndex<>(hub, Register.P_Code);
        assertSame(r1, insensitive.get("ABC"));
        insensitive.close();

        HubUniqueIndex<Register> sensitive = new HubUniqueIndex<>(hub, Register.P_Code, true);
        assertNull(sensitive.get("ABC"));
        assertSame(r1, sensitive.get("abc"));
        sensitive.close();
    }

    @Test
    void indexUpdatesOnAddRemoveAndDirectPropertyChange() {
        Hub<Register> hub = new Hub<>(Register.class);
        Register r1 = register(1, "A");
        HubUniqueIndex<Register> index = new HubUniqueIndex<>(hub, Register.P_Code);

        hub.add(r1);
        assertSame(r1, index.get("A"));

        r1.setCode("B");
        assertNull(index.get("A"));
        assertSame(r1, index.get("B"));

        hub.remove(r1);
        assertNull(index.get("B"));
        index.close();
    }
}
