package com.test.pos.model.oa.propertypath;
 
import java.io.Serializable;
import com.test.pos.model.oa.*;
 
public class ItemOptionTypeValuePPx implements PPxInterface, Serializable {
    private static final long serialVersionUID = 1L;
    public final String pp;  // propertyPath
     
    public ItemOptionTypeValuePPx(String name) {
        this(null, name);
    }

    public ItemOptionTypeValuePPx(PPxInterface parent, String name) {
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

    public ItemOptionTypePPx itemOptionType() {
        ItemOptionTypePPx ppx = new ItemOptionTypePPx(this, ItemOptionTypeValue.P_ItemOptionType);
        return ppx;
    }

    public String id() {
        return pp + "." + ItemOptionTypeValue.P_Id;
    }

    public String created() {
        return pp + "." + ItemOptionTypeValue.P_Created;
    }

    public String value() {
        return pp + "." + ItemOptionTypeValue.P_Value;
    }

    @Override
    public String toString() {
        return pp;
    }
    public String pp() {
        return pp;
    }
}
 
