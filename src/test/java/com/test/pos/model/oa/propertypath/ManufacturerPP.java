package com.test.pos.model.oa.propertypath;
 
import com.test.pos.model.oa.*;
 
public class ManufacturerPP {
    private static ItemPPx items;
     

    public static ItemPPx items() {
        if (items == null) items = new ItemPPx(Manufacturer.P_Items);
        return items;
    }

    public static String id() {
        String s = Manufacturer.P_Id;
        return s;
    }

    public static String created() {
        String s = Manufacturer.P_Created;
        return s;
    }

    public static String name() {
        String s = Manufacturer.P_Name;
        return s;
    }

    public static String pp() {
        return ""; // this
    }
}
 
