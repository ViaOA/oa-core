package com.test.pos.model.oa.propertypath;
 
import com.test.pos.model.oa.*;
 
public class RefundLineItemTaxPP {
    private static RefundLineItemPPx refundLineItem;
     

    public static RefundLineItemPPx refundLineItem() {
        if (refundLineItem == null) refundLineItem = new RefundLineItemPPx(RefundLineItemTax.P_RefundLineItem);
        return refundLineItem;
    }

    public static String id() {
        String s = RefundLineItemTax.P_Id;
        return s;
    }

    public static String created() {
        String s = RefundLineItemTax.P_Created;
        return s;
    }

    public static String taxPercent() {
        String s = RefundLineItemTax.P_TaxPercent;
        return s;
    }

    public static String pp() {
        return ""; // this
    }
}
 
