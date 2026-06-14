package com.test.pos.model.oa.propertypath;
 
import java.io.Serializable;
import com.test.pos.model.oa.*;
 
public class ItemPackTypePPx implements PPxInterface, Serializable {
    private static final long serialVersionUID = 1L;
    public final String pp;  // propertyPath
     
    public ItemPackTypePPx(String name) {
        this(null, name);
    }

    public ItemPackTypePPx(PPxInterface parent, String name) {
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

    public ItemPackPPx itemPacks() {
        ItemPackPPx ppx = new ItemPackPPx(this, ItemPackType.P_ItemPacks);
        return ppx;
    }

    public String id() {
        return pp + "." + ItemPackType.P_Id;
    }

    public String created() {
        return pp + "." + ItemPackType.P_Created;
    }

    public String name() {
        return pp + "." + ItemPackType.P_Name;
    }

    public String type() {
        return pp + "." + ItemPackType.P_Type;
    }

    public String quantityInPack() {
        return pp + "." + ItemPackType.P_QuantityInPack;
    }

    @Override
    public String toString() {
        return pp;
    }
    public String pp() {
        return pp;
    }
}
 
