package com.test.pos.model.oa.propertypath;
 
import com.test.pos.model.oa.*;
 
public class ItemLinePP {
    private static ItemPPx items;
     

    public static ItemPPx items() {
        if (items == null) items = new ItemPPx(ItemLine.P_Items);
        return items;
    }

    public static String id() {
        String s = ItemLine.P_Id;
        return s;
    }

    public static String created() {
        String s = ItemLine.P_Created;
        return s;
    }

    public static String code() {
        String s = ItemLine.P_Code;
        return s;
    }

    public static String name() {
        String s = ItemLine.P_Name;
        return s;
    }

    public static String seq() {
        String s = ItemLine.P_Seq;
        return s;
    }

    public static String pp() {
        return ""; // this
    }
}
 
