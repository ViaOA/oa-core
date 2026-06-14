package com.test.pos.model.oa.propertypath;
 
import java.io.Serializable;
import com.test.pos.model.oa.*;
 
public class StoreClosedDatePPx implements PPxInterface, Serializable {
    private static final long serialVersionUID = 1L;
    public final String pp;  // propertyPath
     
    public StoreClosedDatePPx(String name) {
        this(null, name);
    }

    public StoreClosedDatePPx(PPxInterface parent, String name) {
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

    public StorePPx store() {
        StorePPx ppx = new StorePPx(this, StoreClosedDate.P_Store);
        return ppx;
    }

    public String id() {
        return pp + "." + StoreClosedDate.P_Id;
    }

    public String created() {
        return pp + "." + StoreClosedDate.P_Created;
    }

    public String date() {
        return pp + "." + StoreClosedDate.P_Date;
    }

    public String reason() {
        return pp + "." + StoreClosedDate.P_Reason;
    }

    @Override
    public String toString() {
        return pp;
    }
    public String pp() {
        return pp;
    }
}
 
