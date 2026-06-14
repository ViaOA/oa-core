package com.test.pos.model.oa.propertypath;
 
import java.io.Serializable;
import com.test.pos.model.oa.*;
 
public class VehicleModelPackagePPx implements PPxInterface, Serializable {
    private static final long serialVersionUID = 1L;
    public final String pp;  // propertyPath
     
    public VehicleModelPackagePPx(String name) {
        this(null, name);
    }

    public VehicleModelPackagePPx(PPxInterface parent, String name) {
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
        CatalogItemPPx ppx = new CatalogItemPPx(this, VehicleModelPackage.P_CatalogItems);
        return ppx;
    }

    public GarageVehiclePPx garageVehicles() {
        GarageVehiclePPx ppx = new GarageVehiclePPx(this, VehicleModelPackage.P_GarageVehicles);
        return ppx;
    }

    public VehicleModelPPx vehicleModel() {
        VehicleModelPPx ppx = new VehicleModelPPx(this, VehicleModelPackage.P_VehicleModel);
        return ppx;
    }

    public VehicleModelYearPPx vehicleModelYear() {
        VehicleModelYearPPx ppx = new VehicleModelYearPPx(this, VehicleModelPackage.P_VehicleModelYear);
        return ppx;
    }

    public VinLookupPPx vinLookup() {
        VinLookupPPx ppx = new VinLookupPPx(this, VehicleModelPackage.P_VinLookup);
        return ppx;
    }

    public String id() {
        return pp + "." + VehicleModelPackage.P_Id;
    }

    public String created() {
        return pp + "." + VehicleModelPackage.P_Created;
    }

    @Override
    public String toString() {
        return pp;
    }
    public String pp() {
        return pp;
    }
}
 
