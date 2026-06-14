package com.test.pos.model.oa.propertypath;
 
import java.io.Serializable;
import com.test.pos.model.oa.*;
 
public class VehicleMakePPx implements PPxInterface, Serializable {
    private static final long serialVersionUID = 1L;
    public final String pp;  // propertyPath
     
    public VehicleMakePPx(String name) {
        this(null, name);
    }

    public VehicleMakePPx(PPxInterface parent, String name) {
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

    public VehicleModelPPx vehicleModels() {
        VehicleModelPPx ppx = new VehicleModelPPx(this, VehicleMake.P_VehicleModels);
        return ppx;
    }

    public String id() {
        return pp + "." + VehicleMake.P_Id;
    }

    public String created() {
        return pp + "." + VehicleMake.P_Created;
    }

    @Override
    public String toString() {
        return pp;
    }
    public String pp() {
        return pp;
    }
}
 
