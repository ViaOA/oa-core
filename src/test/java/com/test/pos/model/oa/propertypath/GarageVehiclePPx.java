package com.test.pos.model.oa.propertypath;
 
import java.io.Serializable;
import com.test.pos.model.oa.*;
 
public class GarageVehiclePPx implements PPxInterface, Serializable {
    private static final long serialVersionUID = 1L;
    public final String pp;  // propertyPath
     
    public GarageVehiclePPx(String name) {
        this(null, name);
    }

    public GarageVehiclePPx(PPxInterface parent, String name) {
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

    public GaragePPx garage() {
        GaragePPx ppx = new GaragePPx(this, GarageVehicle.P_Garage);
        return ppx;
    }

    public VehicleModelPPx vehicleModel() {
        VehicleModelPPx ppx = new VehicleModelPPx(this, GarageVehicle.P_VehicleModel);
        return ppx;
    }

    public VehicleModelPackagePPx vehicleModelPackage() {
        VehicleModelPackagePPx ppx = new VehicleModelPackagePPx(this, GarageVehicle.P_VehicleModelPackage);
        return ppx;
    }

    public String id() {
        return pp + "." + GarageVehicle.P_Id;
    }

    public String created() {
        return pp + "." + GarageVehicle.P_Created;
    }

    @Override
    public String toString() {
        return pp;
    }
    public String pp() {
        return pp;
    }
}
 
