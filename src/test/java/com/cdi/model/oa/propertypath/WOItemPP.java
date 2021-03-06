// Generated by OABuilder
package com.cdi.model.oa.propertypath;
 
import com.cdi.model.oa.*;
 
public class WOItemPP {
    private static OrderItemPPx orderItem;
    private static WorkOrderPPx workOrder;
     

    public static OrderItemPPx orderItem() {
        if (orderItem == null) orderItem = new OrderItemPPx(WOItem.P_OrderItem);
        return orderItem;
    }

    public static WorkOrderPPx workOrder() {
        if (workOrder == null) workOrder = new WorkOrderPPx(WOItem.P_WorkOrder);
        return workOrder;
    }

    public static String id() {
        String s = WOItem.P_Id;
        return s;
    }

    public static String quantity() {
        String s = WOItem.P_Quantity;
        return s;
    }

    public static String minDays() {
        String s = WOItem.P_MinDays;
        return s;
    }
}
 
