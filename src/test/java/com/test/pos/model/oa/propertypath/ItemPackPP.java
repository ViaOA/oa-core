package com.test.pos.model.oa.propertypath;
 
import com.test.pos.model.oa.*;
 
public class ItemPackPP {
    private static ItemPPx item;
    private static ItemPackTypePPx itemPackType;
    private static PriceBookEntryPPx priceBookEntries;
    private static ProductPPx products;
     

    public static ItemPPx item() {
        if (item == null) item = new ItemPPx(ItemPack.P_Item);
        return item;
    }

    public static ItemPackTypePPx itemPackType() {
        if (itemPackType == null) itemPackType = new ItemPackTypePPx(ItemPack.P_ItemPackType);
        return itemPackType;
    }

    public static PriceBookEntryPPx priceBookEntries() {
        if (priceBookEntries == null) priceBookEntries = new PriceBookEntryPPx(ItemPack.P_PriceBookEntries);
        return priceBookEntries;
    }

    public static ProductPPx products() {
        if (products == null) products = new ProductPPx(ItemPack.P_Products);
        return products;
    }

    public static String id() {
        String s = ItemPack.P_Id;
        return s;
    }

    public static String created() {
        String s = ItemPack.P_Created;
        return s;
    }

    public static String name() {
        String s = ItemPack.P_Name;
        return s;
    }

    public static String pp() {
        return ""; // this
    }
}
 
