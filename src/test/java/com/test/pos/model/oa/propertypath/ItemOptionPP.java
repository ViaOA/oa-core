package com.test.pos.model.oa.propertypath;
 
import com.test.pos.model.oa.*;
 
public class ItemOptionPP {
    private static ItemPPx item;
    private static ItemOptionTypePPx itemOptionType;
    private static ItemOptionValuePPx itemOptionValues;
     

    public static ItemPPx item() {
        if (item == null) item = new ItemPPx(ItemOption.P_Item);
        return item;
    }

    public static ItemOptionTypePPx itemOptionType() {
        if (itemOptionType == null) itemOptionType = new ItemOptionTypePPx(ItemOption.P_ItemOptionType);
        return itemOptionType;
    }

    public static ItemOptionValuePPx itemOptionValues() {
        if (itemOptionValues == null) itemOptionValues = new ItemOptionValuePPx(ItemOption.P_ItemOptionValues);
        return itemOptionValues;
    }

    public static String id() {
        String s = ItemOption.P_Id;
        return s;
    }

    public static String created() {
        String s = ItemOption.P_Created;
        return s;
    }

    public static String name() {
        String s = ItemOption.P_Name;
        return s;
    }

    public static String pp() {
        return ""; // this
    }
}
 
