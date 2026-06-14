package com.test.pos.model.oa.propertypath;
 
import com.test.pos.model.oa.*;
 
public class ItemOptionTypePP {
    private static ItemOptionPPx itemOptions;
    private static ItemOptionTypeValuePPx itemOptionTypeValues;
     

    public static ItemOptionPPx itemOptions() {
        if (itemOptions == null) itemOptions = new ItemOptionPPx(ItemOptionType.P_ItemOptions);
        return itemOptions;
    }

    public static ItemOptionTypeValuePPx itemOptionTypeValues() {
        if (itemOptionTypeValues == null) itemOptionTypeValues = new ItemOptionTypeValuePPx(ItemOptionType.P_ItemOptionTypeValues);
        return itemOptionTypeValues;
    }

    public static String id() {
        String s = ItemOptionType.P_Id;
        return s;
    }

    public static String created() {
        String s = ItemOptionType.P_Created;
        return s;
    }

    public static String type() {
        String s = ItemOptionType.P_Type;
        return s;
    }

    public static String name() {
        String s = ItemOptionType.P_Name;
        return s;
    }

    public static String pp() {
        return ""; // this
    }
}
 
