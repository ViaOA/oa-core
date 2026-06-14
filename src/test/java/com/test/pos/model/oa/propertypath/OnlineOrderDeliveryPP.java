package com.test.pos.model.oa.propertypath;
 
import com.test.pos.model.oa.*;
 
public class OnlineOrderDeliveryPP {
    private static DeliveryServicePPx deliveryService;
    private static OnlineOrderPPx onlineOrder;
    private static OodItemPPx oodItems;
     

    public static DeliveryServicePPx deliveryService() {
        if (deliveryService == null) deliveryService = new DeliveryServicePPx(OnlineOrderDelivery.P_DeliveryService);
        return deliveryService;
    }

    public static OnlineOrderPPx onlineOrder() {
        if (onlineOrder == null) onlineOrder = new OnlineOrderPPx(OnlineOrderDelivery.P_OnlineOrder);
        return onlineOrder;
    }

    public static OodItemPPx oodItems() {
        if (oodItems == null) oodItems = new OodItemPPx(OnlineOrderDelivery.P_OodItems);
        return oodItems;
    }

    public static String id() {
        String s = OnlineOrderDelivery.P_Id;
        return s;
    }

    public static String created() {
        String s = OnlineOrderDelivery.P_Created;
        return s;
    }

    public static String pp() {
        return ""; // this
    }
}
 
