package com.test.pos.model.oa.propertypath;
 
import com.test.pos.model.oa.*;
 
public class OnlineOrderPP {
    private static CustomerPPx customer;
    private static OnlineOrderDeliveryPPx onlineOrderDeliveries;
    private static OnlineOrderItemPPx onlineOrderItems;
     

    public static CustomerPPx customer() {
        if (customer == null) customer = new CustomerPPx(OnlineOrder.P_Customer);
        return customer;
    }

    public static OnlineOrderDeliveryPPx onlineOrderDeliveries() {
        if (onlineOrderDeliveries == null) onlineOrderDeliveries = new OnlineOrderDeliveryPPx(OnlineOrder.P_OnlineOrderDeliveries);
        return onlineOrderDeliveries;
    }

    public static OnlineOrderItemPPx onlineOrderItems() {
        if (onlineOrderItems == null) onlineOrderItems = new OnlineOrderItemPPx(OnlineOrder.P_OnlineOrderItems);
        return onlineOrderItems;
    }

    public static String id() {
        String s = OnlineOrder.P_Id;
        return s;
    }

    public static String created() {
        String s = OnlineOrder.P_Created;
        return s;
    }

    public static String pp() {
        return ""; // this
    }
}
 
