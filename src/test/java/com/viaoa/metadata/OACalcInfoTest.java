package com.viaoa.metadata;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import com.test.pos.model.oa.Invoice;

class OACalcInfoTest {
    @Test
    void constructorsSetNameDependenciesAndHubFlag() {
        OACalcInfo calc = new OACalcInfo(Invoice.P_TotalAmountDue, new String[] { Invoice.P_TotalItemAmount });
        assertEquals(Invoice.P_TotalAmountDue, calc.getName());
        assertArrayEquals(new String[] { Invoice.P_TotalItemAmount }, calc.getDependentProperties());
        assertFalse(calc.getIsForHub());

        OACalcInfo hubCalc = new OACalcInfo("calcChildren", new String[] { "children" }, true);
        assertTrue(hubCalc.getIsForHub());
    }

    @Test
    void propertyAccessorsRoundTrip() throws Exception {
        OACalcInfo calc = new OACalcInfo("total", new String[] { "a" });
        Method method = Invoice.class.getMethod("getTotalAmountDue");
        calc.setClassType(Double.class);
        calc.setLowerName("total");
        calc.setHtml(true);
        calc.setObjectStatus(true);
        calc.setDependentProperties(new String[] { "b" });
        calc.setOACalculatedProperty(null);
        calc.setViewDependentProperties(new String[] { "view" });
        calc.setContextDependentProperties(new String[] { "context" });
        calc.setEnabledProperty("enabled");
        calc.setEnabledValue(false);
        calc.setVisibleProperty("visible");
        calc.setVisibleValue(false);
        calc.setContextEnabledProperty("ctxEnabled");
        calc.setContextEnabledValue(false);
        calc.setContextVisibleProperty("ctxVisible");
        calc.setContextVisibleValue(false);
        calc.setObjectCallbackMethod(method);

        assertEquals(Double.class, calc.getClassType());
        assertEquals("total", calc.getLowerName());
        assertTrue(calc.isHtml());
        assertTrue(calc.getObjectStatus());
        assertTrue(calc.isObjectStatus());
        assertArrayEquals(new String[] { "b" }, calc.getDependentProperties());
        assertNull(calc.getOACalculatedProperty());
        assertArrayEquals(new String[] { "view" }, calc.getViewDependentProperties());
        assertArrayEquals(new String[] { "context" }, calc.getContextDependentProperties());
        assertEquals("enabled", calc.getEnabledProperty());
        assertFalse(calc.getEnabledValue());
        assertEquals("visible", calc.getVisibleProperty());
        assertFalse(calc.getVisibleValue());
        assertEquals("ctxEnabled", calc.getContextEnabledProperty());
        assertFalse(calc.getContextEnabledValue());
        assertEquals("ctxVisible", calc.getContextVisibleProperty());
        assertFalse(calc.getContextVisibleValue());
        assertSame(method, calc.getObjectCallbackMethod());
        assertSame(OACalcInfo.getFriendAccess(), OACalcInfo.getFriendAccess());
    }
}
