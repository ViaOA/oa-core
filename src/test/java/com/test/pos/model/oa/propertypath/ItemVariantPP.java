package com.test.pos.model.oa.propertypath;
 
import com.test.pos.model.oa.*;
 
public class ItemVariantPP {
    private static ItemPPx item;
    private static ItemOptionValuePPx itemOptionValues;
    private static ProductPPx products;
     

    public static ItemPPx item() {
        if (item == null) item = new ItemPPx(ItemVariant.P_Item);
        return item;
    }

    public static ItemOptionValuePPx itemOptionValues() {
        if (itemOptionValues == null) itemOptionValues = new ItemOptionValuePPx(ItemVariant.P_ItemOptionValues);
        return itemOptionValues;
    }

    public static ProductPPx products() {
        if (products == null) products = new ProductPPx(ItemVariant.P_Products);
        return products;
    }

    public static String id() {
        String s = ItemVariant.P_Id;
        return s;
    }

    public static String created() {
        String s = ItemVariant.P_Created;
        return s;
    }

    public static String name() {
        String s = ItemVariant.P_Name;
        return s;
    }

    public static String pp() {
        return ""; // this
    }
}
 
