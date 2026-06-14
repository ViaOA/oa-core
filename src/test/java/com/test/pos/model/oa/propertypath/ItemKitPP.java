package com.test.pos.model.oa.propertypath;
 
import com.test.pos.model.oa.*;
 
public class ItemKitPP {
    private static ItemPPx item;
     

    public static ItemPPx item() {
        if (item == null) item = new ItemPPx(ItemKit.P_Item);
        return item;
    }

    public static String id() {
        String s = ItemKit.P_Id;
        return s;
    }

    public static String created() {
        String s = ItemKit.P_Created;
        return s;
    }

    public static String pp() {
        return ""; // this
    }
}
 
