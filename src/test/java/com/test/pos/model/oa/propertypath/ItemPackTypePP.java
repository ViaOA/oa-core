package com.test.pos.model.oa.propertypath;
 
import com.test.pos.model.oa.*;
 
public class ItemPackTypePP {
    private static ItemPackPPx itemPacks;
     

    public static ItemPackPPx itemPacks() {
        if (itemPacks == null) itemPacks = new ItemPackPPx(ItemPackType.P_ItemPacks);
        return itemPacks;
    }

    public static String id() {
        String s = ItemPackType.P_Id;
        return s;
    }

    public static String created() {
        String s = ItemPackType.P_Created;
        return s;
    }

    public static String name() {
        String s = ItemPackType.P_Name;
        return s;
    }

    public static String type() {
        String s = ItemPackType.P_Type;
        return s;
    }

    public static String quantityInPack() {
        String s = ItemPackType.P_QuantityInPack;
        return s;
    }

    public static String pp() {
        return ""; // this
    }
}
 
