package com.test.pos.model.oa.propertypath;
 
import com.test.pos.model.oa.*;
 
public class VehicleMakePP {
    private static VehicleModelPPx vehicleModels;
     

    public static VehicleModelPPx vehicleModels() {
        if (vehicleModels == null) vehicleModels = new VehicleModelPPx(VehicleMake.P_VehicleModels);
        return vehicleModels;
    }

    public static String id() {
        String s = VehicleMake.P_Id;
        return s;
    }

    public static String created() {
        String s = VehicleMake.P_Created;
        return s;
    }

    public static String pp() {
        return ""; // this
    }
}
 
