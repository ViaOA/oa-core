package com.test.pos.model.oa.propertypath;
 
import java.io.Serializable;
import com.test.pos.model.oa.*;
 
public class VinLookupPPx implements PPxInterface, Serializable {
    private static final long serialVersionUID = 1L;
    public final String pp;  // propertyPath
     
    public VinLookupPPx(String name) {
        this(null, name);
    }

    public VinLookupPPx(PPxInterface parent, String name) {
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

    public VehicleModelPackagePPx vehicleModelPackages() {
        VehicleModelPackagePPx ppx = new VehicleModelPackagePPx(this, VinLookup.P_VehicleModelPackages);
        return ppx;
    }

    public VehicleModelYearPPx vehicleModelYear() {
        VehicleModelYearPPx ppx = new VehicleModelYearPPx(this, VinLookup.P_VehicleModelYear);
        return ppx;
    }

    public String id() {
        return pp + "." + VinLookup.P_Id;
    }

    public String created() {
        return pp + "." + VinLookup.P_Created;
    }

    public String vin() {
        return pp + "." + VinLookup.P_Vin;
    }

    @Override
    public String toString() {
        return pp;
    }
    public String pp() {
        return pp;
    }
}
 
