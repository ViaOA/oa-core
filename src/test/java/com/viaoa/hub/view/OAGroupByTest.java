package com.viaoa.hub.view;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.test.pos.model.oa.Register;
import com.test.pos.model.oa.Store;

class OAGroupByTest {
    @Test
    void constructorsAndAccessorsManageGroupAndDetailHub() {
        Store store = new Store();
        OAGroupBy<Register, Store> group = new OAGroupBy<>(store);

        assertSame(store, group.getGroupBy());
        assertNotNull(group.getHub());

        Store store2 = new Store();
        group.setGroupBy(store2);
        assertSame(store2, group.getGroupBy());
    }
}
