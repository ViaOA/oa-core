package com.test.pos.model.oa.propertypath;
 
import java.io.Serializable;
import com.test.pos.model.oa.*;
 
public class ItemVendorPPx implements PPxInterface, Serializable {
    private static final long serialVersionUID = 1L;
    public final String pp;  // propertyPath
     
    public ItemVendorPPx(String name) {
        this(null, name);
    }

    public ItemVendorPPx(PPxInterface parent, String name) {
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

    public ItemPPx items() {
        ItemPPx ppx = new ItemPPx(this, ItemVendor.P_Items);
        return ppx;
    }

    public String id() {
        return pp + "." + ItemVendor.P_Id;
    }

    public String created() {
        return pp + "." + ItemVendor.P_Created;
    }

    public String name() {
        return pp + "." + ItemVendor.P_Name;
    }

    @Override
    public String toString() {
        return pp;
    }
    public String pp() {
        return pp;
    }
}
 
