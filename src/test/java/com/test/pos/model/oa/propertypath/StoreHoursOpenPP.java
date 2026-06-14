package com.test.pos.model.oa.propertypath;
 
import com.test.pos.model.oa.*;
 
public class StoreHoursOpenPP {
    private static StorePPx store;
     

    public static StorePPx store() {
        if (store == null) store = new StorePPx(StoreHoursOpen.P_Store);
        return store;
    }

    public static String id() {
        String s = StoreHoursOpen.P_Id;
        return s;
    }

    public static String created() {
        String s = StoreHoursOpen.P_Created;
        return s;
    }

    public static String dayOfWeek() {
        String s = StoreHoursOpen.P_DayOfWeek;
        return s;
    }

    public static String openTime() {
        String s = StoreHoursOpen.P_OpenTime;
        return s;
    }

    public static String closeTime() {
        String s = StoreHoursOpen.P_CloseTime;
        return s;
    }

    public static String pp() {
        return ""; // this
    }
}
 
