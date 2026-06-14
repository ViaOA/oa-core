package com.test.pos.model.oa.propertypath;
 
import com.test.pos.model.oa.*;
 
public class VehicleModelPackagePP {
    private static CatalogItemPPx catalogItems;
    private static GarageVehiclePPx garageVehicles;
    private static VehicleModelPPx vehicleModel;
    private static VehicleModelYearPPx vehicleModelYear;
    private static VinLookupPPx vinLookup;
     

    public static CatalogItemPPx catalogItems() {
        if (catalogItems == null) catalogItems = new CatalogItemPPx(VehicleModelPackage.P_CatalogItems);
        return catalogItems;
    }

    public static GarageVehiclePPx garageVehicles() {
        if (garageVehicles == null) garageVehicles = new GarageVehiclePPx(VehicleModelPackage.P_GarageVehicles);
        return garageVehicles;
    }

    public static VehicleModelPPx vehicleModel() {
        if (vehicleModel == null) vehicleModel = new VehicleModelPPx(VehicleModelPackage.P_VehicleModel);
        return vehicleModel;
    }

    public static VehicleModelYearPPx vehicleModelYear() {
        if (vehicleModelYear == null) vehicleModelYear = new VehicleModelYearPPx(VehicleModelPackage.P_VehicleModelYear);
        return vehicleModelYear;
    }

    public static VinLookupPPx vinLookup() {
        if (vinLookup == null) vinLookup = new VinLookupPPx(VehicleModelPackage.P_VinLookup);
        return vinLookup;
    }

    public static String id() {
        String s = VehicleModelPackage.P_Id;
        return s;
    }

    public static String created() {
        String s = VehicleModelPackage.P_Created;
        return s;
    }

    public static String pp() {
        return ""; // this
    }
}
 
