package com.viaoa.metadata;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import com.test.pos.model.oa.Invoice;
import com.test.pos.model.oa.InvoiceBasket;
import com.test.pos.model.oa.Item;
import com.test.pos.model.oa.LineItem;
import com.test.pos.model.oa.Product;
import com.viaoa.hub.Hub;
import com.viaoa.text.OATextUtil;

class OALinkInfoTest {
    @Test
    void constructorsSetNameTargetTypeCascadeReverseAndOwner() {
        OALinkInfo simple = new OALinkInfo(LineItem.P_Product, Product.class, OALinkInfo.ONE);
        assertEquals(LineItem.P_Product, simple.getName());
        assertEquals(Product.class, simple.getToClass());
        assertEquals(OALinkInfo.ONE, simple.getType());
        assertFalse(simple.getCascadeSave());
        assertFalse(simple.getCascadeDelete());

        OALinkInfo cascade = new OALinkInfo(Invoice.P_InvoiceBaskets, InvoiceBasket.class, OALinkInfo.MANY, true,
                InvoiceBasket.P_Invoice, true);
        assertTrue(cascade.getCascadeSave());
        assertTrue(cascade.getCascadeDelete());
        assertEquals(InvoiceBasket.P_Invoice, cascade.getReverseName());
        assertTrue(cascade.isOwner());
        assertTrue(cascade.getOwner());
    }

    @Test
    void simpleFlagAndStringAccessorsRoundTrip() throws Exception {
        OALinkInfo link = new OALinkInfo(LineItem.P_Product, Product.class, OALinkInfo.ONE);
        Method method = Product.class.getMethod("getSku");
        String itemPath = OATextUtil.createPropertyPath(LineItem.P_Product, Product.P_Item, Item.P_Name);

        link.setOwner(true);
        link.setRecursive(true);
        link.setToClass(Product.class);
        link.setLowerName("product");
        link.setDisplayName("Product");
        link.setReverseName(Product.P_LineItems);
        link.setTransient(true);
        link.setCalculated(true);
        link.setProcessed(true);
        link.setServerSideCalc(true);
        link.setPrivateMethod(true);
        link.setNotUsed(true);
        link.setCascadeSave(true);
        link.setCascadeDelete(true);
        link.setAutoCreateNew(true);
        link.setMustBeEmptyForDelete(true);
        link.setCacheSize(7);
        link.setMatchProperty(Product.P_Sku);
        link.setMatchStopProperty("stop");
        link.setUniqueProperty(Product.P_Sku);
        link.setSortProperty(Product.P_Sku);
        link.setSortAsc(false);
        link.setSeqProperty("seq");
        link.setMatchHub(itemPath);
        link.setCouldBeLarge(true);
        link.setOAOne(null);
        link.setOAMany(null);
        link.setCalcDependentProperties(new String[] { Product.P_Item });
        link.setViewDependentProperties(new String[] { "view" });
        link.setContextDependentProperties(new String[] { "context" });
        link.setMergerPropertyPath(itemPath);
        link.setEnabledProperty("enabled");
        link.setEnabledValue(false);
        link.setVisibleProperty("visible");
        link.setVisibleValue(false);
        link.setContextEnabledProperty("ctxEnabled");
        link.setContextEnabledValue(false);
        link.setContextVisibleProperty("ctxVisible");
        link.setContextVisibleValue(false);
        link.setObjectCallbackMethod(method);
        link.setSchedulerMethod(method);
        link.setDefaultPropertyPath(itemPath);
        link.setDefaultPropertyPathIsHierarchy(true);
        link.setDefaultPropertyPathCanBeChanged(false);
        link.setDefaultContextPropertyPath("contextPath");
        link.setOneAndOnlyOne(true);
        link.setRequired(true);
        link.setImportMatch(true);
        link.setEqualPropertyPath(Product.P_Sku);
        link.setSelectFromPropertyPath(itemPath);
        link.setAutoCreateProperty("auto");

        assertTrue(link.getRecursive());
        assertEquals("product", link.getLowerName());
        assertEquals("Product", link.getDisplayName());
        assertTrue(link.getTransient());
        assertTrue(link.getCalculated());
        assertTrue(link.getProcessed());
        assertFalse(link.getUsed());
        assertTrue(link.getServerSideCalc());
        assertTrue(link.getPrivateMethod());
        assertTrue(link.getNotUsed());
        assertTrue(link.getAutoCreateNew());
        assertTrue(link.getMustBeEmptyForDelete());
        assertEquals(7, link.getCacheSize());
        assertEquals(Product.P_Sku, link.getMatchProperty());
        assertEquals("stop", link.getMatchStopProperty());
        assertEquals(Product.P_Sku, link.getUniqueProperty());
        assertEquals(Product.P_Sku, link.getSortProperty());
        assertFalse(link.isSortAsc());
        assertEquals("seq", link.getSeqProperty());
        assertEquals(itemPath, link.getMatchHub());
        assertTrue(link.getCouldBeLarge());
        assertNull(link.getOAOne());
        assertNull(link.getOAMany());
        assertArrayEquals(new String[] { Product.P_Item }, link.getCalcDependentProperties());
        assertArrayEquals(new String[] { "view" }, link.getViewDependentProperties());
        assertArrayEquals(new String[] { "context" }, link.getContextDependentProperties());
        assertEquals(itemPath, link.getMergerPropertyPath());
        assertEquals("enabled", link.getEnabledProperty());
        assertFalse(link.getEnabledValue());
        assertEquals("visible", link.getVisibleProperty());
        assertFalse(link.getVisibleValue());
        assertEquals("ctxEnabled", link.getContextEnabledProperty());
        assertFalse(link.getContextEnabledValue());
        assertEquals("ctxVisible", link.getContextVisibleProperty());
        assertFalse(link.getContextVisibleValue());
        assertSame(method, link.getObjectCallbackMethod());
        assertSame(method, link.getSchedulerMethod());
        assertEquals(itemPath, link.getDefaultPropertyPath());
        assertTrue(link.getDefaultPropertyPathIsHierarchy());
        assertFalse(link.getDefaultPropertyPathCanBeChanged());
        assertEquals("contextPath", link.getDefaultContextPropertyPath());
        assertTrue(link.getOneAndOnlyOne());
        assertTrue(link.getRequired());
        assertTrue(link.isImportMatch());
        assertTrue(link.getImportMatch());
        assertEquals(Product.P_Sku, link.getEqualPropertyPath());
        assertEquals(itemPath, link.getSelectFromPropertyPath());
        assertEquals("auto", link.getAutoCreateProperty());
        assertNotNull(link.getFkeyInfos());
    }

    @Test
    void valueLoadedAndLockedHaveSafeDefaultsForNonOAObjects() {
        OALinkInfo link = new OALinkInfo(LineItem.P_Product, Product.class, OALinkInfo.ONE);

        assertNull(link.getValue("not oa"));
        assertTrue(link.isLoaded("not oa"));
        assertFalse(link.isLocked("not oa"));
        assertNull(link.getReverseLinkInfo());
        assertNull(new OALinkInfo("x", null, OALinkInfo.ONE).getUniquePropertyGetMethod());
    }

    @Test
    void relationshipKindHelpersReflectTypeWhenReverseIsUnavailable() {
        OALinkInfo one = new OALinkInfo(LineItem.P_Product, Product.class, OALinkInfo.ONE);
        OALinkInfo many = new OALinkInfo(Invoice.P_InvoiceBaskets, InvoiceBasket.class, OALinkInfo.MANY);

        assertTrue(one.isOne());
        assertFalse(one.isMany());
        assertFalse(one.isOne2One());
        assertFalse(one.isOne2Many());
        assertFalse(one.isMany2One());
        assertFalse(one.isMany2Many());
        assertTrue(many.isMany());
        assertFalse(many.isOne());
        assertSame(OALinkInfo.getFriendAccess(), OALinkInfo.getFriendAccess());
    }

}
