package com.viaoa.metadata;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OAObjectInfoLookupCacheTest {

    public static class Order extends OAObject {
    }

    public static class Line extends OAObject {
    }

    @Test
    void propertyLookupCacheReflectsSupportedAddAfterMiss() {
        OAObjectInfo oi = new OAObjectInfo();

        assertNull(oi.getPropertyInfo("name"));

        OAPropertyInfo pi = new OAPropertyInfo();
        pi.setName("Name");
        oi.addPropertyInfo(pi);

        assertSame(pi, oi.getPropertyInfo("name"));
        assertSame(pi, oi.getPropertyInfo("NAME"));
    }

    @Test
    void resetPropertyInfoClearsPropertyLookupCacheAndKeepsList() {
        OAObjectInfo oi = new OAObjectInfo();

        OAPropertyInfo pi = new OAPropertyInfo();
        pi.setName("Name");
        oi.addPropertyInfo(pi);

        assertSame(pi, oi.getPropertyInfo("name"));

        oi.resetPropertyInfo();

        assertSame(pi, oi.getPropertyInfo("name"));
        assertTrue(oi.getPropertyInfos().contains(pi));
    }

    @Test
    void linkLookupCacheReflectsSupportedAddAfterMiss() {
        OAObjectInfo oi = new OAObjectInfo();

        assertNull(oi.getLinkInfo("lines"));

        OALinkInfo li = new OALinkInfo("Lines", Line.class, OALinkInfo.TYPE_MANY);
        oi.addLinkInfo(li);

        assertSame(li, oi.getLinkInfo("lines"));
        assertSame(li, oi.getLinkInfo("LINES"));
    }

    @Test
    void calcLookupCacheReflectsSupportedAddAfterMiss() {
        OAObjectInfo oi = new OAObjectInfo();

        assertNull(oi.getCalcInfo("total"));

        OACalcInfo ci = new OACalcInfo("Total", new String[] { "lines.amount" });
        oi.addCalcInfo(ci);

        assertSame(ci, oi.getCalcInfo("total"));
        assertSame(ci, oi.getCalcInfo("TOTAL"));
    }

    @Test
    void methodLookupCacheReflectsSupportedAddAfterMiss() {
        OAObjectInfo oi = new OAObjectInfo();

        assertNull(oi.getMethodInfo("doWork"));

        OAMethodInfo mi = new OAMethodInfo();
        mi.setName("DoWork");
        oi.addMethodInfo(mi);

        assertSame(mi, oi.getMethodInfo("dowork"));
        assertSame(mi, oi.getMethodInfo("DOWORK"));
    }

    @Test
    void gettersReturnMutableBackingCollectionsCurrentContract() {
        OAObjectInfo oi = new OAObjectInfo();

        List<OALinkInfo> links = oi.getLinkInfos();
        OALinkInfo li = new OALinkInfo("line", Line.class, OALinkInfo.TYPE_ONE);
        links.add(li);
        assertSame(li, oi.getLinkInfo("line"));

        List<OAPropertyInfo> props = oi.getPropertyInfos();
        OAPropertyInfo pi = new OAPropertyInfo();
        pi.setName("Name");
        props.add(pi);
        oi.resetPropertyInfo();
        assertSame(pi, oi.getPropertyInfo("name"));
    }

    @Test
    void duplicatePropertyNamesResolveToFirstAddedCurrentContract() {
        OAObjectInfo oi = new OAObjectInfo();

        OAPropertyInfo first = new OAPropertyInfo();
        first.setName("Name");
        OAPropertyInfo second = new OAPropertyInfo();
        second.setName("Name");

        oi.addPropertyInfo(first);
        oi.addPropertyInfo(second);

        assertSame(first, oi.getPropertyInfo("name"));
        assertTrue(oi.getPropertyInfos().contains(second));
    }

    @Test
    void duplicateLinkNamesResolveToFirstAddedCurrentContract() {
        OAObjectInfo oi = new OAObjectInfo();

        OALinkInfo first = new OALinkInfo("Lines", Line.class, OALinkInfo.TYPE_MANY);
        OALinkInfo second = new OALinkInfo("Lines", Line.class, OALinkInfo.TYPE_MANY);

        oi.addLinkInfo(first);
        oi.addLinkInfo(second);

        assertSame(first, oi.getLinkInfo("lines"));
        assertTrue(oi.getLinkInfos().contains(second));
    }
}
