package com.test.pos.model.oa.propertypath;
 
import com.test.pos.model.oa.*;
 
public class ReturnedCheckFeePP {
    private static InvoicePaymentCheckPPx invoicePaymentCheck;
     

    public static InvoicePaymentCheckPPx invoicePaymentCheck() {
        if (invoicePaymentCheck == null) invoicePaymentCheck = new InvoicePaymentCheckPPx(ReturnedCheckFee.P_InvoicePaymentCheck);
        return invoicePaymentCheck;
    }

    public static String id() {
        String s = ReturnedCheckFee.P_Id;
        return s;
    }

    public static String created() {
        String s = ReturnedCheckFee.P_Created;
        return s;
    }

    public static String amount() {
        String s = ReturnedCheckFee.P_Amount;
        return s;
    }

    public static String collectedDate() {
        String s = ReturnedCheckFee.P_CollectedDate;
        return s;
    }

    public static String note() {
        String s = ReturnedCheckFee.P_Note;
        return s;
    }

    public static String status() {
        String s = ReturnedCheckFee.P_Status;
        return s;
    }

    public static String pp() {
        return ""; // this
    }
}
 
