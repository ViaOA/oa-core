package com.test.pos.model.oa.propertypath;
 
import com.test.pos.model.oa.*;
 
public class CustomerCreditPP {
    private static CustomerPPx customer;
     

    public static CustomerPPx customer() {
        if (customer == null) customer = new CustomerPPx(CustomerCredit.P_Customer);
        return customer;
    }

    public static String id() {
        String s = CustomerCredit.P_Id;
        return s;
    }

    public static String created() {
        String s = CustomerCredit.P_Created;
        return s;
    }

    public static String limit() {
        String s = CustomerCredit.P_Limit;
        return s;
    }

    public static String pp() {
        return ""; // this
    }
}
 
