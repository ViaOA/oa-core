package com.test.pos.model.oa.propertypath;
 
import com.test.pos.model.oa.*;
 
public class InvoiceShipToPP {
    private static AddressPPx address;
    private static InvoiceBasketPPx invoiceBasket;
     

    public static AddressPPx address() {
        if (address == null) address = new AddressPPx(InvoiceShipTo.P_Address);
        return address;
    }

    public static InvoiceBasketPPx invoiceBasket() {
        if (invoiceBasket == null) invoiceBasket = new InvoiceBasketPPx(InvoiceShipTo.P_InvoiceBasket);
        return invoiceBasket;
    }

    public static String id() {
        String s = InvoiceShipTo.P_Id;
        return s;
    }

    public static String created() {
        String s = InvoiceShipTo.P_Created;
        return s;
    }

    public static String pp() {
        return ""; // this
    }
}
 
