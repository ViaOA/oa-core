package com.viaoa.hub.detail;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.test.pos.model.oa.Register;
import com.test.pos.model.oa.Store;
import com.viaoa.hub.Hub;

class DetailHubTest {
    @Test
    void propertyPathConstructorTracksActiveMasterObjectDetailHub() {
        Store store1 = new Store();
        Store store2 = new Store();
        Register r1 = new Register();
        Register r2 = new Register();
        store1.getRegisters().add(r1);
        store2.getRegisters().add(r2);

        Hub<Store> stores = new Hub<>(Store.class);
        stores.add(store1);
        stores.add(store2);

        DetailHub<Register> detail = new DetailHub<>(stores, Store.P_Registers);

        stores.setAO(store1);
        assertEquals(1, detail.size());
        assertSame(r1, detail.getAt(0));

        stores.setAO(store2);
        assertEquals(1, detail.size());
        assertSame(r2, detail.getAt(0));
    }
}
