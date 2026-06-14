package com.test.pos.model.oa.propertypath;
 
import java.io.Serializable;
import com.test.pos.model.oa.*;
 
public class ItemPackPPx implements PPxInterface, Serializable {
    private static final long serialVersionUID = 1L;
    public final String pp;  // propertyPath
     
    public ItemPackPPx(String name) {
        this(null, name);
    }

    public ItemPackPPx(PPxInterface parent, String name) {
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

    public ItemPPx item() {
        ItemPPx ppx = new ItemPPx(this, ItemPack.P_Item);
        return ppx;
    }

    public ItemPackTypePPx itemPackType() {
        ItemPackTypePPx ppx = new ItemPackTypePPx(this, ItemPack.P_ItemPackType);
        return ppx;
    }

    public PriceBookEntryPPx priceBookEntries() {
        PriceBookEntryPPx ppx = new PriceBookEntryPPx(this, ItemPack.P_PriceBookEntries);
        return ppx;
    }

    public ProductPPx products() {
        ProductPPx ppx = new ProductPPx(this, ItemPack.P_Products);
        return ppx;
    }

    public String id() {
        return pp + "." + ItemPack.P_Id;
    }

    public String created() {
        return pp + "." + ItemPack.P_Created;
    }

    public String name() {
        return pp + "." + ItemPack.P_Name;
    }

    @Override
    public String toString() {
        return pp;
    }
    public String pp() {
        return pp;
    }
}
 
