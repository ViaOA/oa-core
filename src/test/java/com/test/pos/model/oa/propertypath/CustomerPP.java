package com.test.pos.model.oa.propertypath;
 
import com.test.pos.model.oa.*;
 
public class CustomerPP {
    private static AddressPPx addresses;
    private static CustomerCreditPPx customerCredit;
    private static GaragePPx garage;
    private static InvoicePPx invoices;
    private static OnlineOrderPPx onlineOrders;
    private static QuotePPx quotes;
     

    public static AddressPPx addresses() {
        if (addresses == null) addresses = new AddressPPx(Customer.P_Addresses);
        return addresses;
    }

    public static CustomerCreditPPx customerCredit() {
        if (customerCredit == null) customerCredit = new CustomerCreditPPx(Customer.P_CustomerCredit);
        return customerCredit;
    }

    public static GaragePPx garage() {
        if (garage == null) garage = new GaragePPx(Customer.P_Garage);
        return garage;
    }

    public static InvoicePPx invoices() {
        if (invoices == null) invoices = new InvoicePPx(Customer.P_Invoices);
        return invoices;
    }

    public static OnlineOrderPPx onlineOrders() {
        if (onlineOrders == null) onlineOrders = new OnlineOrderPPx(Customer.P_OnlineOrders);
        return onlineOrders;
    }

    public static QuotePPx quotes() {
        if (quotes == null) quotes = new QuotePPx(Customer.P_Quotes);
        return quotes;
    }

    public static String id() {
        String s = Customer.P_Id;
        return s;
    }

    public static String created() {
        String s = Customer.P_Created;
        return s;
    }

    public static String name() {
        String s = Customer.P_Name;
        return s;
    }

    public static String type() {
        String s = Customer.P_Type;
        return s;
    }

    public static String inputMask() {
        String s = Customer.P_InputMask;
        return s;
    }

    public static String pp() {
        return ""; // this
    }
}
 
