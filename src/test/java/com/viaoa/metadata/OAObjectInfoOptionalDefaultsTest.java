package com.viaoa.metadata;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OAObjectInfoOptionalDefaultsTest {

    @Test
    void optionalMetadataUnsetStateIsSafe() {
        OAObjectInfo oi = new OAObjectInfo();

        assertFalse(oi.hasImportMatchProperties());
        assertNull(oi.getImportMatchPropertyNames());
        assertNull(oi.getImportMatchPropertyPaths());
        assertNull(oi.getRootTreePropertyPaths());
        assertNull(oi.getViewDependentProperties());
        assertNull(oi.getContextDependentProperties());
        assertNull(oi.getObjectCallbackMethod());
        assertNull(oi.getSoftDeleteProperty());
        assertNull(oi.getSoftDeleteReasonProperty());
        assertNull(oi.getVersionProperty());
        assertNull(oi.getVersionLinkProperty());
        assertNull(oi.getTimeSeriesProperty());
        assertNull(oi.getFreezeProperty());
    }

    @Test
    void importMatchPresenceCheckRequiresNonEmptyNamesOrPaths() {
        OAObjectInfo oi = new OAObjectInfo();

        oi.setImportMatchPropertyNames(new String[0]);
        oi.setImportMatchPropertyPaths(new String[0]);
        assertFalse(oi.hasImportMatchProperties());

        oi.setImportMatchPropertyNames(new String[] { "code" });
        assertTrue(oi.hasImportMatchProperties());

        oi.setImportMatchPropertyNames(null);
        oi.setImportMatchPropertyPaths(new String[] { "customer.code" });
        assertTrue(oi.hasImportMatchProperties());
    }

    @Test
    void optionalArraysRoundTripByReference() {
        OAObjectInfo oi = new OAObjectInfo();

        String[] names = { "code" };
        String[] paths = { "customer.code" };
        String[] roots = { "children" };
        String[] view = { "enabled" };
        String[] context = { "ctx" };

        oi.setImportMatchPropertyNames(names);
        oi.setImportMatchPropertyPaths(paths);
        oi.setRootTreePropertyPaths(roots);
        oi.setViewDependentProperties(view);
        oi.setContextDependentProperties(context);

        assertSame(names, oi.getImportMatchPropertyNames());
        assertSame(paths, oi.getImportMatchPropertyPaths());
        assertSame(roots, oi.getRootTreePropertyPaths());
        assertSame(view, oi.getViewDependentProperties());
        assertSame(context, oi.getContextDependentProperties());
    }

    @Test
    void displayPluralAndLowerNameDefaultsCanBeOverriddenAndResetBehaviorIsDocumented() {
        OAObjectInfo oi = new OAObjectInfo();

        oi.setName("SalesOrder");
        assertEquals("salesorder", oi.getLowerName());
        assertNotNull(oi.getDisplayName());
        assertNotNull(oi.getPluralName());

        oi.setDisplayName("Sales Order");
        oi.setPluralName("Sales Orders");
        oi.setLowerName("sales_order");

        assertEquals("Sales Order", oi.getDisplayName());
        assertEquals("Sales Orders", oi.getPluralName());
        assertEquals("sales_order", oi.getLowerName());
    }

    @Test
    void uiRuleDefaultsAndOverridesAreSafe() {
        OAObjectInfo oi = new OAObjectInfo();

        assertNull(oi.getEnabledProperty());
        assertFalse(oi.getEnabledValue());
        assertNull(oi.getVisibleProperty());
        assertFalse(oi.getVisibleValue());
        assertNull(oi.getContextEnabledProperty());
        assertFalse(oi.getContextEnabledValue());
        assertNull(oi.getContextVisibleProperty());
        assertFalse(oi.getContextVisibleValue());

        oi.setEnabledProperty("enabled");
        oi.setEnabledValue(true);
        oi.setVisibleProperty("visible");
        oi.setVisibleValue(true);
        oi.setContextEnabledProperty("contextEnabled");
        oi.setContextEnabledValue(true);
        oi.setContextVisibleProperty("contextVisible");
        oi.setContextVisibleValue(true);

        assertEquals("enabled", oi.getEnabledProperty());
        assertTrue(oi.getEnabledValue());
        assertEquals("visible", oi.getVisibleProperty());
        assertTrue(oi.getVisibleValue());
        assertEquals("contextEnabled", oi.getContextEnabledProperty());
        assertTrue(oi.getContextEnabledValue());
        assertEquals("contextVisible", oi.getContextVisibleProperty());
        assertTrue(oi.getContextVisibleValue());
    }

    @Test
    void booleanDefaultsAreDocumented() {
        OAObjectInfo oi = new OAObjectInfo();

        assertTrue(oi.getUseDataSource());
        assertFalse(oi.getLocalOnly());
        assertTrue(oi.getAddToCache());
        assertTrue(oi.getInitializeNewObjects());
        assertFalse(oi.getLookup());
        assertFalse(oi.getJsonUsesCapital());
        assertFalse(oi.getGuidIsStored());
        assertFalse(oi.getPreSelect());
        assertFalse(oi.getProcessed());
        assertFalse(oi.getSingleton());
        assertFalse(oi.getPojoSingleton());
        assertFalse(oi.getNoPojo());
    }
}
