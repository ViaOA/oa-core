package com.viaoa.metadata;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OAObjectModelBasicTest {

    @Test
    void constructorSetsExpectedUiDefaults() {
        OAObjectModel m = new OAObjectModel();

        assertTrue(m.getAllowGotoList());
        assertTrue(m.getAllowGotoEdit());
        assertTrue(m.getAllowSearch());
        assertFalse(m.getAllowHubSearch());
        assertTrue(m.getAllowAdd());
        assertTrue(m.getAllowNew());
        assertFalse(m.getAllowRemove());
        assertTrue(m.getAllowSave());
        assertTrue(m.getAllowDelete());
        assertTrue(m.getAllowClear());
        assertTrue(m.getAllowCut());
        assertTrue(m.getAllowCopy());
        assertTrue(m.getAllowPaste());
        assertFalse(m.getAllowMultiSelect());
        assertTrue(m.getAllowTableFilter());
        assertTrue(m.getAllowTableSorting());
        assertTrue(m.getAllowFilter());
        assertFalse(m.getAllowDownload());
        assertTrue(m.getCreateUI());
    }

    @Test
    void defaultAllTurnsMostFlagsOnAndOffTogether() {
        OAObjectModel m = new OAObjectModel();

        m.defaultAll(false);

        assertFalse(m.getAllowGotoList());
        assertFalse(m.getAllowGotoEdit());
        assertFalse(m.getAllowSearch());
        assertFalse(m.getAllowHubSearch());
        assertFalse(m.getAllowAdd());
        assertFalse(m.getAllowNew());
        assertFalse(m.getAllowRemove());
        assertFalse(m.getAllowSave());
        assertFalse(m.getAllowDelete());
        assertFalse(m.getAllowClear());
        assertFalse(m.getAllowCut());
        assertFalse(m.getAllowCopy());
        assertFalse(m.getAllowPaste());
        assertFalse(m.getAllowMultiSelect());
        assertFalse(m.getAllowTableFilter());
        assertFalse(m.getAllowTableSorting());
        assertFalse(m.getAllowFilter());
        assertFalse(m.getAllowDownload());
        assertFalse(m.getCreateUI());

        m.defaultAll(true);

        assertTrue(m.getAllowGotoList());
        assertTrue(m.getAllowGotoEdit());
        assertTrue(m.getAllowSearch());
        assertTrue(m.getAllowHubSearch());
        assertTrue(m.getAllowAdd());
        assertTrue(m.getAllowNew());
        assertTrue(m.getAllowRemove());
        assertTrue(m.getAllowSave());
        assertTrue(m.getAllowDelete());
        assertTrue(m.getAllowClear());
        assertTrue(m.getAllowCut());
        assertTrue(m.getAllowCopy());
        assertTrue(m.getAllowPaste());
        assertTrue(m.getAllowMultiSelect());
        assertTrue(m.getAllowTableFilter());
        assertTrue(m.getAllowTableSorting());
        assertTrue(m.getAllowFilter());
        assertTrue(m.getAllowDownload());
        assertTrue(m.getCreateUI());
    }

    @Test
    void displayNamesRoundTrip() {
        OAObjectModel m = new OAObjectModel();

        m.setDisplayName("Order");
        m.setPluralDisplayName("Orders");

        assertEquals("Order", m.getDisplayName());
        assertEquals("Orders", m.getPluralDisplayName());
    }

    @Test
    void individualFlagsRoundTrip() {
        OAObjectModel m = new OAObjectModel();
        m.defaultAll(false);

        m.setAllowMove(true);
        m.setAllowRefresh(true);
        m.setAllowChildrenSplitPanel(true);
        m.setAllowRecursive(true);
        m.setViewOnly(true);
        m.setForJfc(true);

        assertTrue(m.getAllowMove());
        assertTrue(m.getAllowRefresh());
        assertTrue(m.getAllowChildrenSplitPanel());
        assertTrue(m.getAllowRecursive());
        assertTrue(m.getViewOnly());
        assertTrue(m.getForJfc());
    }
}
