package com.test.pos.model.oa.propertypath;
 
import java.io.Serializable;
import com.test.pos.model.oa.*;
 
public class ItemLinePPx implements PPxInterface, Serializable {
    private static final long serialVersionUID = 1L;
    public final String pp;  // propertyPath
     
    public ItemLinePPx(String name) {
        this(null, name);
    }

    public ItemLinePPx(PPxInterface parent, String name) {
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
        ItemPPx ppx = new ItemPPx(this, ItemLine.P_Items);
        return ppx;
    }

    public String id() {
        return pp + "." + ItemLine.P_Id;
    }

    public String created() {
        return pp + "." + ItemLine.P_Created;
    }

    public String code() {
        return pp + "." + ItemLine.P_Code;
    }

    public String name() {
        return pp + "." + ItemLine.P_Name;
    }

    public String seq() {
        return pp + "." + ItemLine.P_Seq;
    }

    @Override
    public String toString() {
        return pp;
    }
    public String pp() {
        return pp;
    }
}
 
