package com.viaoa.metadata;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.test.pos.model.oa.Invoice;
import com.test.pos.model.oa.InvoiceBasket;
import com.test.pos.model.oa.ItemCategory;
import com.test.pos.model.oa.Store;

class OAObjectInfoTest {
    @Test
    void constructorsAndIdHelpersUseCaseInsensitiveLookup() {
        OAObjectInfo empty = new OAObjectInfo();
        assertArrayEquals(new String[0], empty.getIdProperties());

        OAObjectInfo one = new OAObjectInfo(Store.P_Id);
        assertArrayEquals(new String[] { Store.P_Id }, one.getIdProperties());
        assertArrayEquals(one.getIdProperties(), one.getKeyProperties());
        assertTrue(one.isIdProperty(Store.P_Id.toUpperCase()));
        assertTrue(one.isKeyProperty(Store.P_Id));
        assertFalse(one.isIdProperty(null));

        OAObjectInfo multi = new OAObjectInfo(new String[] { "storeNumber", "name" });
        assertTrue(multi.isIdProperty("STORENUMBER"));
    }

    @Test
    void metadataLookupFindsPropertiesLinksCalculatedValuesAndMethodsCaseInsensitively() {
        OAObjectInfo invoiceInfo = new OAObjectInfo(Invoice.P_Id);
        invoiceInfo.setForClass(Invoice.class);
        OAPropertyInfo id = new OAPropertyInfo();
        id.setName(Invoice.P_Id);
        invoiceInfo.addPropertyInfo(id);
        OALinkInfo baskets = new OALinkInfo(Invoice.P_InvoiceBaskets, InvoiceBasket.class, OALinkInfo.MANY);
        invoiceInfo.addLinkInfo(baskets);
        OACalcInfo calc = new OACalcInfo(Invoice.P_TotalAmountDue, new String[] { Invoice.P_TotalItemAmount });
        invoiceInfo.addCalcInfo(calc);
        OAMethodInfo method = new OAMethodInfo();
        method.setName("completeSale");
        invoiceInfo.addMethodInfo(method);

        assertSame(Invoice.class, invoiceInfo.getForClass());
        assertSame(id, invoiceInfo.getPropertyInfo(Invoice.P_Id.toUpperCase()));
        assertSame(baskets, invoiceInfo.getLinkInfo(Invoice.P_InvoiceBaskets.toUpperCase()));
        assertSame(calc, invoiceInfo.getCalcInfo(Invoice.P_TotalAmountDue.toUpperCase()));
        assertSame(method, invoiceInfo.getMethodInfo("COMPLETESALE"));
        assertNull(invoiceInfo.getPropertyInfo("missing"));
        assertNull(invoiceInfo.getLinkInfo("missing"));
    }

    @Test
    void addingLinksPropertiesCalcsAndMethodsResetsLookupCaches() throws Exception {
        OAObjectInfo info = new OAObjectInfo(Store.P_Id);
        info.setForClass(Store.class);
        OAPropertyInfo prop = new OAPropertyInfo();
        prop.setName(Store.P_Name);
        prop.setBlob(true);
        info.addPropertyInfo(prop);
        assertSame(prop, info.getPropertyInfo(Store.P_Name.toUpperCase()));
        assertTrue(info.getHasBlobProperty());
        assertTrue(info.getHasBlobPropery());

        OALinkInfo link = new OALinkInfo(Store.P_Registers, com.test.pos.model.oa.Register.class, OALinkInfo.MANY, true,
                com.test.pos.model.oa.Register.P_Store, true);
        info.addLink(link);
        assertSame(link, info.getLinkInfo(Store.P_Registers.toUpperCase()));
        assertArrayEquals(new OALinkInfo[] { link }, info.getOwnedLinkInfos());

        OACalcInfo calc = new OACalcInfo("calcHub", new String[] { Store.P_Registers }, true);
        info.addCalcInfo(calc);
        assertSame(calc, info.getCalcInfo("CALCHUB"));
        assertTrue(info.isHubCalcInfo("calchub"));

        OAMethodInfo methodInfo = new OAMethodInfo();
        methodInfo.setName("getName");
        Method method = Store.class.getMethod("getName");
        methodInfo.setObjectCallbackMethod(method);
        info.addMethod(methodInfo);
        assertSame(methodInfo, info.getMethodInfo("GETNAME"));
        info.addObjectCallbackMethod("custom", method);
        assertSame(method, info.getObjectCallbackMethod("custom"));
    }

    @Test
    void primitiveHubAndImportMetadataDefaultsAreDeterministic() {
        OAObjectInfo categoryInfo = new OAObjectInfo(ItemCategory.P_Id);
        categoryInfo.setForClass(ItemCategory.class);
        categoryInfo.addLinkInfo(new OALinkInfo(ItemCategory.P_SubItemCategories, ItemCategory.class, OALinkInfo.MANY));
        assertNotNull(categoryInfo.getLinkInfo(ItemCategory.P_SubItemCategories));

        OAObjectInfo storeInfo = new OAObjectInfo(Store.P_Id);
        storeInfo.setPrimitiveProperties(new String[] { Store.P_Id, Store.P_Name });
        assertArrayEquals(new String[] { Store.P_Id, Store.P_Name }, storeInfo.getPrimitiveProperties());
        assertNull(storeInfo.getHubProperties());
        assertFalse(new OAObjectInfo().hasImportMatchProperties());
        assertNull(new OAObjectInfo().getImportMatchPropertyNames());
        assertNull(new OAObjectInfo().getImportMatchPaths());
    }

    @Test
    void configurationFlagsAndMetadataPropertiesRoundTrip() throws Exception {
        OAObjectInfo info = new OAObjectInfo(Store.P_Id);
        Method method = Store.class.getMethod("getName");
        info.setForClass(Store.class);
        info.setUseDataSource(false);
        info.setLocalOnly(true);
        info.setAddToCache(false);
        info.setInitializeNewObjects(false);
        info.setName("Store");
        info.setDisplayName("Store Display");
        info.setPluralName("Stores");
        info.setLowerName("store");
        info.setRootTreePaths(new String[] { Store.P_Registers });
        info.addRequired(Store.P_Name);
        info.setLookup(true);
        info.setJsonUsesCapital(true);
        info.setGuidIsStored(true);
        info.setPreSelect(true);
        info.setProcessed(true);
        info.setViewDependentProperties(new String[] { "view" });
        info.setModelUserDependentProperties(new String[] { "context" });
        info.setEnabledProperty("enabled");
        info.setEnabledValue(false);
        info.setVisibleProperty("visible");
        info.setVisibleValue(false);
        info.setModelUserEnabledProperty("ctxEnabled");
        info.setModelUserEnabledValue(false);
        info.setModelUserVisibleProperty("ctxVisible");
        info.setModelUserVisibleValue(false);
        info.setObjectCallbackMethod(method);
        info.setHasOneAndOnlyOneLink(true);
        info.setSoftDeleteProperty("delete");
        info.setSoftDeleteReasonProperty("deleteReason");
        info.setVersionProperty("version");
        info.setVersionLinkProperty("versionLink");
        info.setTimeSeriesProperty("timeSeries");
        info.setFreezeProperty("freeze");
        info.setSingleton(true);
        info.setPojoSingleton(true);
        info.setNoPojo(true);

        assertSame(Store.class, info.getForClass());
        assertFalse(info.getUseDataSource());
        assertTrue(info.getLocalOnly());
        assertFalse(info.getAddToCache());
        assertFalse(info.getInitializeNewObjects());
        assertEquals("Store", info.getName());
        assertEquals("Store Display", info.getDisplayName());
        assertEquals("Stores", info.getPluralName());
        assertEquals("store", info.getLowerName());
        assertArrayEquals(new String[] { Store.P_Registers }, info.getRootTreePaths());
        assertTrue(info.getLookup());
        assertTrue(info.getJsonUsesCapital());
        assertTrue(info.getGuidIsStored());
        assertTrue(info.getPreSelect());
        assertTrue(info.getProcessed());
        assertArrayEquals(new String[] { "view" }, info.getViewDependentProperties());
        assertArrayEquals(new String[] { "context" }, info.getModelUserDependentProperties());
        assertEquals("enabled", info.getEnabledProperty());
        assertFalse(info.getEnabledValue());
        assertEquals("visible", info.getVisibleProperty());
        assertFalse(info.getVisibleValue());
        assertEquals("ctxEnabled", info.getModelUserEnabledProperty());
        assertFalse(info.getModelUserEnabledValue());
        assertEquals("ctxVisible", info.getModelUserVisibleProperty());
        assertFalse(info.getModelUserVisibleValue());
        assertSame(method, info.getObjectCallbackMethod());
        assertTrue(info.getHasOneAndOnlyOneLink());
        assertEquals("delete", info.getSoftDeleteProperty());
        assertEquals("deleteReason", info.getSoftDeleteReasonProperty());
        assertEquals("version", info.getVersionProperty());
        assertEquals("versionLink", info.getVersionLinkProperty());
        assertEquals("timeSeries", info.getTimeSeriesProperty());
        assertEquals("freeze", info.getFreezeProperty());
        assertTrue(info.getSingleton());
        assertTrue(info.getPojoSingleton());
        assertTrue(info.getNoPojo());
        assertSame(OAObjectInfo.getFriendAccess(), OAObjectInfo.getFriendAccess());
    }

    @Test
    void triggerMethodsHaveDeterministicNoTriggerDefaults() {
        OAObjectInfo info = new OAObjectInfo(Store.P_Id);
        assertFalse(info.getHasTriggers());
        assertTrue(info.getTriggerPropertNames().isEmpty());
        assertNull(info.getTriggers(Store.P_Name));
        int before = OAObjectInfo.getTotalTriggers();
        assertTrue(before >= 0);
    }

}
