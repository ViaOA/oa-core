package com.test.pos.model.oa.propertypath;
 
import com.test.pos.model.oa.*;
 
public class InvoicePaymentCheckPP {
    private static InvoicePaymentPPx invoicePayment;
    private static ReturnedCheckFeePPx returnedCheckFee;
    private static StoreSafePPx storeSafe;
    private static StoreSafeLedgerEntryPPx storeSafeLedgerEntries;
    private static TillPPx till;
    private static TillLedgerEntryPPx tillLedgerEntries;
     

    public static InvoicePaymentPPx invoicePayment() {
        if (invoicePayment == null) invoicePayment = new InvoicePaymentPPx(InvoicePaymentCheck.P_InvoicePayment);
        return invoicePayment;
    }

    public static ReturnedCheckFeePPx returnedCheckFee() {
        if (returnedCheckFee == null) returnedCheckFee = new ReturnedCheckFeePPx(InvoicePaymentCheck.P_ReturnedCheckFee);
        return returnedCheckFee;
    }

    public static StoreSafePPx storeSafe() {
        if (storeSafe == null) storeSafe = new StoreSafePPx(InvoicePaymentCheck.P_StoreSafe);
        return storeSafe;
    }

    public static StoreSafeLedgerEntryPPx storeSafeLedgerEntries() {
        if (storeSafeLedgerEntries == null) storeSafeLedgerEntries = new StoreSafeLedgerEntryPPx(InvoicePaymentCheck.P_StoreSafeLedgerEntries);
        return storeSafeLedgerEntries;
    }

    public static TillPPx till() {
        if (till == null) till = new TillPPx(InvoicePaymentCheck.P_Till);
        return till;
    }

    public static TillLedgerEntryPPx tillLedgerEntries() {
        if (tillLedgerEntries == null) tillLedgerEntries = new TillLedgerEntryPPx(InvoicePaymentCheck.P_TillLedgerEntries);
        return tillLedgerEntries;
    }

    public static String id() {
        String s = InvoicePaymentCheck.P_Id;
        return s;
    }

    public static String created() {
        String s = InvoicePaymentCheck.P_Created;
        return s;
    }

    public static String location() {
        String s = InvoicePaymentCheck.P_Location;
        return s;
    }

    public static String checkNumber() {
        String s = InvoicePaymentCheck.P_CheckNumber;
        return s;
    }

    public static String bankName() {
        String s = InvoicePaymentCheck.P_BankName;
        return s;
    }

    public static String routingNumber() {
        String s = InvoicePaymentCheck.P_RoutingNumber;
        return s;
    }

    public static String accountNumber() {
        String s = InvoicePaymentCheck.P_AccountNumber;
        return s;
    }

    public static String checkDate() {
        String s = InvoicePaymentCheck.P_CheckDate;
        return s;
    }

    public static String status() {
        String s = InvoicePaymentCheck.P_Status;
        return s;
    }

    public static String clearDate() {
        String s = InvoicePaymentCheck.P_ClearDate;
        return s;
    }

    public static String bouncedDate() {
        String s = InvoicePaymentCheck.P_BouncedDate;
        return s;
    }

    public static String bouncedReason() {
        String s = InvoicePaymentCheck.P_BouncedReason;
        return s;
    }

    public static String licenseNumber() {
        String s = InvoicePaymentCheck.P_LicenseNumber;
        return s;
    }

    public static String licenseState() {
        String s = InvoicePaymentCheck.P_LicenseState;
        return s;
    }

    public static String pp() {
        return ""; // this
    }
}
 
