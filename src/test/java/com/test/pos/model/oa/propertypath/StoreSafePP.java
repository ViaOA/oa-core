package com.test.pos.model.oa.propertypath;
 
import com.test.pos.model.oa.*;
 
public class StoreSafePP {
    private static BankDepositPPx bankDeposits;
    private static InvoicePaymentCheckPPx invoicePaymentChecks;
    private static StorePPx store;
    private static StoreSafeLedgerEntryPPx storeSafeLedgerEntries;
     

    public static BankDepositPPx bankDeposits() {
        if (bankDeposits == null) bankDeposits = new BankDepositPPx(StoreSafe.P_BankDeposits);
        return bankDeposits;
    }

    public static InvoicePaymentCheckPPx invoicePaymentChecks() {
        if (invoicePaymentChecks == null) invoicePaymentChecks = new InvoicePaymentCheckPPx(StoreSafe.P_InvoicePaymentChecks);
        return invoicePaymentChecks;
    }

    public static StorePPx store() {
        if (store == null) store = new StorePPx(StoreSafe.P_Store);
        return store;
    }

    public static StoreSafeLedgerEntryPPx storeSafeLedgerEntries() {
        if (storeSafeLedgerEntries == null) storeSafeLedgerEntries = new StoreSafeLedgerEntryPPx(StoreSafe.P_StoreSafeLedgerEntries);
        return storeSafeLedgerEntries;
    }

    public static String id() {
        String s = StoreSafe.P_Id;
        return s;
    }

    public static String created() {
        String s = StoreSafe.P_Created;
        return s;
    }

    public static String name() {
        String s = StoreSafe.P_Name;
        return s;
    }

    public static String cashAmount() {
        String s = StoreSafe.P_CashAmount;
        return s;
    }

    public static String pettyCashAmount() {
        String s = StoreSafe.P_PettyCashAmount;
        return s;
    }

    public static String checkCount() {
        String s = StoreSafe.P_CheckCount;
        return s;
    }

    public static String totalCheckAmount() {
        String s = StoreSafe.P_TotalCheckAmount;
        return s;
    }

    public static String pp() {
        return ""; // this
    }
}
 
