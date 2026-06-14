package com.test.pos.model.oa.propertypath;
 
import com.test.pos.model.oa.*;
 
public class StsdItemEachPP {
    private static StsdItemPPx stsdItem;
     

    public static StsdItemPPx stsdItem() {
        if (stsdItem == null) stsdItem = new StsdItemPPx(StsdItemEach.P_StsdItem);
        return stsdItem;
    }

    public static String id() {
        String s = StsdItemEach.P_Id;
        return s;
    }

    public static String created() {
        String s = StsdItemEach.P_Created;
        return s;
    }

    public static String serialCode() {
        String s = StsdItemEach.P_SerialCode;
        return s;
    }

    public static String pp() {
        return ""; // this
    }
}
 
