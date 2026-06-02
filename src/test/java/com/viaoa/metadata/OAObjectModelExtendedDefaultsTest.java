package com.viaoa.metadata;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OAObjectModelExtendedDefaultsTest {

    @Test
    void allFlagsCanBeIndividuallyToggledFalseThenTrue() {
        OAObjectModel m = new OAObjectModel();

        m.defaultAll(false);

        m.setAllowGotoList(true);
        m.setAllowGotoEdit(true);
        m.setAllowSearch(true);
        m.setAllowHubSearch(true);
        m.setAllowMultiSelect(true);
        m.setAllowTableFilter(true);
        m.setAllowTableSorting(true);
        m.setAllowAdd(true);
        m.setAllowNew(true);
        m.setAllowSave(true);
        m.setAllowRemove(true);
        m.setAllowDelete(true);
        m.setAllowClear(true);
        m.setAllowRecursive(true);
        m.setAllowFilter(true);
        m.setAllowDownload(true);
        m.setAllowRefresh(true);
        m.setAllowCut(true);
        m.setAllowCopy(true);
        m.setAllowPaste(true);
        m.setViewOnly(true);
        m.setCreateUI(true);
        m.setAllowMove(true);
        m.setAllowChildrenSplitPanel(true);
        m.setForJfc(true);

        assertTrue(m.getAllowGotoList());
        assertTrue(m.getAllowGotoEdit());
        assertTrue(m.getAllowSearch());
        assertTrue(m.getAllowHubSearch());
        assertTrue(m.getAllowMultiSelect());
        assertTrue(m.getAllowTableFilter());
        assertTrue(m.getAllowTableSorting());
        assertTrue(m.getAllowAdd());
        assertTrue(m.getAllowNew());
        assertTrue(m.getAllowSave());
        assertTrue(m.getAllowRemove());
        assertTrue(m.getAllowDelete());
        assertTrue(m.getAllowClear());
        assertTrue(m.getAllowRecursive());
        assertTrue(m.getAllowFilter());
        assertTrue(m.getAllowDownload());
        assertTrue(m.getAllowRefresh());
        assertTrue(m.getAllowCut());
        assertTrue(m.getAllowCopy());
        assertTrue(m.getAllowPaste());
        assertTrue(m.getViewOnly());
        assertTrue(m.getCreateUI());
        assertTrue(m.getAllowMove());
        assertTrue(m.getAllowChildrenSplitPanel());
        assertTrue(m.getForJfc());
    }

    @Test
    void displayNameConstantsRemainStable() {
        assertEquals("DisplayName", OAObjectModel.P_DisplayName);
        assertEquals("DisplayNamePlural", OAObjectModel.P_DisplayNamePlural);
    }

    @Test
    void nullDisplayNamesAreAllowedCurrentContract() {
        OAObjectModel m = new OAObjectModel();

        m.setDisplayName(null);
        m.setPluralDisplayName(null);

        assertNull(m.getDisplayName());
        assertNull(m.getPluralDisplayName());
    }
}
