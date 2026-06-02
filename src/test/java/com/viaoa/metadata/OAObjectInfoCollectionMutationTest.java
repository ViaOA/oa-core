package com.viaoa.metadata;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OAObjectInfoCollectionMutationTest {

    public static class Child extends OAObject {
    }

    @Test
    void getPropertyInfosCreatesMutableListOnDemand() {
        OAObjectInfo oi = new OAObjectInfo();

        ArrayList<OAPropertyInfo> list = oi.getPropertyInfos();
        assertNotNull(list);
        assertTrue(list.isEmpty());

        OAPropertyInfo pi = new OAPropertyInfo();
        pi.setName("Name");
        list.add(pi);
        oi.resetPropertyInfo();

        assertSame(pi, oi.getPropertyInfo("name"));
    }

    @Test
    void setPropertyInfosReplacesBackingList() {
        OAObjectInfo oi = new OAObjectInfo();

        ArrayList<OAPropertyInfo> list = new ArrayList<>();
        OAPropertyInfo pi = new OAPropertyInfo();
        pi.setName("Name");
        list.add(pi);

        oi.setPropertyInfos(list);

        assertSame(list, oi.getPropertyInfos());
        assertSame(pi, oi.getPropertyInfo("name"));
    }

    @Test
    void getLinkInfosCreatesMutableListOnDemand() {
        OAObjectInfo oi = new OAObjectInfo();

        List<OALinkInfo> list = oi.getLinkInfos();
        assertNotNull(list);
        assertTrue(list.isEmpty());

        OALinkInfo li = new OALinkInfo("Children", Child.class, OALinkInfo.TYPE_MANY);
        list.add(li);

        assertSame(li, oi.getLinkInfo("children"));
    }

    @Test
    void setLinkInfosReplacesBackingList() {
        OAObjectInfo oi = new OAObjectInfo();

        List<OALinkInfo> list = new ArrayList<>();
        OALinkInfo li = new OALinkInfo("Children", Child.class, OALinkInfo.TYPE_MANY);
        list.add(li);

        oi.setLinkInfos(list);

        assertSame(list, oi.getLinkInfos());
        assertSame(li, oi.getLinkInfo("children"));
    }

    @Test
    void calcInfosListIsMutableAndLookupReflectsAdd() {
        OAObjectInfo oi = new OAObjectInfo();

        ArrayList<OACalcInfo> list = oi.getCalcInfos();
        assertNotNull(list);

        OACalcInfo ci = new OACalcInfo("Total", new String[] { "amount" });
        list.add(ci);

        assertSame(ci, oi.getCalcInfo("total"));
    }

    @Test
    void methodInfosListIsMutableAndLookupReflectsAdd() {
        OAObjectInfo oi = new OAObjectInfo();

        ArrayList<OAMethodInfo> list = oi.getMethodInfos();
        assertNotNull(list);

        OAMethodInfo mi = new OAMethodInfo();
        mi.setName("DoWork");
        list.add(mi);

        assertSame(mi, oi.getMethodInfo("dowork"));
    }

    @Test
    void addingNullMetadataEntriesDoesNotBreakLookupOfValidEntries() {
        OAObjectInfo oi = new OAObjectInfo();

        oi.getPropertyInfos().add(null);
        OAPropertyInfo pi = new OAPropertyInfo();
        pi.setName("Name");
        oi.addPropertyInfo(pi);

        oi.getLinkInfos().add(null);
        OALinkInfo li = new OALinkInfo("Children", Child.class, OALinkInfo.TYPE_MANY);
        oi.addLinkInfo(li);

        oi.getCalcInfos().add(null);
        OACalcInfo ci = new OACalcInfo("Total", new String[] { "amount" });
        oi.addCalcInfo(ci);

        oi.getMethodInfos().add(null);
        OAMethodInfo mi = new OAMethodInfo();
        mi.setName("DoWork");
        oi.addMethodInfo(mi);

        assertSame(pi, oi.getPropertyInfo("name"));
        assertSame(li, oi.getLinkInfo("children"));
        assertSame(ci, oi.getCalcInfo("total"));
        assertSame(mi, oi.getMethodInfo("dowork"));
    }
}
