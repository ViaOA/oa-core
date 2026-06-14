package com.test.pos.model.oa.propertypath;
 
import com.test.pos.model.oa.*;
 
public class ItemVendorPP {
    private static ItemPPx items;
     

    public static ItemPPx items() {
        if (items == null) items = new ItemPPx(ItemVendor.P_Items);
        return items;
    }

    public static String id() {
        String s = ItemVendor.P_Id;
        return s;
    }

    public static String created() {
        String s = ItemVendor.P_Created;
        return s;
    }

    public static String name() {
        String s = ItemVendor.P_Name;
        return s;
    }

    public static String pp() {
        return ""; // this
    }
}
 
