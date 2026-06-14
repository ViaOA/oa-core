package com.test.pos.model.oa.propertypath;
 
import com.test.pos.model.oa.*;
 
public class GaragePP {
    private static CustomerPPx customer;
    private static GarageVehiclePPx garageVehicles;
     

    public static CustomerPPx customer() {
        if (customer == null) customer = new CustomerPPx(Garage.P_Customer);
        return customer;
    }

    public static GarageVehiclePPx garageVehicles() {
        if (garageVehicles == null) garageVehicles = new GarageVehiclePPx(Garage.P_GarageVehicles);
        return garageVehicles;
    }

    public static String id() {
        String s = Garage.P_Id;
        return s;
    }

    public static String created() {
        String s = Garage.P_Created;
        return s;
    }

    public static String pp() {
        return ""; // this
    }
}
 
