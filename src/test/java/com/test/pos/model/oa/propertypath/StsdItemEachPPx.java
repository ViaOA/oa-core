package com.test.pos.model.oa.propertypath;
 
import java.io.Serializable;
import com.test.pos.model.oa.*;
 
public class StsdItemEachPPx implements PPxInterface, Serializable {
    private static final long serialVersionUID = 1L;
    public final String pp;  // propertyPath
     
    public StsdItemEachPPx(String name) {
        this(null, name);
    }

    public StsdItemEachPPx(PPxInterface parent, String name) {
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

    public StsdItemPPx stsdItem() {
        StsdItemPPx ppx = new StsdItemPPx(this, StsdItemEach.P_StsdItem);
        return ppx;
    }

    public String id() {
        return pp + "." + StsdItemEach.P_Id;
    }

    public String created() {
        return pp + "." + StsdItemEach.P_Created;
    }

    public String serialCode() {
        return pp + "." + StsdItemEach.P_SerialCode;
    }

    @Override
    public String toString() {
        return pp;
    }
    public String pp() {
        return pp;
    }
}
 
