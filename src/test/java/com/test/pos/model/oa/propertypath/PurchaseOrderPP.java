package com.test.pos.model.oa.propertypath;
 
import com.test.pos.model.oa.*;
 
public class PurchaseOrderPP {
    private static InvoicePPx invoices;
     

    public static InvoicePPx invoices() {
        if (invoices == null) invoices = new InvoicePPx(PurchaseOrder.P_Invoices);
        return invoices;
    }

    public static String id() {
        String s = PurchaseOrder.P_Id;
        return s;
    }

    public static String created() {
        String s = PurchaseOrder.P_Created;
        return s;
    }

    public static String reference() {
        String s = PurchaseOrder.P_Reference;
        return s;
    }

    public static String pp() {
        return ""; // this
    }
}
 
