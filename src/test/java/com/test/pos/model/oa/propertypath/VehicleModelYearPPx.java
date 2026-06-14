package com.test.pos.model.oa.propertypath;
 
import java.io.Serializable;
import com.test.pos.model.oa.*;
 
public class VehicleModelYearPPx implements PPxInterface, Serializable {
    private static final long serialVersionUID = 1L;
    public final String pp;  // propertyPath
     
    public VehicleModelYearPPx(String name) {
        this(null, name);
    }

    public VehicleModelYearPPx(PPxInterface parent, String name) {
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

    public VehicleModelPPx vehicleModel() {
        VehicleModelPPx ppx = new VehicleModelPPx(this, VehicleModelYear.P_VehicleModel);
        return ppx;
    }

    public VehicleModelPackagePPx vehicleModelPackages() {
        VehicleModelPackagePPx ppx = new VehicleModelPackagePPx(this, VehicleModelYear.P_VehicleModelPackages);
        return ppx;
    }

    public VinLookupPPx vinLookups() {
        VinLookupPPx ppx = new VinLookupPPx(this, VehicleModelYear.P_VinLookups);
        return ppx;
    }

    public String id() {
        return pp + "." + VehicleModelYear.P_Id;
    }

    public String created() {
        return pp + "." + VehicleModelYear.P_Created;
    }

    @Override
    public String toString() {
        return pp;
    }
    public String pp() {
        return pp;
    }
}
 
