package com.viaoa.metadata;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OAObjectModelTest {
    @Test
    void defaultConstructorEnablesExpectedUiFlags() {
        OAObjectModel model = new OAObjectModel();
        assertTrue(model.getAllowGotoList());
        assertTrue(model.getAllowGotoEdit());
        assertTrue(model.getAllowSearch());
        assertFalse(model.getAllowHubSearch());
        assertTrue(model.getAllowAdd());
        assertTrue(model.getAllowNew());
        assertTrue(model.getAllowSave());
        assertFalse(model.getAllowRemove());
        assertTrue(model.getAllowDelete());
        assertTrue(model.getAllowClear());
        assertTrue(model.getAllowCut());
        assertTrue(model.getAllowCopy());
        assertTrue(model.getAllowPaste());
        assertTrue(model.getCreateUI());
    }

    @Test
    void defaultAllAndAccessorsRoundTrip() {
        OAObjectModel model = new OAObjectModel();
        model.defaultAll(false);
        assertFalse(model.getAllowGotoList());
        assertFalse(model.getAllowAdd());
        assertFalse(model.getAllowDownload());

        model.setDisplayName("Store");
        model.setPluralDisplayName("Stores");
        model.setAllowGotoList(true);
        model.setAllowGotoEdit(true);
        model.setAllowSearch(true);
        model.setAllowHubSearch(true);
        model.setAllowAdd(true);
        model.setAllowNew(true);
        model.setAllowSave(true);
        model.setAllowRemove(true);
        model.setAllowDelete(true);
        model.setAllowClear(true);
        model.setAllowRecursive(true);
        model.setAllowCut(true);
        model.setAllowCopy(true);
        model.setAllowPaste(true);
        model.setViewOnly(true);
        model.setCreateUI(true);
        model.setAllowMultiSelect(true);
        model.setAllowTableFilter(true);
        model.setAllowTableSorting(true);
        model.setAllowFilter(true);
        model.setForJfc(true);
        model.setAllowDownload(true);
        model.setAllowMove(true);
        model.setAllowRefresh(true);
        model.setAllowChildrenSplitPanel(true);

        assertEquals("Store", model.getDisplayName());
        assertEquals("Stores", model.getPluralDisplayName());
        assertTrue(model.getAllowGotoList());
        assertTrue(model.getAllowGotoEdit());
        assertTrue(model.getAllowSearch());
        assertTrue(model.getAllowHubSearch());
        assertTrue(model.getAllowAdd());
        assertTrue(model.getAllowNew());
        assertTrue(model.getAllowSave());
        assertTrue(model.getAllowRemove());
        assertTrue(model.getAllowDelete());
        assertTrue(model.getAllowClear());
        assertTrue(model.getAllowRecursive());
        assertTrue(model.getAllowCut());
        assertTrue(model.getAllowCopy());
        assertTrue(model.getAllowPaste());
        assertTrue(model.getViewOnly());
        assertTrue(model.getCreateUI());
        assertTrue(model.getAllowMultiSelect());
        assertTrue(model.getAllowTableFilter());
        assertTrue(model.getAllowTableSorting());
        assertTrue(model.getAllowFilter());
        assertTrue(model.getForJfc());
        assertTrue(model.getAllowDownload());
        assertTrue(model.getAllowMove());
        assertTrue(model.getAllowRefresh());
        assertTrue(model.getAllowChildrenSplitPanel());
    }
}
