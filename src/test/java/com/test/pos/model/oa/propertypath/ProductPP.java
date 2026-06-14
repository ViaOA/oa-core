package com.test.pos.model.oa.propertypath;
 
import com.test.pos.model.oa.*;
 
public class ProductPP {
    private static PriceBookEntryPPx currentPriceBookEntry;
    private static ItemPPx item;
    private static ItemPackPPx itemPack;
    private static ItemVariantPPx itemVariant;
    private static LineItemPPx lineItems;
    private static PriceBookEntryPPx priceBookEntries;
    private static ProductSerialCodePPx productSerialCodes;
    private static ProductUpcPPx productUpcs;
     

    public static PriceBookEntryPPx currentPriceBookEntry() {
        if (currentPriceBookEntry == null) currentPriceBookEntry = new PriceBookEntryPPx(Product.P_CurrentPriceBookEntry);
        return currentPriceBookEntry;
    }

    public static ItemPPx item() {
        if (item == null) item = new ItemPPx(Product.P_Item);
        return item;
    }

    public static ItemPackPPx itemPack() {
        if (itemPack == null) itemPack = new ItemPackPPx(Product.P_ItemPack);
        return itemPack;
    }

    public static ItemVariantPPx itemVariant() {
        if (itemVariant == null) itemVariant = new ItemVariantPPx(Product.P_ItemVariant);
        return itemVariant;
    }

    public static LineItemPPx lineItems() {
        if (lineItems == null) lineItems = new LineItemPPx(Product.P_LineItems);
        return lineItems;
    }

    public static PriceBookEntryPPx priceBookEntries() {
        if (priceBookEntries == null) priceBookEntries = new PriceBookEntryPPx(Product.P_PriceBookEntries);
        return priceBookEntries;
    }

    public static ProductSerialCodePPx productSerialCodes() {
        if (productSerialCodes == null) productSerialCodes = new ProductSerialCodePPx(Product.P_ProductSerialCodes);
        return productSerialCodes;
    }

    public static ProductUpcPPx productUpcs() {
        if (productUpcs == null) productUpcs = new ProductUpcPPx(Product.P_ProductUpcs);
        return productUpcs;
    }

    public static String id() {
        String s = Product.P_Id;
        return s;
    }

    public static String created() {
        String s = Product.P_Created;
        return s;
    }

    public static String sku() {
        String s = Product.P_Sku;
        return s;
    }

    public static String quantityOnHand() {
        String s = Product.P_QuantityOnHand;
        return s;
    }

    public static String weight() {
        String s = Product.P_Weight;
        return s;
    }

    public static String sealedPackage() {
        String s = Product.P_SealedPackage;
        return s;
    }

    public static String discontinued() {
        String s = Product.P_Discontinued;
        return s;
    }

    public static String discontinuedReason() {
        String s = Product.P_DiscontinuedReason;
        return s;
    }

    public static String pp() {
        return ""; // this
    }
}
 
