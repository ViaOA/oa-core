package com.viaoa.metadata;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OAMetadataRuleFieldsConsistencyTest {

    @Test
    void propertyLinkCalcMethodAndObjectInfoHaveConsistentEnabledVisibleDefaults() {
        OAPropertyInfo pi = new OAPropertyInfo();
        OALinkInfo li = new OALinkInfo("x", Object.class, OALinkInfo.TYPE_ONE);
        OACalcInfo ci = new OACalcInfo("calc", null);
        OAMethodInfo mi = new OAMethodInfo();
        OAObjectInfo oi = new OAObjectInfo();

        assertNull(pi.getEnabledProperty());
        assertFalse(pi.getEnabledValue());
        assertNull(li.getEnabledProperty());
        assertFalse(li.getEnabledValue());
        assertNull(ci.getEnabledProperty());
        assertFalse(ci.getEnabledValue());
        assertNull(mi.getEnabledProperty());
        assertFalse(mi.getEnabledValue());
        assertNull(oi.getEnabledProperty());
        assertFalse(oi.getEnabledValue());

        assertNull(pi.getVisibleProperty());
        assertFalse(pi.getVisibleValue());
        assertNull(li.getVisibleProperty());
        assertFalse(li.getVisibleValue());
        assertNull(ci.getVisibleProperty());
        assertFalse(ci.getVisibleValue());
        assertNull(mi.getVisibleProperty());
        assertFalse(mi.getVisibleValue());
        assertNull(oi.getVisibleProperty());
        assertFalse(oi.getVisibleValue());
    }

    @Test
    void propertyLinkCalcMethodAndObjectInfoStoreContextRulesConsistently() {
        OAPropertyInfo pi = new OAPropertyInfo();
        OALinkInfo li = new OALinkInfo("x", Object.class, OALinkInfo.TYPE_ONE);
        OACalcInfo ci = new OACalcInfo("calc", null);
        OAMethodInfo mi = new OAMethodInfo();
        OAObjectInfo oi = new OAObjectInfo();

        setRules(pi);
        setRules(li);
        setRules(ci);
        setRules(mi);
        setRules(oi);

        assertRules(pi);
        assertRules(li);
        assertRules(ci);
        assertRules(mi);
        assertRules(oi);
    }

    private static void setRules(Object obj) {
        if (obj instanceof OAPropertyInfo x) {
            x.setContextEnabledProperty("ce");
            x.setContextEnabledValue(true);
            x.setContextVisibleProperty("cv");
            x.setContextVisibleValue(true);
        } else if (obj instanceof OALinkInfo x) {
            x.setContextEnabledProperty("ce");
            x.setContextEnabledValue(true);
            x.setContextVisibleProperty("cv");
            x.setContextVisibleValue(true);
        } else if (obj instanceof OACalcInfo x) {
            x.setContextEnabledProperty("ce");
            x.setContextEnabledValue(true);
            x.setContextVisibleProperty("cv");
            x.setContextVisibleValue(true);
        } else if (obj instanceof OAMethodInfo x) {
            x.setContextEnabledProperty("ce");
            x.setContextEnabledValue(true);
            x.setContextVisibleProperty("cv");
            x.setContextVisibleValue(true);
        } else if (obj instanceof OAObjectInfo x) {
            x.setContextEnabledProperty("ce");
            x.setContextEnabledValue(true);
            x.setContextVisibleProperty("cv");
            x.setContextVisibleValue(true);
        }
    }

    private static void assertRules(Object obj) {
        if (obj instanceof OAPropertyInfo x) {
            assertEquals("ce", x.getContextEnabledProperty());
            assertTrue(x.getContextEnabledValue());
            assertEquals("cv", x.getContextVisibleProperty());
            assertTrue(x.getContextVisibleValue());
        } else if (obj instanceof OALinkInfo x) {
            assertEquals("ce", x.getContextEnabledProperty());
            assertTrue(x.getContextEnabledValue());
            assertEquals("cv", x.getContextVisibleProperty());
            assertTrue(x.getContextVisibleValue());
        } else if (obj instanceof OACalcInfo x) {
            assertEquals("ce", x.getContextEnabledProperty());
            assertTrue(x.getContextEnabledValue());
            assertEquals("cv", x.getContextVisibleProperty());
            assertTrue(x.getContextVisibleValue());
        } else if (obj instanceof OAMethodInfo x) {
            assertEquals("ce", x.getContextEnabledProperty());
            assertTrue(x.getContextEnabledValue());
            assertEquals("cv", x.getContextVisibleProperty());
            assertTrue(x.getContextVisibleValue());
        } else if (obj instanceof OAObjectInfo x) {
            assertEquals("ce", x.getContextEnabledProperty());
            assertTrue(x.getContextEnabledValue());
            assertEquals("cv", x.getContextVisibleProperty());
            assertTrue(x.getContextVisibleValue());
        }
    }
}
