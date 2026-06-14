package com.viaoa.hub.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.test.pos.model.oa.Register;
import com.viaoa.hub.Hub;

class HubTempTest {
    @Test
    void createHubCachesTemporaryHubWithReferenceCount() {
        Register register = new Register();

        assertNull(HubTemp.createHub(null));
        assertEquals(0, HubTemp.getCount(null));

        Hub hub1 = HubTemp.createHub(register);
        Hub hub2 = HubTemp.createHub(register);

        assertSame(hub1, hub2);
        assertSame(register, hub1.getAt(0));
        assertSame(register, hub1.getAO());
        assertEquals(2, HubTemp.getCount(register));

        HubTemp.deleteHub(register);
        assertEquals(1, HubTemp.getCount(register));
        HubTemp.deleteHub(register);
        assertEquals(0, HubTemp.getCount(register));
    }
}
