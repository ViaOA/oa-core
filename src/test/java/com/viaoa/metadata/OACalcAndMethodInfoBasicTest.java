package com.viaoa.metadata;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

class OACalcAndMethodInfoBasicTest {

    public static class Bean {
        public String getDisplayName() {
            return "Display";
        }

        public void doWork() {
        }
    }

    @Test
    void calcInfoConstructorAndCoreFieldsRoundTrip() throws Exception {
        String[] deps = { "firstName", "lastName" };
        OACalcInfo ci = new OACalcInfo("FullName", deps, true);
        Method m = Bean.class.getMethod("getDisplayName");

        ci.setClassType(String.class);
        ci.setLowerName("fullname");
        ci.setHtml(true);
        ci.setObjectStatus(true);
        ci.setObjectCallbackMethod(m);

        assertEquals("FullName", ci.getName());
        assertEquals("fullname", ci.getLowerName());
        assertSame(deps, ci.getDependentProperties());
        assertTrue(ci.getIsForHub());
        assertEquals(String.class, ci.getClassType());
        assertTrue(ci.isHtml());
        assertTrue(ci.getObjectStatus());
        assertTrue(ci.isObjectStatus());
        assertSame(m, ci.getObjectCallbackMethod());
    }

    @Test
    void calcInfoDependenciesAndRuleMetadataRoundTripByReference() {
        OACalcInfo ci = new OACalcInfo("Calc", new String[] { "a" });

        String[] deps = { "x", "y" };
        String[] view = { "v" };
        String[] context = { "c" };

        ci.setDependentProperties(deps);
        ci.setViewDependentProperties(view);
        ci.setContextDependentProperties(context);
        ci.setEnabledProperty("enabled");
        ci.setEnabledValue(true);
        ci.setVisibleProperty("visible");
        ci.setVisibleValue(false);
        ci.setContextEnabledProperty("ctxEnabled");
        ci.setContextEnabledValue(true);
        ci.setContextVisibleProperty("ctxVisible");
        ci.setContextVisibleValue(false);

        assertSame(deps, ci.getDependentProperties());
        assertSame(view, ci.getViewDependentProperties());
        assertSame(context, ci.getContextDependentProperties());
        assertEquals("enabled", ci.getEnabledProperty());
        assertTrue(ci.getEnabledValue());
        assertEquals("visible", ci.getVisibleProperty());
        assertFalse(ci.getVisibleValue());
        assertEquals("ctxEnabled", ci.getContextEnabledProperty());
        assertTrue(ci.getContextEnabledValue());
        assertEquals("ctxVisible", ci.getContextVisibleProperty());
        assertFalse(ci.getContextVisibleValue());
    }

    @Test
    void methodInfoCoreFieldsRoundTrip() throws Exception {
        OAMethodInfo mi = new OAMethodInfo();
        Method m = Bean.class.getMethod("doWork");

        mi.setName("doWork");
        mi.setObjectCallbackMethod(m);

        assertEquals("doWork", mi.getName());
        assertSame(m, mi.getObjectCallbackMethod());
    }

    @Test
    void methodInfoRuleMetadataRoundTripByReference() {
        OAMethodInfo mi = new OAMethodInfo();

        String[] view = { "v" };
        String[] context = { "c" };

        mi.setViewDependentProperties(view);
        mi.setContextDependentProperties(context);
        mi.setEnabledProperty("enabled");
        mi.setEnabledValue(true);
        mi.setVisibleProperty("visible");
        mi.setVisibleValue(false);
        mi.setContextEnabledProperty("ctxEnabled");
        mi.setContextEnabledValue(true);
        mi.setContextVisibleProperty("ctxVisible");
        mi.setContextVisibleValue(false);

        assertSame(view, mi.getViewDependentProperties());
        assertSame(context, mi.getContextDependentProperties());
        assertEquals("enabled", mi.getEnabledProperty());
        assertTrue(mi.getEnabledValue());
        assertEquals("visible", mi.getVisibleProperty());
        assertFalse(mi.getVisibleValue());
        assertEquals("ctxEnabled", mi.getContextEnabledProperty());
        assertTrue(mi.getContextEnabledValue());
        assertEquals("ctxVisible", mi.getContextVisibleProperty());
        assertFalse(mi.getContextVisibleValue());
    }
}
