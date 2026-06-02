package com.viaoa.metadata;

import static org.junit.jupiter.api.Assertions.*;

import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OAObjectInfoFinalSmokeTest {

    public static class Order extends OAObject { }
    public static class Line extends OAObject { }

    @Test
    void objectInfoCombinesCoreMetadataWithoutCrossContamination() {
        OAObjectInfo oi = new OAObjectInfo(new String[] { "id" });
        oi.setForClass(Order.class);
        oi.setName("Order");

        OAPropertyInfo pi = new OAPropertyInfo();
        pi.setName("Total");
        pi.setClassType(Double.class);
        oi.addPropertyInfo(pi);

        OALinkInfo li = new OALinkInfo("Lines", Line.class, OALinkInfo.TYPE_MANY);
        oi.addLinkInfo(li);

        OACalcInfo ci = new OACalcInfo("LineCount", new String[] { "lines" });
        oi.addCalcInfo(ci);

        OAMethodInfo mi = new OAMethodInfo();
        mi.setName("DoWork");
        oi.addMethodInfo(mi);

        assertEquals(Order.class, oi.getForClass());
        assertTrue(oi.isIdProperty("id"));
        assertSame(pi, oi.getPropertyInfo("total"));
        assertSame(li, oi.getLinkInfo("lines"));
        assertSame(ci, oi.getCalcInfo("linecount"));
        assertSame(mi, oi.getMethodInfo("dowork"));

        assertNull(oi.getPropertyInfo("lines"));
        assertNull(oi.getLinkInfo("total"));
        assertNull(oi.getCalcInfo("total"));
        assertNull(oi.getMethodInfo("total"));
    }

    @Test
    void missingLookupReturnsNullNotExceptionAcrossMetadataKinds() {
        OAObjectInfo oi = new OAObjectInfo();

        assertNull(oi.getPropertyInfo("missing"));
        assertNull(oi.getLinkInfo("missing"));
        assertNull(oi.getCalcInfo("missing"));
        assertNull(oi.getMethodInfo("missing"));
    }

    @Test
    void nullLookupInputsAreSafeAcrossMetadataKinds() {
        OAObjectInfo oi = new OAObjectInfo();

        assertNull(oi.getPropertyInfo(null));
        assertNull(oi.getLinkInfo(null));
        assertNull(oi.getCalcInfo(null));
        assertNull(oi.getMethodInfo(null));
    }

    @Test
    void classAndNameCanBeResetCurrentContract() {
        OAObjectInfo oi = new OAObjectInfo();

        oi.setForClass(Order.class);
        oi.setName("Order");

        assertEquals(Order.class, oi.getForClass());
        assertEquals("Order", oi.getName());

        oi.setForClass(Line.class);
        oi.setName("Line");

        assertEquals(Line.class, oi.getForClass());
        assertEquals("Line", oi.getName());
    }
}
