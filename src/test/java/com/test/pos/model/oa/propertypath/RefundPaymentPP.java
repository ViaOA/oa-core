package com.test.pos.model.oa.propertypath;
 
import com.test.pos.model.oa.*;
 
public class RefundPaymentPP {
    private static InvoicePaymentPPx invoicePayment;
    private static RefundInvoicePPx refundInvoice;
    private static TillLedgerEntryPPx tillLedgerEntry;
     

    public static InvoicePaymentPPx invoicePayment() {
        if (invoicePayment == null) invoicePayment = new InvoicePaymentPPx(RefundPayment.P_InvoicePayment);
        return invoicePayment;
    }

    public static RefundInvoicePPx refundInvoice() {
        if (refundInvoice == null) refundInvoice = new RefundInvoicePPx(RefundPayment.P_RefundInvoice);
        return refundInvoice;
    }

    public static TillLedgerEntryPPx tillLedgerEntry() {
        if (tillLedgerEntry == null) tillLedgerEntry = new TillLedgerEntryPPx(RefundPayment.P_TillLedgerEntry);
        return tillLedgerEntry;
    }

    public static String id() {
        String s = RefundPayment.P_Id;
        return s;
    }

    public static String created() {
        String s = RefundPayment.P_Created;
        return s;
    }

    public static String amount() {
        String s = RefundPayment.P_Amount;
        return s;
    }

    public static String applied() {
        String s = RefundPayment.P_Applied;
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
 
