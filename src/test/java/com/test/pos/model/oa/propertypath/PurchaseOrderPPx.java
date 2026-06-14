package com.test.pos.model.oa.propertypath;
 
import java.io.Serializable;
import com.test.pos.model.oa.*;
 
public class PurchaseOrderPPx implements PPxInterface, Serializable {
    private static final long serialVersionUID = 1L;
    public final String pp;  // propertyPath
     
    public PurchaseOrderPPx(String name) {
        this(null, name);
    }

    public PurchaseOrderPPx(PPxInterface parent, String name) {
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

    public InvoicePPx invoices() {
        InvoicePPx ppx = new InvoicePPx(this, PurchaseOrder.P_Invoices);
        return ppx;
    }

    public String id() {
        return pp + "." + PurchaseOrder.P_Id;
    }

    public String created() {
        return pp + "." + PurchaseOrder.P_Created;
    }

    public String reference() {
        return pp + "." + PurchaseOrder.P_Reference;
    }

    @Override
    public String toString() {
        return pp;
    }
    public String pp() {
        return pp;
    }
}
 
