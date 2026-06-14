package com.test.pos.model.oa.propertypath;
 
import com.test.pos.model.oa.*;
 
public class StoreDayEndPP {
    private static StoreSchedulePPx storeSchedule;
     

    public static StoreSchedulePPx storeSchedule() {
        if (storeSchedule == null) storeSchedule = new StoreSchedulePPx(StoreDayEnd.P_StoreSchedule);
        return storeSchedule;
    }

    public static String id() {
        String s = StoreDayEnd.P_Id;
        return s;
    }

    public static String created() {
        String s = StoreDayEnd.P_Created;
        return s;
    }

    public static String pettyCash() {
        String s = StoreDayEnd.P_PettyCash;
        return s;
    }

    public static String pp() {
        return ""; // this
    }
}
 
