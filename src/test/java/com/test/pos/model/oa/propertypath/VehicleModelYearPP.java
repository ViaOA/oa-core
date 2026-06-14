package com.test.pos.model.oa.propertypath;
 
import com.test.pos.model.oa.*;
 
public class VehicleModelYearPP {
    private static VehicleModelPPx vehicleModel;
    private static VehicleModelPackagePPx vehicleModelPackages;
    private static VinLookupPPx vinLookups;
     

    public static VehicleModelPPx vehicleModel() {
        if (vehicleModel == null) vehicleModel = new VehicleModelPPx(VehicleModelYear.P_VehicleModel);
        return vehicleModel;
    }

    public static VehicleModelPackagePPx vehicleModelPackages() {
        if (vehicleModelPackages == null) vehicleModelPackages = new VehicleModelPackagePPx(VehicleModelYear.P_VehicleModelPackages);
        return vehicleModelPackages;
    }

    public static VinLookupPPx vinLookups() {
        if (vinLookups == null) vinLookups = new VinLookupPPx(VehicleModelYear.P_VinLookups);
        return vinLookups;
    }

    public static String id() {
        String s = VehicleModelYear.P_Id;
        return s;
    }

    public static String created() {
        String s = VehicleModelYear.P_Created;
        return s;
    }

    public static String pp() {
        return ""; // this
    }
}
 
