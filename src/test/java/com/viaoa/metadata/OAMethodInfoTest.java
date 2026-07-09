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
        info.setOAMethod(null);

        assertEquals("completeSale", info.getName());
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
        assertNull(info.getOAMethod());
    }
}
