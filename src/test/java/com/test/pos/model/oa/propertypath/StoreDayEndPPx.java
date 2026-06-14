package com.test.pos.model.oa.propertypath;
 
import java.io.Serializable;
import com.test.pos.model.oa.*;
 
public class StoreDayEndPPx implements PPxInterface, Serializable {
    private static final long serialVersionUID = 1L;
    public final String pp;  // propertyPath
     
    public StoreDayEndPPx(String name) {
        this(null, name);
    }

    public StoreDayEndPPx(PPxInterface parent, String name) {
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

    public StoreSchedulePPx storeSchedule() {
        StoreSchedulePPx ppx = new StoreSchedulePPx(this, StoreDayEnd.P_StoreSchedule);
        return ppx;
    }

    public String id() {
        return pp + "." + StoreDayEnd.P_Id;
    }

    public String created() {
        return pp + "." + StoreDayEnd.P_Created;
    }

    public String pettyCash() {
        return pp + "." + StoreDayEnd.P_PettyCash;
    }

    @Override
    public String toString() {
        return pp;
    }
    public String pp() {
        return pp;
    }
}
 
