package com.viaoa.metadata;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import com.test.pos.model.oa.Invoice;

class OAMethodInfoTest {
    @Test
    void constructorAndAccessorsRoundTrip() throws Exception {
        OAMethodInfo info = new OAMethodInfo();
        Method method = Invoice.class.getMethod("getTotalAmountDue");
        info.setName("completeSale");
        info.setViewDependentProperties(new String[] { "view" });
        info.setContextDependentProperties(new String[] { "context" });
        info.setEnabledProperty("enabled");
        info.setEnabledValue(false);
        info.setVisibleProperty("visible");
        info.setVisibleValue(false);
        info.setContextEnabledProperty("ctxEnabled");
        info.setContextEnabledValue(false);
        info.setContextVisibleProperty("ctxVisible");
        info.setContextVisibleValue(false);
        info.setObjectCallbackMethod(method);
        info.setOAMethod(null);

        assertEquals("completeSale", info.getName());
        assertArrayEquals(new String[] { "view" }, info.getViewDependentProperties());
        assertArrayEquals(new String[] { "context" }, info.getContextDependentProperties());
        assertEquals("enabled", info.getEnabledProperty());
        assertFalse(info.getEnabledValue());
        assertEquals("visible", info.getVisibleProperty());
        assertFalse(info.getVisibleValue());
        assertEquals("ctxEnabled", info.getContextEnabledProperty());
        assertFalse(info.getContextEnabledValue());
        assertEquals("ctxVisible", info.getContextVisibleProperty());
        assertFalse(info.getContextVisibleValue());
        assertSame(method, info.getObjectCallbackMethod());
        assertNull(info.getOAMethod());
    }
}
