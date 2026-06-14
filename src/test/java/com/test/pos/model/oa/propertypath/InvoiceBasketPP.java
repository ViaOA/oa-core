package com.test.pos.model.oa.propertypath;
 
import com.test.pos.model.oa.*;
 
public class InvoiceBasketPP {
    private static InvoicePPx invoice;
    private static InvoiceShipToPPx invoiceShipTo;
    private static LineItemPPx lineItems;
     

    public static InvoicePPx invoice() {
        if (invoice == null) invoice = new InvoicePPx(InvoiceBasket.P_Invoice);
        return invoice;
    }

    public static InvoiceShipToPPx invoiceShipTo() {
        if (invoiceShipTo == null) invoiceShipTo = new InvoiceShipToPPx(InvoiceBasket.P_InvoiceShipTo);
        return invoiceShipTo;
    }

    public static LineItemPPx lineItems() {
        if (lineItems == null) lineItems = new LineItemPPx(InvoiceBasket.P_LineItems);
        return lineItems;
    }

    public static String id() {
        String s = InvoiceBasket.P_Id;
        return s;
    }

    public static String created() {
        String s = InvoiceBasket.P_Created;
        return s;
    }

    public static String pp() {
        return ""; // this
    }
}
 
