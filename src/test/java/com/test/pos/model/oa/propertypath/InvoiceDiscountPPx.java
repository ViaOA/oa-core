package com.test.pos.model.oa.propertypath;
 
import java.io.Serializable;
import com.test.pos.model.oa.*;
 
public class InvoiceDiscountPPx implements PPxInterface, Serializable {
    private static final long serialVersionUID = 1L;
    public final String pp;  // propertyPath
     
    public InvoiceDiscountPPx(String name) {
        this(null, name);
    }

    public InvoiceDiscountPPx(PPxInterface parent, String name) {
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

    public String id() {
        return pp + "." + InvoiceDiscount.P_Id;
    }

    public String created() {
        return pp + "." + InvoiceDiscount.P_Created;
    }

    public String name() {
        return pp + "." + InvoiceDiscount.P_Name;
    }

    public String type() {
        return pp + "." + InvoiceDiscount.P_Type;
    }

    public String amount() {
        return pp + "." + InvoiceDiscount.P_Amount;
    }

    public String percentage() {
        return pp + "." + InvoiceDiscount.P_Percentage;
    }

    @Override
    public String toString() {
        return pp;
    }
    public String pp() {
        return pp;
    }
}
 
