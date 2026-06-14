package com.test.pos.model.oa.propertypath;
 
import com.test.pos.model.oa.*;
 
public class OodItemEachPP {
    private static OodItemPPx oodItem;
     

    public static OodItemPPx oodItem() {
        if (oodItem == null) oodItem = new OodItemPPx(OodItemEach.P_OodItem);
        return oodItem;
    }

    public static String id() {
        String s = OodItemEach.P_Id;
        return s;
    }

    public static String created() {
        String s = OodItemEach.P_Created;
        return s;
    }

    public static String serialCode() {
        String s = OodItemEach.P_SerialCode;
        return s;
    }

    public static String pp() {
        return ""; // this
    }
}
 
