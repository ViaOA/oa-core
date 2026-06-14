package com.test.pos.model.oa.propertypath;
 
import java.io.Serializable;
import com.test.pos.model.oa.*;
 
public class RefundLineItemTaxPPx implements PPxInterface, Serializable {
    private static final long serialVersionUID = 1L;
    public final String pp;  // propertyPath
     
    public RefundLineItemTaxPPx(String name) {
        this(null, name);
    }

    public RefundLineItemTaxPPx(PPxInterface parent, String name) {
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

    public RefundLineItemPPx refundLineItem() {
        RefundLineItemPPx ppx = new RefundLineItemPPx(this, RefundLineItemTax.P_RefundLineItem);
        return ppx;
    }

    public String id() {
        return pp + "." + RefundLineItemTax.P_Id;
    }

    public String created() {
        return pp + "." + RefundLineItemTax.P_Created;
    }

    public String taxPercent() {
        return pp + "." + RefundLineItemTax.P_TaxPercent;
    }

    @Override
    public String toString() {
        return pp;
    }
    public String pp() {
        return pp;
    }
}
 
