package com.test.pos.model.oa.propertypath;
 
import com.test.pos.model.oa.*;
 
public class DiscountCouponPP {
     

    public static String id() {
        String s = DiscountCoupon.P_Id;
        return s;
    }

    public static String created() {
        String s = DiscountCoupon.P_Created;
        return s;
    }

    public static String amount() {
        String s = DiscountCoupon.P_Amount;
        return s;
    }

    public static String reference() {
        String s = DiscountCoupon.P_Reference;
        return s;
    }

    public static String pp() {
        return ""; // this
    }
}
 
