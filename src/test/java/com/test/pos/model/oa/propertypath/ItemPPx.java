package com.test.pos.model.oa.propertypath;
 
import java.io.Serializable;
import com.test.pos.model.oa.*;
 
public class ItemPPx implements PPxInterface, Serializable {
    private static final long serialVersionUID = 1L;
    public final String pp;  // propertyPath
     
    public ItemPPx(String name) {
        this(null, name);
    }

    public ItemPPx(PPxInterface parent, String name) {
        String s = null;
        if (parent != null) {
            s = parent.toString();
        }
        if (s == null) s = "";
        if (name != null && name.length() > 0) {
            if (s.length() > 0 && name.charAt(0) != ':') s += ".";
            s += name;
        }
        pp = s;
    }

    public CatalogItemPPx catalogItems() {
        CatalogItemPPx ppx = new CatalogItemPPx(this, Item.P_CatalogItems);
        return ppx;
    }

    public ItemCategoryPPx itemCategories() {
        ItemCategoryPPx ppx = new ItemCategoryPPx(this, Item.P_ItemCategories);
        return ppx;
    }

    public ItemKitPPx itemKits() {
        ItemKitPPx ppx = new ItemKitPPx(this, Item.P_ItemKits);
        return ppx;
    }

    public ItemLinePPx itemLine() {
        ItemLinePPx ppx = new ItemLinePPx(this, Item.P_ItemLine);
        return ppx;
    }

    public ItemOptionPPx itemOptions() {
        ItemOptionPPx ppx = new ItemOptionPPx(this, Item.P_ItemOptions);
        return ppx;
    }

    public ItemPackPPx itemPacks() {
        ItemPackPPx ppx = new ItemPackPPx(this, Item.P_ItemPacks);
        return ppx;
    }

    public ItemVariantPPx itemVariants() {
        ItemVariantPPx ppx = new ItemVariantPPx(this, Item.P_ItemVariants);
        return ppx;
    }

    public ItemVendorPPx itemVendors() {
        ItemVendorPPx ppx = new ItemVendorPPx(this, Item.P_ItemVendors);
        return ppx;
    }

    public ManufacturerPPx manufacturer() {
        ManufacturerPPx ppx = new ManufacturerPPx(this, Item.P_Manufacturer);
        return ppx;
    }

    public OnlineOrderItemPPx onlineOrderItems() {
        OnlineOrderItemPPx ppx = new OnlineOrderItemPPx(this, Item.P_OnlineOrderItems);
        return ppx;
    }

    public PriceBookEntryPPx priceBookEntries() {
        PriceBookEntryPPx ppx = new PriceBookEntryPPx(this, Item.P_PriceBookEntries);
        return ppx;
    }

    public ProductPPx products() {
        ProductPPx ppx = new ProductPPx(this, Item.P_Products);
        return ppx;
    }

    public StsItemPPx stsItems() {
        StsItemPPx ppx = new StsItemPPx(this, Item.P_StsItems);
        return ppx;
    }

    public VertexTaxCodePPx vertexTaxCodes() {
        VertexTaxCodePPx ppx = new VertexTaxCodePPx(this, Item.P_VertexTaxCodes);
        return ppx;
    }

    public String id() {
        return pp + "." + Item.P_Id;
    }

    public String created() {
        return pp + "." + Item.P_Created;
    }

    public String code() {
        return pp + "." + Item.P_Code;
    }

    public String name() {
        return pp + "." + Item.P_Name;
    }

    public String brand() {
        return pp + "." + Item.P_Brand;
    }

    public String description() {
        return pp + "." + Item.P_Description;
    }

    public String useSerialCode() {
        return pp + "." + Item.P_UseSerialCode;
    }

    public String serialCodeMask() {
        return pp + "." + Item.P_SerialCodeMask;
    }

    public String keywords() {
        return pp + "." + Item.P_Keywords;
    }

    public String htmlDescription() {
        return pp + "." + Item.P_HtmlDescription;
    }

    public String discontinued() {
        return pp + "." + Item.P_Discontinued;
    }

    public String discontinuedReason() {
        return pp + "." + Item.P_DiscontinuedReason;
    }

    public String stocking() {
        return pp + "." + Item.P_Stocking;
    }

    public String quantityOnHand() {
        return pp + "." + Item.P_QuantityOnHand;
    }

    public String minQuantityOnHand() {
        return pp + "." + Item.P_MinQuantityOnHand;
    }

    public String maxQuantityOnHand() {
        return pp + "." + Item.P_MaxQuantityOnHand;
    }

    public String shelfLifeInDays() {
        return pp + "." + Item.P_ShelfLifeInDays;
    }

    public String ageRestricted() {
        return pp + "." + Item.P_AgeRestricted;
    }

    public String minAge() {
        return pp + "." + Item.P_MinAge;
    }

    public String maxAge() {
        return pp + "." + Item.P_MaxAge;
    }

    public String saleByWeight() {
        return pp + "." + Item.P_SaleByWeight;
    }

    public String usedForKitOnly() {
        return pp + "." + Item.P_UsedForKitOnly;
    }

    public String notReturnable() {
        return pp + "." + Item.P_NotReturnable;
    }

    @Override
    public String toString() {
        return pp;
    }
    public String pp() {
        return pp;
    }
}
 
