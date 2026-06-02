package com.viaoa.metadata;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;
import java.util.List;

import com.viaoa.object.OAObject;

import org.junit.jupiter.api.Test;

class OALinkInfoBasicTest {

    public static class Parent extends OAObject {
        public String getName() {
            return "parent";
        }
    }

    public static class Child extends OAObject {
        public String getName() {
            return "child";
        }
    }

    @Test
    void constructorsSetCoreLinkMetadata() {
        OALinkInfo li = new OALinkInfo("children", Child.class, OALinkInfo.TYPE_MANY);

        assertEquals("children", li.getName());
        assertEquals("children", li.getLowerName());
        assertEquals(Child.class, li.getToClass());
        assertEquals(OALinkInfo.TYPE_MANY, li.getType());
        assertFalse(li.getCascadeSave());
        assertFalse(li.getCascadeDelete());
        assertNull(li.getReverseName());
        assertFalse(li.getOwner());

        OALinkInfo li2 = new OALinkInfo("child", Child.class, OALinkInfo.TYPE_ONE, true, "parent", true);

        assertTrue(li2.getCascadeSave());
        assertTrue(li2.getCascadeDelete());
        assertEquals("parent", li2.getReverseName());
        assertTrue(li2.isOwner());
        assertTrue(li2.getOwner());
    }

    @Test
    void independentCascadeConstructorKeepsSaveAndDeleteSeparate() {
        OALinkInfo li = new OALinkInfo("child", Child.class, OALinkInfo.TYPE_ONE, true, false, "parent");

        assertTrue(li.getCascadeSave());
        assertFalse(li.getCascadeDelete());
    }

    @Test
    void mutableCoreFlagsRoundTrip() {
        OALinkInfo li = new OALinkInfo("child", Child.class, OALinkInfo.TYPE_ONE);

        li.setOwner(true);
        li.setRecursive(true);
        li.setToClass(Parent.class);
        li.setLowerName("custom");
        li.setDisplayName("Child Link");
        li.setReverseName("parent");
        li.setTransient(true);
        li.setCalculated(true);
        li.setProcessed(true);
        li.setServerSideCalc(true);
        li.setPrivateMethod(true);
        li.setNotUsed(true);
        li.setCascadeSave(true);
        li.setCascadeDelete(true);
        li.setAutoCreateNew(true);
        li.setMustBeEmptyForDelete(true);
        li.setCacheSize(25);
        li.setCouldBeLarge(true);

        assertTrue(li.getOwner());
        assertTrue(li.getRecursive());
        assertEquals(Parent.class, li.getToClass());
        assertEquals("custom", li.getLowerName());
        assertEquals("Child Link", li.getDisplayName());
        assertEquals("parent", li.getReverseName());
        assertTrue(li.getTransient());
        assertTrue(li.getCalculated());
        assertTrue(li.getProcessed());
        assertFalse(li.getUsed());
        assertTrue(li.getServerSideCalc());
        assertTrue(li.getPrivateMethod());
        assertTrue(li.getNotUsed());
        assertTrue(li.getCascadeSave());
        assertTrue(li.getCascadeDelete());
        assertTrue(li.getAutoCreateNew());
        assertTrue(li.getMustBeEmptyForDelete());
        assertEquals(25, li.getCacheSize());
        assertTrue(li.getCouldBeLarge());
    }

    @Test
    void pathAndRelationshipMetadataRoundTrip() {
        OALinkInfo li = new OALinkInfo("children", Child.class, OALinkInfo.TYPE_MANY);

        li.setMatchProperty("code");
        li.setMatchStopProperty("stop");
        li.setUniqueProperty("uid");
        li.setSortProperty("name");
        li.setSortAsc(false);
        li.setSeqProperty("seq");
        li.setMatchHub("orders");
        li.setMergerPropertyPath("orders.lines");
        li.setDefaultPropertyPath("name");
        li.setDefaultPropertyPathIsHierarchy(true);
        li.setDefaultPropertyPathCanBeChanged(true);
        li.setDefaultContextPropertyPath(".");
        li.setOneAndOnlyOne(true);
        li.setRequired(true);
        li.setImportMatch(true);
        li.setEqualPropertyPath("id");
        li.setSelectFromPropertyPath("activeChildren");
        li.setAutoCreateProperty("createFlag");

        assertEquals("code", li.getMatchProperty());
        assertEquals("stop", li.getMatchStopProperty());
        assertEquals("uid", li.getUniqueProperty());
        assertEquals("name", li.getSortProperty());
        assertFalse(li.isSortAsc());
        assertEquals("seq", li.getSeqProperty());
        assertEquals("orders", li.getMatchHub());
        assertEquals("orders.lines", li.getMergerPropertyPath());
        assertEquals("name", li.getDefaultPropertyPath());
        assertTrue(li.getDefaultPropertyPathIsHierarchy());
        assertTrue(li.getDefaultPropertyPathCanBeChanged());
        assertEquals(".", li.getDefaultContextPropertyPath());
        assertTrue(li.getOneAndOnlyOne());
        assertTrue(li.getRequired());
        assertTrue(li.isImportMatch());
        assertTrue(li.getImportMatch());
        assertEquals("id", li.getEqualPropertyPath());
        assertEquals("activeChildren", li.getSelectFromPropertyPath());
        assertEquals("createFlag", li.getAutoCreateProperty());
    }

    @Test
    void dependencyAndRuleMetadataRoundTripByReference() {
        OALinkInfo li = new OALinkInfo("children", Child.class, OALinkInfo.TYPE_MANY);

        String[] calc = { "a" };
        String[] view = { "b" };
        String[] context = { "c" };
        li.setCalcDependentProperties(calc);
        li.setViewDependentProperties(view);
        li.setContextDependentProperties(context);
        li.setPojoNames(new String[] { "pojoChild" });

        li.setEnabledProperty("enabled");
        li.setEnabledValue(true);
        li.setVisibleProperty("visible");
        li.setVisibleValue(false);
        li.setContextEnabledProperty("ctxEnabled");
        li.setContextEnabledValue(true);
        li.setContextVisibleProperty("ctxVisible");
        li.setContextVisibleValue(false);

        assertSame(calc, li.getCalcDependentProperties());
        assertSame(view, li.getViewDependentProperties());
        assertSame(context, li.getContextDependentProperties());
        assertArrayEquals(new String[] { "pojoChild" }, li.getPojoNames());

        assertEquals("enabled", li.getEnabledProperty());
        assertTrue(li.getEnabledValue());
        assertEquals("visible", li.getVisibleProperty());
        assertFalse(li.getVisibleValue());
        assertEquals("ctxEnabled", li.getContextEnabledProperty());
        assertTrue(li.getContextEnabledValue());
        assertEquals("ctxVisible", li.getContextVisibleProperty());
        assertFalse(li.getContextVisibleValue());
    }

    @Test
    void callbackAndSchedulerMethodsRoundTrip() throws Exception {
        OALinkInfo li = new OALinkInfo("child", Child.class, OALinkInfo.TYPE_ONE);
        Method m = Parent.class.getMethod("getName");

        li.setObjectCallbackMethod(m);
        li.setSchedulerMethod(m);

        assertSame(m, li.getObjectCallbackMethod());
        assertSame(m, li.getSchedulerMethod());
    }

    @Test
    void fkeyInfosListIsLazyAndMutable() {
        OALinkInfo li = new OALinkInfo("child", Child.class, OALinkInfo.TYPE_ONE);

        List<OAFkeyInfo> list = li.getFkeyInfos();
        assertNotNull(list);
        assertTrue(list.isEmpty());

        OAFkeyInfo fki = new OAFkeyInfo();
        list.add(fki);

        assertSame(list, li.getFkeyInfos());
        assertEquals(1, li.getFkeyInfos().size());
        assertSame(fki, li.getFkeyInfos().get(0));
    }

    @Test
    void typeHelpersReflectCardinalityWithoutReverseMetadata() {
        OALinkInfo one = new OALinkInfo("child", Child.class, OALinkInfo.TYPE_ONE);
        OALinkInfo many = new OALinkInfo("children", Child.class, OALinkInfo.TYPE_MANY);

        assertTrue(one.isOne());
        assertFalse(one.isMany());

        assertFalse(many.isOne());
        assertTrue(many.isMany());
    }
}
