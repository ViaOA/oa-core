package com.test.pos.model.oa.propertypath;
 
import java.io.Serializable;
import com.test.pos.model.oa.*;
 
public class GaragePPx implements PPxInterface, Serializable {
    private static final long serialVersionUID = 1L;
    public final String pp;  // propertyPath
     
    public GaragePPx(String name) {
        this(null, name);
    }

    public GaragePPx(PPxInterface parent, String name) {
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

    public CustomerPPx customer() {
        CustomerPPx ppx = new CustomerPPx(this, Garage.P_Customer);
        return ppx;
    }

    public GarageVehiclePPx garageVehicles() {
        GarageVehiclePPx ppx = new GarageVehiclePPx(this, Garage.P_GarageVehicles);
        return ppx;
    }

    public String id() {
        return pp + "." + Garage.P_Id;
    }

    public String created() {
        return pp + "." + Garage.P_Created;
    }

    @Override
    public String toString() {
        return pp;
    }
    public String pp() {
        return pp;
    }
}
 
