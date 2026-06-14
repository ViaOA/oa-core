package com.viaoa.graph.sibling;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.test.pos.model.oa.Invoice;
import com.test.pos.model.oa.Register;
import com.test.pos.model.oa.RegisterSession;
import com.test.pos.model.oa.Store;
import com.viaoa.hub.Hub;

class OASiblingHelperTest {
    @Test
    void constructorGetHubAndUseSameThreadAccessors() {
        Hub<Store> stores = new Hub<>(Store.class);
        OASiblingHelper<Store> helper = new OASiblingHelper<>(stores);

        assertSame(stores, helper.getHub());
        assertFalse(helper.getUseSameThread());

        helper.setUseSameThread(true);
        assertTrue(helper.getUseSameThread());

        helper.setUseSameThread(false);
        assertFalse(helper.getUseSameThread());
    }

    @Test
    void addAndGetPropertyPathResolveKnownOaposPaths() {
        Hub<Store> stores = new Hub<>(Store.class);
        OASiblingHelper<Store> helper = new OASiblingHelper<>(stores);
        helper.add(Store.P_Registers + "." + Register.P_RegisterSessions);

        assertEquals(Store.P_Registers, helper.getPropertyPath(new Store(), Store.P_Registers));
        assertEquals(Store.P_Registers + "." + Register.P_RegisterSessions,
                helper.getPropertyPath(new Register(), Register.P_RegisterSessions));
        assertEquals(Store.P_Registers + "." + Register.P_RegisterSessions + "." + RegisterSession.P_Invoices,
                helper.getPropertyPath(new RegisterSession(), RegisterSession.P_Invoices));
    }

    @Test
    void onGetReferenceLearnsPathsAndNullInputsReturnNull() {
        Hub<Store> stores = new Hub<>(Store.class);
        OASiblingHelper<Store> helper = new OASiblingHelper<>(stores);

        assertNull(helper.getPropertyPath(null, Store.P_Registers));
        assertNull(helper.getPropertyPath(new Invoice(), Invoice.P_InvoiceBaskets));

        Store store = new Store();
        helper.onGetReference(store, Store.P_Registers);
        assertEquals(Store.P_Registers, helper.getPropertyPath(store, Store.P_Registers));

        Register register = new Register();
        helper.onGetReference(register, Register.P_RegisterSessions);
        assertEquals(Store.P_Registers + "." + Register.P_RegisterSessions,
                helper.getPropertyPath(register, Register.P_RegisterSessions, true));
    }
}
