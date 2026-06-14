package com.viaoa.hub;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.Vector;

import org.junit.jupiter.api.Test;

import com.test.pos.model.oa.Register;
import com.viaoa.hub.detail.HubDetail;
import com.viaoa.hub.link.HubLinkEventListener;
import com.viaoa.hub.listener.HubListenerTree;

class HubDataUniqueTest {
    @Test
    void accessorsRoundTripUniqueHubState() throws Exception {
        HubDataUnique<Register> data = new HubDataUnique<>();
        Hub<Register> hub = new Hub<>(Register.class);
        HubListenerTree tree = new HubListenerTree(hub);
        Vector<HubDetail> details = new Vector<>();
        Method getCode = Register.class.getMethod("getCode");
        Method setCode = Register.class.getMethod("setCode", String.class);
        HubLinkEventListener listener = new HubLinkEventListener(hub, hub);
        @SuppressWarnings("unchecked")
        WeakReference<Hub<Register>>[] weak = new WeakReference[] { new WeakReference<>(hub) };

        data.setDefaultPos(1);
        data.setNullOnRemove(true);
        data.setListenerTree(tree);
        data.setVecHubDetail(details);
        data.setUpdatingActiveObject(true);
        data.setLinkToHub(hub);
        data.setLinkPos(true);
        data.setLinkToPropertyName(Register.P_Code);
        data.setLinkToGetMethod(getCode);
        data.setLinkToSetMethod(setCode);
        data.setLinkFromPropertyName(Register.P_Code);
        data.setLinkFromGetMethod(getCode);
        data.setHubLinkEventListener(listener);
        data.setSharedHub(hub);
        data.setWeakSharedHubs(weak);
        data.setAddHub(hub);
        data.setAutoCreate(true);
        data.setAutoCreateAllowDups(true);

        assertEquals(1, data.getDefaultPos());
        assertTrue(data.isNullOnRemove());
        assertSame(tree, data.getListenerTree());
        assertSame(details, data.getVecHubDetail());
        assertTrue(data.isUpdatingActiveObject());
        assertSame(hub, data.getLinkToHub());
        assertTrue(data.isLinkPos());
        assertEquals(Register.P_Code, data.getLinkToPropertyName());
        assertSame(getCode, data.getLinkToGetMethod());
        assertSame(setCode, data.getLinkToSetMethod());
        assertEquals(Register.P_Code, data.getLinkFromPropertyName());
        assertSame(getCode, data.getLinkFromGetMethod());
        assertSame(listener, data.getHubLinkEventListener());
        assertSame(hub, data.getSharedHub());
        assertSame(weak, data.getWeakSharedHubs());
        assertSame(hub, data.getAddHub());
        assertTrue(data.isAutoCreate());
        assertTrue(data.isAutoCreateAllowDups());
    }
}
