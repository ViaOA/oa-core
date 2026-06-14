package com.test.pos.model.oa.propertypath;
 
import com.test.pos.model.oa.*;
 
public class GarageVehiclePP {
    private static GaragePPx garage;
    private static VehicleModelPPx vehicleModel;
    private static VehicleModelPackagePPx vehicleModelPackage;
     

    public static GaragePPx garage() {
        if (garage == null) garage = new GaragePPx(GarageVehicle.P_Garage);
        return garage;
    }

    public static VehicleModelPPx vehicleModel() {
        if (vehicleModel == null) vehicleModel = new VehicleModelPPx(GarageVehicle.P_VehicleModel);
        return vehicleModel;
    }

    public static VehicleModelPackagePPx vehicleModelPackage() {
        if (vehicleModelPackage == null) vehicleModelPackage = new VehicleModelPackagePPx(GarageVehicle.P_VehicleModelPackage);
        return vehicleModelPackage;
    }

    public static String id() {
        String s = GarageVehicle.P_Id;
        return s;
    }

    public static String created() {
        String s = GarageVehicle.P_Created;
        return s;
    }

    public static String pp() {
        return ""; // this
    }
}
 
