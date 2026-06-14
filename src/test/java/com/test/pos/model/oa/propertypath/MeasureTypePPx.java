package com.test.pos.model.oa.propertypath;
 
import java.io.Serializable;
import com.test.pos.model.oa.*;
 
public class MeasureTypePPx implements PPxInterface, Serializable {
    private static final long serialVersionUID = 1L;
    public final String pp;  // propertyPath
     
    public MeasureTypePPx(String name) {
        this(null, name);
    }

    public MeasureTypePPx(PPxInterface parent, String name) {
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

    public String id() {
        return pp + "." + MeasureType.P_Id;
    }

    public String created() {
        return pp + "." + MeasureType.P_Created;
    }

    public String type() {
        return pp + "." + MeasureType.P_Type;
    }

    @Override
    public String toString() {
        return pp;
    }
    public String pp() {
        return pp;
    }
}
 
