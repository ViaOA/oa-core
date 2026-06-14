package com.test.pos.model.oa.propertypath;
 
import com.test.pos.model.oa.*;
 
public class InvoicePaymentPP {
    private static BankDepositCheckPPx bankDepositCheck;
    private static InvoicePPx invoice;
    private static InvoicePaymentCheckPPx invoicePaymentCheck;
    private static RefundPaymentPPx refundPayments;
    private static TillLedgerEntryPPx tillLedgerEntry;
     

    public static BankDepositCheckPPx bankDepositCheck() {
        if (bankDepositCheck == null) bankDepositCheck = new BankDepositCheckPPx(InvoicePayment.P_BankDepositCheck);
        return bankDepositCheck;
    }

    public static InvoicePPx invoice() {
        if (invoice == null) invoice = new InvoicePPx(InvoicePayment.P_Invoice);
        return invoice;
    }

    public static InvoicePaymentCheckPPx invoicePaymentCheck() {
        if (invoicePaymentCheck == null) invoicePaymentCheck = new InvoicePaymentCheckPPx(InvoicePayment.P_InvoicePaymentCheck);
        return invoicePaymentCheck;
    }

    public static RefundPaymentPPx refundPayments() {
        if (refundPayments == null) refundPayments = new RefundPaymentPPx(InvoicePayment.P_RefundPayments);
        return refundPayments;
    }

    public static TillLedgerEntryPPx tillLedgerEntry() {
        if (tillLedgerEntry == null) tillLedgerEntry = new TillLedgerEntryPPx(InvoicePayment.P_TillLedgerEntry);
        return tillLedgerEntry;
    }

    public static String id() {
        String s = InvoicePayment.P_Id;
        return s;
    }

    public static String created() {
        String s = InvoicePayment.P_Created;
        return s;
    }

    public static String type() {
        String s = InvoicePayment.P_Type;
        return s;
    }

    public static String inputCode() {
        String s = InvoicePayment.P_InputCode;
        return s;
    }

    public static String outputCode() {
        String s = InvoicePayment.P_OutputCode;
        return s;
    }

    public static String amount() {
        String s = InvoicePayment.P_Amount;
        return s;
    }

    public static String cashIn() {
        String s = InvoicePayment.P_CashIn;
        return s;
    }

    public static String cashOut() {
        String s = InvoicePayment.P_CashOut;
        return s;
    }

    public static String applied() {
        String s = InvoicePayment.P_Applied;
        return s;
    }

    public static String typeIsCash() {
        String s = InvoicePayment.P_TypeIsCash;
        return s;
    }

    public static String typeIsCheck() {
        String s = InvoicePayment.P_TypeIsCheck;
        return s;
    }

    public static String apply() {
        String s = "apply";
        return s;
    }

    public static String pp() {
        return ""; // this
    }
}
 
