package com.test.pos.model.oa.propertypath;
 
import com.test.pos.model.oa.*;
 
public class DeliveryServicePP {
    private static OnlineOrderDeliveryPPx onlineOrderDeliveries;
    private static StsDeliveryPPx stsDeliveries;
     

    public static OnlineOrderDeliveryPPx onlineOrderDeliveries() {
        if (onlineOrderDeliveries == null) onlineOrderDeliveries = new OnlineOrderDeliveryPPx(DeliveryService.P_OnlineOrderDeliveries);
        return onlineOrderDeliveries;
    }

    public static StsDeliveryPPx stsDeliveries() {
        if (stsDeliveries == null) stsDeliveries = new StsDeliveryPPx(DeliveryService.P_StsDeliveries);
        return stsDeliveries;
    }

    public static String id() {
        String s = DeliveryService.P_Id;
        return s;
    }

    public static String created() {
        String s = DeliveryService.P_Created;
        return s;
    }

    public static String name() {
        String s = DeliveryService.P_Name;
        return s;
    }

    public static String pp() {
        return ""; // this
    }
}
 
