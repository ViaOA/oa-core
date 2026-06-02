package com.viaoa.metadata;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OAObjectInfoBasicMetadataTest {

    public static class Order extends OAObject {
        public String getName() {
            return "order";
        }

        public void onChange() {
        }
    }

    public static class Line extends OAObject {
    }

    @Test
    void constructorsAndClassIdentityRoundTrip() {
        OAObjectInfo oi = new OAObjectInfo(new String[] { "id", "storeId" });

        assertArrayEquals(new String[] { "id", "storeId" }, oi.getIdProperties());
        assertArrayEquals(new String[] { "id", "storeId" }, oi.getKeyProperties());

        oi.setForClass(Order.class);
        assertEquals(Order.class, oi.getForClass());
    }

    @Test
    void idAndKeyPropertyChecksAreCaseInsensitive() {
        OAObjectInfo oi = new OAObjectInfo(new String[] { "Id", "StoreId" });

        assertTrue(oi.isIdProperty("id"));
        assertTrue(oi.isIdProperty("STOREID"));
        assertTrue(oi.isKeyProperty("id"));
        assertTrue(oi.isKeyProperty("storeid"));
        assertFalse(oi.isIdProperty("missing"));
        assertFalse(oi.isKeyProperty("missing"));
        assertFalse(oi.isIdProperty(null));
        assertFalse(oi.isKeyProperty(null));
    }

    @Test
    void propertyInfoAddLookupAndBlobFlagAreCaseInsensitive() {
        OAObjectInfo oi = new OAObjectInfo();

        OAPropertyInfo pi = new OAPropertyInfo();
        pi.setName("Name");
        pi.setBlob(true);

        oi.addPropertyInfo(pi);

        assertSame(pi, oi.getPropertyInfo("name"));
        assertSame(pi, oi.getPropertyInfo("NAME"));
        assertTrue(oi.getHasBlobProperty());
        assertTrue(oi.getHasBlobPropery());
        assertNull(oi.getPropertyInfo("missing"));
    }

    @Test
    void linkInfoAddLookupIsCaseInsensitive() {
        OAObjectInfo oi = new OAObjectInfo();

        OALinkInfo li = new OALinkInfo("Lines", Line.class, OALinkInfo.TYPE_MANY);
        oi.addLinkInfo(li);

        assertSame(li, oi.getLinkInfo("lines"));
        assertSame(li, oi.getLinkInfo("LINES"));
        assertNull(oi.getLinkInfo("missing"));

        List<OALinkInfo> links = oi.getLinkInfos();
        assertTrue(links.contains(li));
    }

    @Test
    void calcInfoAddLookupAndHubCalcFlagAreCaseInsensitive() {
        OAObjectInfo oi = new OAObjectInfo();

        OACalcInfo ci = new OACalcInfo("Total", new String[] { "lines.amount" }, true);
        oi.addCalcInfo(ci);

        assertSame(ci, oi.getCalcInfo("total"));
        assertSame(ci, oi.getCalcInfo("TOTAL"));
        assertTrue(oi.isHubCalcInfo("total"));
        assertTrue(oi.isHubCalcInfo("TOTAL"));
        assertNull(oi.getCalcInfo("missing"));
    }

    @Test
    void methodInfoAddLookupIsCaseInsensitive() {
        OAObjectInfo oi = new OAObjectInfo();

        OAMethodInfo mi = new OAMethodInfo();
        mi.setName("DoWork");
        oi.addMethodInfo(mi);

        assertSame(mi, oi.getMethodInfo("dowork"));
        assertSame(mi, oi.getMethodInfo("DOWORK"));
        assertNull(oi.getMethodInfo("missing"));

        ArrayList<OAMethodInfo> methods = oi.getMethodInfos();
        assertTrue(methods.contains(mi));
    }

    @Test
    void optionalImportMatchMetadataDefaultsAreSafe() {
        OAObjectInfo oi = new OAObjectInfo();

        assertFalse(oi.hasImportMatchProperties());
        assertNull(oi.getImportMatchPropertyNames());
        assertNull(oi.getImportMatchPropertyPaths());
    }

    @Test
    void objectLevelFlagsAndDisplayMetadataRoundTrip() {
        OAObjectInfo oi = new OAObjectInfo();

        oi.setName("Order");
        oi.setDisplayName("Sales Order");
        oi.setPluralName("Sales Orders");
        oi.setLowerName("order");
        oi.setRootTreePropertyPaths(new String[] { "lines" });
        oi.setUseDataSource(false);
        oi.setLocalOnly(true);
        oi.setAddToCache(false);
        oi.setInitializeNewObjects(false);
        oi.setLookup(true);
        oi.setJsonUsesCapital(true);
        oi.setGuidIsStored(true);
        oi.setPreSelect(true);
        oi.setProcessed(true);
        oi.setSingleton(true);
        oi.setPojoSingleton(true);
        oi.setNoPojo(true);

        assertEquals("Order", oi.getName());
        assertEquals("Sales Order", oi.getDisplayName());
        assertEquals("Sales Orders", oi.getPluralName());
        assertEquals("order", oi.getLowerName());
        assertArrayEquals(new String[] { "lines" }, oi.getRootTreePropertyPaths());
        assertFalse(oi.getUseDataSource());
        assertTrue(oi.getLocalOnly());
        assertFalse(oi.getAddToCache());
        assertFalse(oi.getInitializeNewObjects());
        assertTrue(oi.getLookup());
        assertTrue(oi.getJsonUsesCapital());
        assertTrue(oi.getGuidIsStored());
        assertTrue(oi.getPreSelect());
        assertTrue(oi.getProcessed());
        assertTrue(oi.getSingleton());
        assertTrue(oi.getPojoSingleton());
        assertTrue(oi.getNoPojo());
    }

    @Test
    void callbackMethodAndRuleMetadataRoundTrip() throws Exception {
        OAObjectInfo oi = new OAObjectInfo();
        Method m = Order.class.getMethod("onChange");

        String[] view = { "v" };
        String[] context = { "c" };

        oi.setObjectCallbackMethod(m);
        oi.setViewDependentProperties(view);
        oi.setContextDependentProperties(context);
        oi.setEnabledProperty("enabled");
        oi.setEnabledValue(true);
        oi.setVisibleProperty("visible");
        oi.setVisibleValue(false);
        oi.setContextEnabledProperty("ctxEnabled");
        oi.setContextEnabledValue(true);
        oi.setContextVisibleProperty("ctxVisible");
        oi.setContextVisibleValue(false);

        assertSame(m, oi.getObjectCallbackMethod());
        assertSame(view, oi.getViewDependentProperties());
        assertSame(context, oi.getContextDependentProperties());
        assertEquals("enabled", oi.getEnabledProperty());
        assertTrue(oi.getEnabledValue());
        assertEquals("visible", oi.getVisibleProperty());
        assertFalse(oi.getVisibleValue());
        assertEquals("ctxEnabled", oi.getContextEnabledProperty());
        assertTrue(oi.getContextEnabledValue());
        assertEquals("ctxVisible", oi.getContextVisibleProperty());
        assertFalse(oi.getContextVisibleValue());
    }

    @Test
    void specialPropertyNamesRoundTrip() {
        OAObjectInfo oi = new OAObjectInfo();

        oi.setSoftDeleteProperty("deleted");
        oi.setSoftDeleteReasonProperty("deleteReason");
        oi.setVersionProperty("version");
        oi.setVersionLinkProperty("versionLink");
        oi.setTimeSeriesProperty("timeSeries");
        oi.setFreezeProperty("frozen");

        assertEquals("deleted", oi.getSoftDeleteProperty());
        assertEquals("deleteReason", oi.getSoftDeleteReasonProperty());
        assertEquals("version", oi.getVersionProperty());
        assertEquals("versionLink", oi.getVersionLinkProperty());
        assertEquals("timeSeries", oi.getTimeSeriesProperty());
        assertEquals("frozen", oi.getFreezeProperty());
    }
}
