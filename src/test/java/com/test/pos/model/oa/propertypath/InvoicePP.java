package com.test.pos.model.oa.propertypath;
 
import com.test.pos.model.oa.*;
 
public class InvoicePP {
    private static CustomerPPx customer;
    private static InvoiceBasketPPx invoiceBaskets;
    private static InvoicePaymentPPx invoicePayments;
    private static PurchaseOrderPPx purchaseOrders;
    private static QuotePPx quote;
    private static RefundInvoicePPx refundInvoices;
    private static RegisterSessionPPx registerSession;
     

    public static CustomerPPx customer() {
        if (customer == null) customer = new CustomerPPx(Invoice.P_Customer);
        return customer;
    }

    public static InvoiceBasketPPx invoiceBaskets() {
        if (invoiceBaskets == null) invoiceBaskets = new InvoiceBasketPPx(Invoice.P_InvoiceBaskets);
        return invoiceBaskets;
    }

    public static InvoicePaymentPPx invoicePayments() {
        if (invoicePayments == null) invoicePayments = new InvoicePaymentPPx(Invoice.P_InvoicePayments);
        return invoicePayments;
    }

    public static PurchaseOrderPPx purchaseOrders() {
        if (purchaseOrders == null) purchaseOrders = new PurchaseOrderPPx(Invoice.P_PurchaseOrders);
        return purchaseOrders;
    }

    public static QuotePPx quote() {
        if (quote == null) quote = new QuotePPx(Invoice.P_Quote);
        return quote;
    }

    public static RefundInvoicePPx refundInvoices() {
        if (refundInvoices == null) refundInvoices = new RefundInvoicePPx(Invoice.P_RefundInvoices);
        return refundInvoices;
    }

    public static RegisterSessionPPx registerSession() {
        if (registerSession == null) registerSession = new RegisterSessionPPx(Invoice.P_RegisterSession);
        return registerSession;
    }

    public static String id() {
        String s = Invoice.P_Id;
        return s;
    }

    public static String created() {
        String s = Invoice.P_Created;
        return s;
    }

    public static String completed() {
        String s = Invoice.P_Completed;
        return s;
    }

    public static String canBeCompleted() {
        String s = Invoice.P_CanBeCompleted;
        return s;
    }

    public static String totalItemAmount() {
        String s = Invoice.P_TotalItemAmount;
        return s;
    }

    public static String totalDiscountAmount() {
        String s = Invoice.P_TotalDiscountAmount;
        return s;
    }

    public static String totalTaxAmount() {
        String s = Invoice.P_TotalTaxAmount;
        return s;
    }

    public static String totalAmountDue() {
        String s = Invoice.P_TotalAmountDue;
        return s;
    }

    public static String totalPaymentAmount() {
        String s = Invoice.P_TotalPaymentAmount;
        return s;
    }

    public static String remainingBalanceAmount() {
        String s = Invoice.P_RemainingBalanceAmount;
        return s;
    }

    public static String totalRefundAmount() {
        String s = Invoice.P_TotalRefundAmount;
        return s;
    }

    public static String isPaidInFull() {
        String s = Invoice.P_IsPaidInFull;
        return s;
    }

    public static String updateWithNetPriceCaclulator() {
        String s = "updateWithNetPriceCaclulator";
        return s;
    }

    public static String completeSale() {
        String s = "completeSale";
        return s;
    }

    public static String pp() {
        return ""; // this
    }
}
 
