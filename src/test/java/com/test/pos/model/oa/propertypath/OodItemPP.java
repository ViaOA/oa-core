package com.test.pos.model.oa.propertypath;
 
import com.test.pos.model.oa.*;
 
public class OodItemPP {
    private static OnlineOrderDeliveryPPx onlineOrderDelivery;
    private static OnlineOrderItemPPx onlineOrderItem;
    private static OodItemEachPPx oodItemEaches;
     

    public static OnlineOrderDeliveryPPx onlineOrderDelivery() {
        if (onlineOrderDelivery == null) onlineOrderDelivery = new OnlineOrderDeliveryPPx(OodItem.P_OnlineOrderDelivery);
        return onlineOrderDelivery;
    }

    public static OnlineOrderItemPPx onlineOrderItem() {
        if (onlineOrderItem == null) onlineOrderItem = new OnlineOrderItemPPx(OodItem.P_OnlineOrderItem);
        return onlineOrderItem;
    }

    public static OodItemEachPPx oodItemEaches() {
        if (oodItemEaches == null) oodItemEaches = new OodItemEachPPx(OodItem.P_OodItemEaches);
        return oodItemEaches;
    }

    public static String id() {
        String s = OodItem.P_Id;
        return s;
    }

    public static String created() {
        String s = OodItem.P_Created;
        return s;
    }

    public static String quantity() {
        String s = OodItem.P_Quantity;
        return s;
    }

    public static String pp() {
        return ""; // this
    }
}
 
