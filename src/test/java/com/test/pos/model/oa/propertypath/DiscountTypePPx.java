package com.test.pos.model.oa.propertypath;
 
import java.io.Serializable;
import com.test.pos.model.oa.*;
 
public class DiscountTypePPx implements PPxInterface, Serializable {
    private static final long serialVersionUID = 1L;
    public final String pp;  // propertyPath
     
    public DiscountTypePPx(String name) {
        this(null, name);
    }

    public DiscountTypePPx(PPxInterface parent, String name) {
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
        return pp + "." + DiscountType.P_Id;
    }

    public String created() {
        return pp + "." + DiscountType.P_Created;
    }

    public String type() {
        return pp + "." + DiscountType.P_Type;
    }

    public String type2() {
        return pp + "." + DiscountType.P_Type2;
    }

    public String name() {
        return pp + "." + DiscountType.P_Name;
    }

    @Override
    public String toString() {
        return pp;
    }
    public String pp() {
        return pp;
    }
}
 
