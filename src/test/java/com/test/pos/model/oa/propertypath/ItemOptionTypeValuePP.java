package com.test.pos.model.oa.propertypath;
 
import com.test.pos.model.oa.*;
 
public class ItemOptionTypeValuePP {
    private static ItemOptionTypePPx itemOptionType;
     

    public static ItemOptionTypePPx itemOptionType() {
        if (itemOptionType == null) itemOptionType = new ItemOptionTypePPx(ItemOptionTypeValue.P_ItemOptionType);
        return itemOptionType;
    }

    public static String id() {
        String s = ItemOptionTypeValue.P_Id;
        return s;
    }

    public static String created() {
        String s = ItemOptionTypeValue.P_Created;
        return s;
    }

    public static String value() {
        String s = ItemOptionTypeValue.P_Value;
        return s;
    }

    public static String pp() {
        return ""; // this
    }
}
 
