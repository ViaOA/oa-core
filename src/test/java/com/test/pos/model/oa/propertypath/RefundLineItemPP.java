package com.test.pos.model.oa.propertypath;
 
import com.test.pos.model.oa.*;
 
public class RefundLineItemPP {
    private static LineItemPPx lineItem;
    private static RefundInvoicePPx refundInvoice;
    private static RefundLineItemTaxPPx refundLineItemTaxes;
     

    public static LineItemPPx lineItem() {
        if (lineItem == null) lineItem = new LineItemPPx(RefundLineItem.P_LineItem);
        return lineItem;
    }

    public static RefundInvoicePPx refundInvoice() {
        if (refundInvoice == null) refundInvoice = new RefundInvoicePPx(RefundLineItem.P_RefundInvoice);
        return refundInvoice;
    }

    public static RefundLineItemTaxPPx refundLineItemTaxes() {
        if (refundLineItemTaxes == null) refundLineItemTaxes = new RefundLineItemTaxPPx(RefundLineItem.P_RefundLineItemTaxes);
        return refundLineItemTaxes;
    }

    public static String id() {
        String s = RefundLineItem.P_Id;
        return s;
    }

    public static String created() {
        String s = RefundLineItem.P_Created;
        return s;
    }

    public static String quantity() {
        String s = RefundLineItem.P_Quantity;
        return s;
    }

    public static String priceEach() {
        String s = RefundLineItem.P_PriceEach;
        return s;
    }

    public static String pp() {
        return ""; // this
    }
}
 
