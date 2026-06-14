package com.test.pos.model.oa.propertypath;
 
import java.io.Serializable;
import com.test.pos.model.oa.*;
 
public class ItemOptionTypePPx implements PPxInterface, Serializable {
    private static final long serialVersionUID = 1L;
    public final String pp;  // propertyPath
     
    public ItemOptionTypePPx(String name) {
        this(null, name);
    }

    public ItemOptionTypePPx(PPxInterface parent, String name) {
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

    public ItemOptionPPx itemOptions() {
        ItemOptionPPx ppx = new ItemOptionPPx(this, ItemOptionType.P_ItemOptions);
        return ppx;
    }

    public ItemOptionTypeValuePPx itemOptionTypeValues() {
        ItemOptionTypeValuePPx ppx = new ItemOptionTypeValuePPx(this, ItemOptionType.P_ItemOptionTypeValues);
        return ppx;
    }

    public String id() {
        return pp + "." + ItemOptionType.P_Id;
    }

    public String created() {
        return pp + "." + ItemOptionType.P_Created;
    }

    public String type() {
        return pp + "." + ItemOptionType.P_Type;
    }

    public String name() {
        return pp + "." + ItemOptionType.P_Name;
    }

    @Override
    public String toString() {
        return pp;
    }
    public String pp() {
        return pp;
    }
}
 
