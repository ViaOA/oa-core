package com.test.pos.model.oa.propertypath;
 
import java.io.Serializable;
import com.test.pos.model.oa.*;
 
public class ProductPPx implements PPxInterface, Serializable {
    private static final long serialVersionUID = 1L;
    public final String pp;  // propertyPath
     
    public ProductPPx(String name) {
        this(null, name);
    }

    public ProductPPx(PPxInterface parent, String name) {
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

    public PriceBookEntryPPx currentPriceBookEntry() {
        PriceBookEntryPPx ppx = new PriceBookEntryPPx(this, Product.P_CurrentPriceBookEntry);
        return ppx;
    }

    public ItemPPx item() {
        ItemPPx ppx = new ItemPPx(this, Product.P_Item);
        return ppx;
    }

    public ItemPackPPx itemPack() {
        ItemPackPPx ppx = new ItemPackPPx(this, Product.P_ItemPack);
        return ppx;
    }

    public ItemVariantPPx itemVariant() {
        ItemVariantPPx ppx = new ItemVariantPPx(this, Product.P_ItemVariant);
        return ppx;
    }

    public LineItemPPx lineItems() {
        LineItemPPx ppx = new LineItemPPx(this, Product.P_LineItems);
        return ppx;
    }

    public PriceBookEntryPPx priceBookEntries() {
        PriceBookEntryPPx ppx = new PriceBookEntryPPx(this, Product.P_PriceBookEntries);
        return ppx;
    }

    public ProductSerialCodePPx productSerialCodes() {
        ProductSerialCodePPx ppx = new ProductSerialCodePPx(this, Product.P_ProductSerialCodes);
        return ppx;
    }

    public ProductUpcPPx productUpcs() {
        ProductUpcPPx ppx = new ProductUpcPPx(this, Product.P_ProductUpcs);
        return ppx;
    }

    public String id() {
        return pp + "." + Product.P_Id;
    }

    public String created() {
        return pp + "." + Product.P_Created;
    }

    public String sku() {
        return pp + "." + Product.P_Sku;
    }

    public String quantityOnHand() {
        return pp + "." + Product.P_QuantityOnHand;
    }

    public String weight() {
        return pp + "." + Product.P_Weight;
    }

    public String sealedPackage() {
        return pp + "." + Product.P_SealedPackage;
    }

    public String discontinued() {
        return pp + "." + Product.P_Discontinued;
    }

    public String discontinuedReason() {
        return pp + "." + Product.P_DiscontinuedReason;
    }

    @Override
    public String toString() {
        return pp;
    }
    public String pp() {
        return pp;
    }
}
 
