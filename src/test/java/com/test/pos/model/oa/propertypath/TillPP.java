package com.test.pos.model.oa.propertypath;
 
import com.test.pos.model.oa.*;
 
public class TillPP {
    private static InvoicePaymentCheckPPx invoicePaymentChecks;
    private static RegisterPPx register;
    private static StorePPx store;
    private static TillLedgerEntryPPx tillLedgerEntries;
     

    public static InvoicePaymentCheckPPx invoicePaymentChecks() {
        if (invoicePaymentChecks == null) invoicePaymentChecks = new InvoicePaymentCheckPPx(Till.P_InvoicePaymentChecks);
        return invoicePaymentChecks;
    }

    public static RegisterPPx register() {
        if (register == null) register = new RegisterPPx(Till.P_Register);
        return register;
    }

    public static StorePPx store() {
        if (store == null) store = new StorePPx(Till.P_Store);
        return store;
    }

    public static TillLedgerEntryPPx tillLedgerEntries() {
        if (tillLedgerEntries == null) tillLedgerEntries = new TillLedgerEntryPPx(Till.P_TillLedgerEntries);
        return tillLedgerEntries;
    }

    public static String id() {
        String s = Till.P_Id;
        return s;
    }

    public static String created() {
        String s = Till.P_Created;
        return s;
    }

    public static String code() {
        String s = Till.P_Code;
        return s;
    }

    public static String cashAmount() {
        String s = Till.P_CashAmount;
        return s;
    }

    public static String totalCheckAmount() {
        String s = Till.P_TotalCheckAmount;
        return s;
    }

    public static String moveCashToSafe() {
        String s = "moveCashToSafe";
        return s;
    }

    public static String pp() {
        return ""; // this
    }
}
 
