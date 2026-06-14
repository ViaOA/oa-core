package com.test.pos.model.oa.propertypath;
 
import java.io.Serializable;
import com.test.pos.model.oa.*;
 
public class RefundInvoicePPx implements PPxInterface, Serializable {
    private static final long serialVersionUID = 1L;
    public final String pp;  // propertyPath
     
    public RefundInvoicePPx(String name) {
        this(null, name);
    }

    public RefundInvoicePPx(PPxInterface parent, String name) {
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

    public InvoicePPx invoice() {
        InvoicePPx ppx = new InvoicePPx(this, RefundInvoice.P_Invoice);
        return ppx;
    }

    public RefundPPx refund() {
        RefundPPx ppx = new RefundPPx(this, RefundInvoice.P_Refund);
        return ppx;
    }

    public RefundLineItemPPx refundLineItems() {
        RefundLineItemPPx ppx = new RefundLineItemPPx(this, RefundInvoice.P_RefundLineItems);
        return ppx;
    }

    public RefundPaymentPPx refundPayments() {
        RefundPaymentPPx ppx = new RefundPaymentPPx(this, RefundInvoice.P_RefundPayments);
        return ppx;
    }

    public String id() {
        return pp + "." + RefundInvoice.P_Id;
    }

    public String created() {
        return pp + "." + RefundInvoice.P_Created;
    }

    @Override
    public String toString() {
        return pp;
    }
    public String pp() {
        return pp;
    }
}
 
