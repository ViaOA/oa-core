package com.test.pos.model.oa.propertypath;
 
import java.io.Serializable;
import com.test.pos.model.oa.*;
 
public class ItemOptionPPx implements PPxInterface, Serializable {
    private static final long serialVersionUID = 1L;
    public final String pp;  // propertyPath
     
    public ItemOptionPPx(String name) {
        this(null, name);
    }

    public ItemOptionPPx(PPxInterface parent, String name) {
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
        ItemPPx ppx = new ItemPPx(this, ItemOption.P_Item);
        return ppx;
    }

    public ItemOptionTypePPx itemOptionType() {
        ItemOptionTypePPx ppx = new ItemOptionTypePPx(this, ItemOption.P_ItemOptionType);
        return ppx;
    }

    public ItemOptionValuePPx itemOptionValues() {
        ItemOptionValuePPx ppx = new ItemOptionValuePPx(this, ItemOption.P_ItemOptionValues);
        return ppx;
    }

    public String id() {
        return pp + "." + ItemOption.P_Id;
    }

    public String created() {
        return pp + "." + ItemOption.P_Created;
    }

    public String name() {
        return pp + "." + ItemOption.P_Name;
    }

    @Override
    public String toString() {
        return pp;
    }
    public String pp() {
        return pp;
    }
}
 
