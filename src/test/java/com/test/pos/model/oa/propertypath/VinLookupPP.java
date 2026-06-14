package com.test.pos.model.oa.propertypath;
 
import com.test.pos.model.oa.*;
 
public class VinLookupPP {
    private static VehicleModelPackagePPx vehicleModelPackages;
    private static VehicleModelYearPPx vehicleModelYear;
     

    public static VehicleModelPackagePPx vehicleModelPackages() {
        if (vehicleModelPackages == null) vehicleModelPackages = new VehicleModelPackagePPx(VinLookup.P_VehicleModelPackages);
        return vehicleModelPackages;
    }

    public static VehicleModelYearPPx vehicleModelYear() {
        if (vehicleModelYear == null) vehicleModelYear = new VehicleModelYearPPx(VinLookup.P_VehicleModelYear);
        return vehicleModelYear;
    }

    public static String id() {
        String s = VinLookup.P_Id;
        return s;
    }

    public static String created() {
        String s = VinLookup.P_Created;
        return s;
    }

    public static String vin() {
        String s = VinLookup.P_Vin;
        return s;
    }

    public static String pp() {
        return ""; // this
    }
}
 
