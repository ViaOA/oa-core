package com.test.pos.model.oa.propertypath;
 
import java.io.Serializable;
import com.test.pos.model.oa.*;
 
public class LineItemDiscountPPx implements PPxInterface, Serializable {
    private static final long serialVersionUID = 1L;
    public final String pp;  // propertyPath
     
    public LineItemDiscountPPx(String name) {
        this(null, name);
    }

    public LineItemDiscountPPx(PPxInterface parent, String name) {
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
        return pp + "." + LineItemDiscount.P_Id;
    }

    public String created() {
        return pp + "." + LineItemDiscount.P_Created;
    }

    public String type() {
        return pp + "." + LineItemDiscount.P_Type;
    }

    public String percentage() {
        return pp + "." + LineItemDiscount.P_Percentage;
    }

    public String amount() {
        return pp + "." + LineItemDiscount.P_Amount;
    }

    @Override
    public String toString() {
        return pp;
    }
    public String pp() {
        return pp;
    }
}
 
