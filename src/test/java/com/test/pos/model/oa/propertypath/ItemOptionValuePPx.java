package com.test.pos.model.oa.propertypath;
 
import java.io.Serializable;
import com.test.pos.model.oa.*;
 
public class ItemOptionValuePPx implements PPxInterface, Serializable {
    private static final long serialVersionUID = 1L;
    public final String pp;  // propertyPath
     
    public ItemOptionValuePPx(String name) {
        this(null, name);
    }

    public ItemOptionValuePPx(PPxInterface parent, String name) {
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

    public ItemOptionPPx itemOption() {
        ItemOptionPPx ppx = new ItemOptionPPx(this, ItemOptionValue.P_ItemOption);
        return ppx;
    }

    public ItemVariantPPx itemVariants() {
        ItemVariantPPx ppx = new ItemVariantPPx(this, ItemOptionValue.P_ItemVariants);
        return ppx;
    }

    public PriceBookEntryPPx priceBookEntries() {
        PriceBookEntryPPx ppx = new PriceBookEntryPPx(this, ItemOptionValue.P_PriceBookEntries);
        return ppx;
    }

    public String id() {
        return pp + "." + ItemOptionValue.P_Id;
    }

    public String created() {
        return pp + "." + ItemOptionValue.P_Created;
    }

    public String value() {
        return pp + "." + ItemOptionValue.P_Value;
    }

    @Override
    public String toString() {
        return pp;
    }
    public String pp() {
        return pp;
    }
}
 
