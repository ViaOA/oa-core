package com.test.pos.model.oa.propertypath;
 
import java.io.Serializable;
import com.test.pos.model.oa.*;
 
public class InvoiceBasketPPx implements PPxInterface, Serializable {
    private static final long serialVersionUID = 1L;
    public final String pp;  // propertyPath
     
    public InvoiceBasketPPx(String name) {
        this(null, name);
    }

    public InvoiceBasketPPx(PPxInterface parent, String name) {
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
        InvoicePPx ppx = new InvoicePPx(this, InvoiceBasket.P_Invoice);
        return ppx;
    }

    public InvoiceShipToPPx invoiceShipTo() {
        InvoiceShipToPPx ppx = new InvoiceShipToPPx(this, InvoiceBasket.P_InvoiceShipTo);
        return ppx;
    }

    public LineItemPPx lineItems() {
        LineItemPPx ppx = new LineItemPPx(this, InvoiceBasket.P_LineItems);
        return ppx;
    }

    public String id() {
        return pp + "." + InvoiceBasket.P_Id;
    }

    public String created() {
        return pp + "." + InvoiceBasket.P_Created;
    }

    @Override
    public String toString() {
        return pp;
    }
    public String pp() {
        return pp;
    }
}
 
