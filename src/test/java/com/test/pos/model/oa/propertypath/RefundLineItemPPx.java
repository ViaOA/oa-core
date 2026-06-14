package com.test.pos.model.oa.propertypath;
 
import java.io.Serializable;
import com.test.pos.model.oa.*;
 
public class RefundLineItemPPx implements PPxInterface, Serializable {
    private static final long serialVersionUID = 1L;
    public final String pp;  // propertyPath
     
    public RefundLineItemPPx(String name) {
        this(null, name);
    }

    public RefundLineItemPPx(PPxInterface parent, String name) {
        String s = null;
        if (parent != null) {
            s = parent.toString();
        }
        if (s == null) s = "";
        if (name != null && name.length() > 0) {
            if (s.length() > 0 && name.charAt(0) != ':') s += ".";
            s += name;
        }
        pp = s;
    }

    public LineItemPPx lineItem() {
        LineItemPPx ppx = new LineItemPPx(this, RefundLineItem.P_LineItem);
        return ppx;
    }

    public RefundInvoicePPx refundInvoice() {
        RefundInvoicePPx ppx = new RefundInvoicePPx(this, RefundLineItem.P_RefundInvoice);
        return ppx;
    }

    public RefundLineItemTaxPPx refundLineItemTaxes() {
        RefundLineItemTaxPPx ppx = new RefundLineItemTaxPPx(this, RefundLineItem.P_RefundLineItemTaxes);
        return ppx;
    }

    public String id() {
        return pp + "." + RefundLineItem.P_Id;
    }

    public String created() {
        return pp + "." + RefundLineItem.P_Created;
    }

    public String quantity() {
        return pp + "." + RefundLineItem.P_Quantity;
    }

    public String priceEach() {
        return pp + "." + RefundLineItem.P_PriceEach;
    }

    @Override
    public String toString() {
        return pp;
    }
    public String pp() {
        return pp;
    }
}
 
