package com.test.pos.model.oa.propertypath;
 
import com.test.pos.model.oa.*;
 
public class RefundInvoicePP {
    private static InvoicePPx invoice;
    private static RefundPPx refund;
    private static RefundLineItemPPx refundLineItems;
    private static RefundPaymentPPx refundPayments;
     

    public static InvoicePPx invoice() {
        if (invoice == null) invoice = new InvoicePPx(RefundInvoice.P_Invoice);
        return invoice;
    }

    public static RefundPPx refund() {
        if (refund == null) refund = new RefundPPx(RefundInvoice.P_Refund);
        return refund;
    }

    public static RefundLineItemPPx refundLineItems() {
        if (refundLineItems == null) refundLineItems = new RefundLineItemPPx(RefundInvoice.P_RefundLineItems);
        return refundLineItems;
    }

    public static RefundPaymentPPx refundPayments() {
        if (refundPayments == null) refundPayments = new RefundPaymentPPx(RefundInvoice.P_RefundPayments);
        return refundPayments;
    }

    public static String id() {
        String s = RefundInvoice.P_Id;
        return s;
    }

    public static String created() {
        String s = RefundInvoice.P_Created;
        return s;
    }

    public static String pp() {
        return ""; // this
    }
}
 
