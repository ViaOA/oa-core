package com.test.pos.model.oa.propertypath;
 
import com.test.pos.model.oa.*;
 
public class StoreClosedDatePP {
    private static StorePPx store;
     

    public static StorePPx store() {
        if (store == null) store = new StorePPx(StoreClosedDate.P_Store);
        return store;
    }

    public static String id() {
        String s = StoreClosedDate.P_Id;
        return s;
    }

    public static String created() {
        String s = StoreClosedDate.P_Created;
        return s;
    }

    public static String date() {
        String s = StoreClosedDate.P_Date;
        return s;
    }

    public static String reason() {
        String s = StoreClosedDate.P_Reason;
        return s;
    }

    public static String pp() {
        return ""; // this
    }
}
 
