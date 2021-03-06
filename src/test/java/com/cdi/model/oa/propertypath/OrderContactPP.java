// Generated by OABuilder
package com.cdi.model.oa.propertypath;
 
import com.cdi.model.oa.*;
 
public class OrderContactPP {
    private static ContactPPx contact;
    private static OrderPPx order;
     

    public static ContactPPx contact() {
        if (contact == null) contact = new ContactPPx(OrderContact.P_Contact);
        return contact;
    }

    public static OrderPPx order() {
        if (order == null) order = new OrderPPx(OrderContact.P_Order);
        return order;
    }

    public static String id() {
        String s = OrderContact.P_Id;
        return s;
    }

    public static String date() {
        String s = OrderContact.P_Date;
        return s;
    }

    public static String time() {
        String s = OrderContact.P_Time;
        return s;
    }

    public static String notes() {
        String s = OrderContact.P_Notes;
        return s;
    }

    public static String followup() {
        String s = OrderContact.P_Followup;
        return s;
    }

    public static String followupDate() {
        String s = OrderContact.P_FollowupDate;
        return s;
    }

    public static String followupTime() {
        String s = OrderContact.P_FollowupTime;
        return s;
    }

    public static String followupNote() {
        String s = OrderContact.P_FollowupNote;
        return s;
    }

    public static String followupCompleted() {
        String s = OrderContact.P_FollowupCompleted;
        return s;
    }
}
 
