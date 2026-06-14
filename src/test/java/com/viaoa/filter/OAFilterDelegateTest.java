package com.viaoa.filter;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OAFilterDelegateTest {


    @Test
    void finderInfoConstructorStoresFinderAndRemainingPath() {
        com.viaoa.find.OAFinder finder = new com.viaoa.find.OAFinder(com.test.pos.model.oa.Invoice.P_InvoiceBaskets);
        OAFilterDelegate.FinderInfo info = new OAFilterDelegate.FinderInfo(finder, com.test.pos.model.oa.InvoiceBasket.P_Id);

        assertSame(finder, info.finder);
        assertEquals(com.test.pos.model.oa.InvoiceBasket.P_Id, info.pp);
    }

    @Test
    void createFinderReturnsNullForScalarOnlyPath() {
        assertNull(OAFilterDelegate.createFinder(com.test.pos.model.oa.Item.class,
                new com.viaoa.path.OAPath(com.test.pos.model.oa.Item.P_Name)));
    }

    @Test
    void createFinderSplitsManyLinkPathForFinderTraversal() {
        OAFilterDelegate.FinderInfo info = OAFilterDelegate.createFinder(com.test.pos.model.oa.Invoice.class,
                new com.viaoa.path.OAPath(FilterTestSupport.ITEM_NAME_PATH));

        assertNotNull(info);
        assertNotNull(info.finder);
        assertEquals(com.test.pos.model.oa.Item.P_Name, info.pp);
    }
}
