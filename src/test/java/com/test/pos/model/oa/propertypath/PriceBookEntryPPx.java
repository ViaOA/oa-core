package com.test.pos.model.oa.propertypath;
 
import java.io.Serializable;
import com.test.pos.model.oa.*;
 
public class PriceBookEntryPPx implements PPxInterface, Serializable {
    private static final long serialVersionUID = 1L;
    public final String pp;  // propertyPath
     
    public PriceBookEntryPPx(String name) {
        this(null, name);
    }

    public PriceBookEntryPPx(PPxInterface parent, String name) {
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

    public ProductPPx calcForCurrentPriceBookEntry() {
        ProductPPx ppx = new ProductPPx(this, PriceBookEntry.P_CalcForCurrentPriceBookEntry);
        return ppx;
    }

    public ItemPPx item() {
        ItemPPx ppx = new ItemPPx(this, PriceBookEntry.P_Item);
        return ppx;
    }

    public ItemOptionValuePPx itemOptionValue() {
        ItemOptionValuePPx ppx = new ItemOptionValuePPx(this, PriceBookEntry.P_ItemOptionValue);
        return ppx;
    }

    public ItemPackPPx itemPack() {
        ItemPackPPx ppx = new ItemPackPPx(this, PriceBookEntry.P_ItemPack);
        return ppx;
    }

    public ProductPPx product() {
        ProductPPx ppx = new ProductPPx(this, PriceBookEntry.P_Product);
        return ppx;
    }

    public String id() {
        return pp + "." + PriceBookEntry.P_Id;
    }

    public String created() {
        return pp + "." + PriceBookEntry.P_Created;
    }

    public String name() {
        return pp + "." + PriceBookEntry.P_Name;
    }

    public String salePrice() {
        return pp + "." + PriceBookEntry.P_SalePrice;
    }

    public String fromDate() {
        return pp + "." + PriceBookEntry.P_FromDate;
    }

    public String toDate() {
        return pp + "." + PriceBookEntry.P_ToDate;
    }

    public String promotion() {
        return pp + "." + PriceBookEntry.P_Promotion;
    }

    public String priority() {
        return pp + "." + PriceBookEntry.P_Priority;
    }

    @Override
    public String toString() {
        return pp;
    }
    public String pp() {
        return pp;
    }
}
 
