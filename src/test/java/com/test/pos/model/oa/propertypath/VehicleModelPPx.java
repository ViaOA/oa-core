package com.test.pos.model.oa.propertypath;
 
import java.io.Serializable;
import com.test.pos.model.oa.*;
 
public class VehicleModelPPx implements PPxInterface, Serializable {
    private static final long serialVersionUID = 1L;
    public final String pp;  // propertyPath
     
    public VehicleModelPPx(String name) {
        this(null, name);
    }

    public VehicleModelPPx(PPxInterface parent, String name) {
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

    public CatalogItemPPx catalogItems() {
        CatalogItemPPx ppx = new CatalogItemPPx(this, VehicleModel.P_CatalogItems);
        return ppx;
    }

    public GarageVehiclePPx garageVehicles() {
        GarageVehiclePPx ppx = new GarageVehiclePPx(this, VehicleModel.P_GarageVehicles);
        return ppx;
    }

    public VehicleMakePPx vehicleMake() {
        VehicleMakePPx ppx = new VehicleMakePPx(this, VehicleModel.P_VehicleMake);
        return ppx;
    }

    public VehicleModelPackagePPx vehicleModelPackages() {
        VehicleModelPackagePPx ppx = new VehicleModelPackagePPx(this, VehicleModel.P_VehicleModelPackages);
        return ppx;
    }

    public VehicleModelYearPPx vehicleModelYears() {
        VehicleModelYearPPx ppx = new VehicleModelYearPPx(this, VehicleModel.P_VehicleModelYears);
        return ppx;
    }

    public String id() {
        return pp + "." + VehicleModel.P_Id;
    }

    public String created() {
        return pp + "." + VehicleModel.P_Created;
    }

    @Override
    public String toString() {
        return pp;
    }
    public String pp() {
        return pp;
    }
}
 
