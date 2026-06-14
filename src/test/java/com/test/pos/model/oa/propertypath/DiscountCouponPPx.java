package com.test.pos.model.oa.propertypath;
 
import java.io.Serializable;
import com.test.pos.model.oa.*;
 
public class DiscountCouponPPx implements PPxInterface, Serializable {
    private static final long serialVersionUID = 1L;
    public final String pp;  // propertyPath
     
    public DiscountCouponPPx(String name) {
        this(null, name);
    }

    public DiscountCouponPPx(PPxInterface parent, String name) {
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
        return pp + "." + DiscountCoupon.P_Id;
    }

    public String created() {
        return pp + "." + DiscountCoupon.P_Created;
    }

    public String amount() {
        return pp + "." + DiscountCoupon.P_Amount;
    }

    public String reference() {
        return pp + "." + DiscountCoupon.P_Reference;
    }

    @Override
    public String toString() {
        return pp;
    }
    public String pp() {
        return pp;
    }
}
 
