package com.test.pos.model.oa.propertypath;
 
import com.test.pos.model.oa.*;
 
public class VehicleModelPP {
    private static CatalogItemPPx catalogItems;
    private static GarageVehiclePPx garageVehicles;
    private static VehicleMakePPx vehicleMake;
    private static VehicleModelPackagePPx vehicleModelPackages;
    private static VehicleModelYearPPx vehicleModelYears;
     

    public static CatalogItemPPx catalogItems() {
        if (catalogItems == null) catalogItems = new CatalogItemPPx(VehicleModel.P_CatalogItems);
        return catalogItems;
    }

    public static GarageVehiclePPx garageVehicles() {
        if (garageVehicles == null) garageVehicles = new GarageVehiclePPx(VehicleModel.P_GarageVehicles);
        return garageVehicles;
    }

    public static VehicleMakePPx vehicleMake() {
        if (vehicleMake == null) vehicleMake = new VehicleMakePPx(VehicleModel.P_VehicleMake);
        return vehicleMake;
    }

    public static VehicleModelPackagePPx vehicleModelPackages() {
        if (vehicleModelPackages == null) vehicleModelPackages = new VehicleModelPackagePPx(VehicleModel.P_VehicleModelPackages);
        return vehicleModelPackages;
    }

    public static VehicleModelYearPPx vehicleModelYears() {
        if (vehicleModelYears == null) vehicleModelYears = new VehicleModelYearPPx(VehicleModel.P_VehicleModelYears);
        return vehicleModelYears;
    }

    public static String id() {
        String s = VehicleModel.P_Id;
        return s;
    }

    public static String created() {
        String s = VehicleModel.P_Created;
        return s;
    }

    public static String pp() {
        return ""; // this
    }
}
 
