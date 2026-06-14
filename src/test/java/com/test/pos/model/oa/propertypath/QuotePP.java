package com.test.pos.model.oa.propertypath;
 
import com.test.pos.model.oa.*;
 
public class QuotePP {
    private static CustomerPPx customer;
    private static InvoicePPx invoice;
     

    public static CustomerPPx customer() {
        if (customer == null) customer = new CustomerPPx(Quote.P_Customer);
        return customer;
    }

    public static InvoicePPx invoice() {
        if (invoice == null) invoice = new InvoicePPx(Quote.P_Invoice);
        return invoice;
    }

    public static String id() {
        String s = Quote.P_Id;
        return s;
    }

    public static String created() {
        String s = Quote.P_Created;
        return s;
    }

    public static String name() {
        String s = Quote.P_Name;
        return s;
    }

    public static String note() {
        String s = Quote.P_Note;
        return s;
    }

    public static String endDate() {
        String s = Quote.P_EndDate;
        return s;
    }

    public static String pp() {
        return ""; // this
    }
}
 
