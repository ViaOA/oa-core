// Generated by OABuilder
package com.cdi.model.oa.propertypath;
 
import com.cdi.model.oa.*;
 
public class OrderItemCommentPP {
    private static OrderItemPPx orderItem;
     

    public static OrderItemPPx orderItem() {
        if (orderItem == null) orderItem = new OrderItemPPx(OrderItemComment.P_OrderItem);
        return orderItem;
    }

    public static String id() {
        String s = OrderItemComment.P_Id;
        return s;
    }

    public static String description() {
        String s = OrderItemComment.P_Description;
        return s;
    }
}
 
