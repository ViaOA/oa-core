package com.viaoa.path;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.test.pos.model.oa.Invoice;
import com.test.pos.model.oa.InvoiceBasket;
import com.test.pos.model.oa.Item;
import com.test.pos.model.oa.LineItem;
import com.test.pos.model.oa.Product;
import com.test.pos.model.oa.Register;
import com.test.pos.model.oa.RegisterSession;
import com.test.pos.model.oa.Store;
import com.viaoa.hub.Hub;

class OAPathDelegateTest {

    @Test
    void createRootPropertyPathDelegatesToOAPathConstructorAndResolvesLeadingRootClass() throws Exception {
        OAPath path = OAPathDelegate.createRootPropertyPath("[Invoice]." + Invoice.P_InvoiceBaskets + "."
                + InvoiceBasket.P_LineItems + "." + LineItem.P_Product + "." + Product.P_Item + "." + Item.P_Name,
                Store.class);

        assertNotNull(path);
        assertEquals(Invoice.class, path.getFromClass());
        assertEquals(Item.P_Name, path.getLastPropertyName());
        assertArrayEquals(new String[] { Invoice.P_InvoiceBaskets, InvoiceBasket.P_LineItems, LineItem.P_Product,
                Product.P_Item, Item.P_Name }, path.getProperties());
    }

    @Test
    void getPropertyPathForClassesHandlesNullsSimplePathsAndUnreachableClasses() {
        Hub<Store> stores = new Hub<>(Store.class);

        assertNull(OAPathDelegate.getPropertyPathforClasses(null, new Class[] { Register.class }));
        assertNull(OAPathDelegate.getPropertyPathforClasses(stores, null));

        assertEquals(Store.P_Registers,
                OAPathDelegate.getPropertyPathforClasses(stores, new Class[] { Register.class }));
        assertEquals(Store.P_Registers + "." + Register.P_RegisterSessions,
                OAPathDelegate.getPropertyPathforClasses(stores, new Class[] { Register.class, RegisterSession.class }));
        assertEquals(Store.P_Registers + "." + Register.P_RegisterSessions + "." + RegisterSession.P_Invoices + "."
                + Invoice.P_InvoiceBaskets + "." + InvoiceBasket.P_LineItems + "." + LineItem.P_Product + "."
                + Product.P_Item,
                OAPathDelegate.getPropertyPathforClasses(stores, new Class[] { Register.class, RegisterSession.class,
                        Invoice.class, InvoiceBasket.class, LineItem.class, Product.class, Item.class }));

        assertNull(OAPathDelegate.getPropertyPathforClasses(stores, new Class[] { Item.class }));
    }
}
