package com.test.pos.model.oa.propertypath;
 
import com.test.pos.model.oa.*;
 
public class OnlineOrderItemPP {
    private static ItemPPx item;
    private static OnlineOrderPPx onlineOrder;
    private static OodItemPPx oodItems;
     

    public static ItemPPx item() {
        if (item == null) item = new ItemPPx(OnlineOrderItem.P_Item);
        return item;
    }

    public static OnlineOrderPPx onlineOrder() {
        if (onlineOrder == null) onlineOrder = new OnlineOrderPPx(OnlineOrderItem.P_OnlineOrder);
        return onlineOrder;
    }

    public static OodItemPPx oodItems() {
        if (oodItems == null) oodItems = new OodItemPPx(OnlineOrderItem.P_OodItems);
        return oodItems;
    }

    public static String id() {
        String s = OnlineOrderItem.P_Id;
        return s;
    }

    public static String created() {
        String s = OnlineOrderItem.P_Created;
        return s;
    }

    public static String quantity() {
        String s = OnlineOrderItem.P_Quantity;
        return s;
    }

    public static String pp() {
        return ""; // this
    }
}
 
