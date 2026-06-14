package com.test.pos.model.oa.propertypath;
 
import java.io.Serializable;
import com.test.pos.model.oa.*;
 
public class InvoiceShipToPPx implements PPxInterface, Serializable {
    private static final long serialVersionUID = 1L;
    public final String pp;  // propertyPath
     
    public InvoiceShipToPPx(String name) {
        this(null, name);
    }

    public InvoiceShipToPPx(PPxInterface parent, String name) {
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

    public AddressPPx address() {
        AddressPPx ppx = new AddressPPx(this, InvoiceShipTo.P_Address);
        return ppx;
    }

    public InvoiceBasketPPx invoiceBasket() {
        InvoiceBasketPPx ppx = new InvoiceBasketPPx(this, InvoiceShipTo.P_InvoiceBasket);
        return ppx;
    }

    public String id() {
        return pp + "." + InvoiceShipTo.P_Id;
    }

    public String created() {
        return pp + "." + InvoiceShipTo.P_Created;
    }

    @Override
    public String toString() {
        return pp;
    }
    public String pp() {
        return pp;
    }
}
 
