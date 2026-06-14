package com.test.pos.model.oa.propertypath;
 
import com.test.pos.model.oa.*;
 
public class ItemPP {
    private static CatalogItemPPx catalogItems;
    private static ItemCategoryPPx itemCategories;
    private static ItemKitPPx itemKits;
    private static ItemLinePPx itemLine;
    private static ItemOptionPPx itemOptions;
    private static ItemPackPPx itemPacks;
    private static ItemVariantPPx itemVariants;
    private static ItemVendorPPx itemVendors;
    private static ManufacturerPPx manufacturer;
    private static OnlineOrderItemPPx onlineOrderItems;
    private static PriceBookEntryPPx priceBookEntries;
    private static ProductPPx products;
    private static StsItemPPx stsItems;
    private static VertexTaxCodePPx vertexTaxCodes;
     

    public static CatalogItemPPx catalogItems() {
        if (catalogItems == null) catalogItems = new CatalogItemPPx(Item.P_CatalogItems);
        return catalogItems;
    }

    public static ItemCategoryPPx itemCategories() {
        if (itemCategories == null) itemCategories = new ItemCategoryPPx(Item.P_ItemCategories);
        return itemCategories;
    }

    public static ItemKitPPx itemKits() {
        if (itemKits == null) itemKits = new ItemKitPPx(Item.P_ItemKits);
        return itemKits;
    }

    public static ItemLinePPx itemLine() {
        if (itemLine == null) itemLine = new ItemLinePPx(Item.P_ItemLine);
        return itemLine;
    }

    public static ItemOptionPPx itemOptions() {
        if (itemOptions == null) itemOptions = new ItemOptionPPx(Item.P_ItemOptions);
        return itemOptions;
    }

    public static ItemPackPPx itemPacks() {
        if (itemPacks == null) itemPacks = new ItemPackPPx(Item.P_ItemPacks);
        return itemPacks;
    }

    public static ItemVariantPPx itemVariants() {
        if (itemVariants == null) itemVariants = new ItemVariantPPx(Item.P_ItemVariants);
        return itemVariants;
    }

    public static ItemVendorPPx itemVendors() {
        if (itemVendors == null) itemVendors = new ItemVendorPPx(Item.P_ItemVendors);
        return itemVendors;
    }

    public static ManufacturerPPx manufacturer() {
        if (manufacturer == null) manufacturer = new ManufacturerPPx(Item.P_Manufacturer);
        return manufacturer;
    }

    public static OnlineOrderItemPPx onlineOrderItems() {
        if (onlineOrderItems == null) onlineOrderItems = new OnlineOrderItemPPx(Item.P_OnlineOrderItems);
        return onlineOrderItems;
    }

    public static PriceBookEntryPPx priceBookEntries() {
        if (priceBookEntries == null) priceBookEntries = new PriceBookEntryPPx(Item.P_PriceBookEntries);
        return priceBookEntries;
    }

    public static ProductPPx products() {
        if (products == null) products = new ProductPPx(Item.P_Products);
        return products;
    }

    public static StsItemPPx stsItems() {
        if (stsItems == null) stsItems = new StsItemPPx(Item.P_StsItems);
        return stsItems;
    }

    public static VertexTaxCodePPx vertexTaxCodes() {
        if (vertexTaxCodes == null) vertexTaxCodes = new VertexTaxCodePPx(Item.P_VertexTaxCodes);
        return vertexTaxCodes;
    }

    public static String id() {
        String s = Item.P_Id;
        return s;
    }

    public static String created() {
        String s = Item.P_Created;
        return s;
    }

    public static String code() {
        String s = Item.P_Code;
        return s;
    }

    public static String name() {
        String s = Item.P_Name;
        return s;
    }

    public static String brand() {
        String s = Item.P_Brand;
        return s;
    }

    public static String description() {
        String s = Item.P_Description;
        return s;
    }

    public static String useSerialCode() {
        String s = Item.P_UseSerialCode;
        return s;
    }

    public static String serialCodeMask() {
        String s = Item.P_SerialCodeMask;
        return s;
    }

    public static String keywords() {
        String s = Item.P_Keywords;
        return s;
    }

    public static String htmlDescription() {
        String s = Item.P_HtmlDescription;
        return s;
    }

    public static String discontinued() {
        String s = Item.P_Discontinued;
        return s;
    }

    public static String discontinuedReason() {
        String s = Item.P_DiscontinuedReason;
        return s;
    }

    public static String stocking() {
        String s = Item.P_Stocking;
        return s;
    }

    public static String quantityOnHand() {
        String s = Item.P_QuantityOnHand;
        return s;
    }

    public static String minQuantityOnHand() {
        String s = Item.P_MinQuantityOnHand;
        return s;
    }

    public static String maxQuantityOnHand() {
        String s = Item.P_MaxQuantityOnHand;
        return s;
    }

    public static String shelfLifeInDays() {
        String s = Item.P_ShelfLifeInDays;
        return s;
    }

    public static String ageRestricted() {
        String s = Item.P_AgeRestricted;
        return s;
    }

    public static String minAge() {
        String s = Item.P_MinAge;
        return s;
    }

    public static String maxAge() {
        String s = Item.P_MaxAge;
        return s;
    }

    public static String saleByWeight() {
        String s = Item.P_SaleByWeight;
        return s;
    }

    public static String usedForKitOnly() {
        String s = Item.P_UsedForKitOnly;
        return s;
    }

    public static String notReturnable() {
        String s = Item.P_NotReturnable;
        return s;
    }

    public static String pp() {
        return ""; // this
    }
}
 
