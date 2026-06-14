package com.test.pos.model.oa.propertypath;
 
import com.test.pos.model.oa.*;
 
public class PriceBookEntryPP {
    private static ProductPPx calcForCurrentPriceBookEntry;
    private static ItemPPx item;
    private static ItemOptionValuePPx itemOptionValue;
    private static ItemPackPPx itemPack;
    private static ProductPPx product;
     

    public static ProductPPx calcForCurrentPriceBookEntry() {
        if (calcForCurrentPriceBookEntry == null) calcForCurrentPriceBookEntry = new ProductPPx(PriceBookEntry.P_CalcForCurrentPriceBookEntry);
        return calcForCurrentPriceBookEntry;
    }

    public static ItemPPx item() {
        if (item == null) item = new ItemPPx(PriceBookEntry.P_Item);
        return item;
    }

    public static ItemOptionValuePPx itemOptionValue() {
        if (itemOptionValue == null) itemOptionValue = new ItemOptionValuePPx(PriceBookEntry.P_ItemOptionValue);
        return itemOptionValue;
    }

    public static ItemPackPPx itemPack() {
        if (itemPack == null) itemPack = new ItemPackPPx(PriceBookEntry.P_ItemPack);
        return itemPack;
    }

    public static ProductPPx product() {
        if (product == null) product = new ProductPPx(PriceBookEntry.P_Product);
        return product;
    }

    public static String id() {
        String s = PriceBookEntry.P_Id;
        return s;
    }

    public static String created() {
        String s = PriceBookEntry.P_Created;
        return s;
    }

    public static String name() {
        String s = PriceBookEntry.P_Name;
        return s;
    }

    public static String salePrice() {
        String s = PriceBookEntry.P_SalePrice;
        return s;
    }

    public static String fromDate() {
        String s = PriceBookEntry.P_FromDate;
        return s;
    }

    public static String toDate() {
        String s = PriceBookEntry.P_ToDate;
        return s;
    }

    public static String promotion() {
        String s = PriceBookEntry.P_Promotion;
        return s;
    }

    public static String priority() {
        String s = PriceBookEntry.P_Priority;
        return s;
    }

    public static String pp() {
        return ""; // this
    }
}
 
