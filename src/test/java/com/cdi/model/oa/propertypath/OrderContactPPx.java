// Generated by OABuilder
package com.cdi.model.oa.propertypath;
 
import java.io.Serializable;
import com.cdi.model.oa.*;
 
public class OrderContactPPx implements PPxInterface, Serializable {
    private static final long serialVersionUID = 1L;
    public final String pp;  // propertyPath
     
    public OrderContactPPx(String name) {
        this(null, name);
    }

    public OrderContactPPx(PPxInterface parent, String name) {
        String s = null;
        if (parent != null) {
            s = parent.toString();
        }
        if (s == null) s = "";
        if (name != null && name.length() > 0) {
            if (s.length() > 0 && name.charAt(0) != ':') s += ".";
            s += name;
        }
        pp = s;
    }

    public ContactPPx contact() {
        ContactPPx ppx = new ContactPPx(this, OrderContact.P_Contact);
        return ppx;
    }

    public OrderPPx order() {
        OrderPPx ppx = new OrderPPx(this, OrderContact.P_Order);
        return ppx;
    }

    public String id() {
        return pp + "." + OrderContact.P_Id;
    }

    public String date() {
        return pp + "." + OrderContact.P_Date;
    }

    public String time() {
        return pp + "." + OrderContact.P_Time;
    }

    public String notes() {
        return pp + "." + OrderContact.P_Notes;
    }

    public String followup() {
        return pp + "." + OrderContact.P_Followup;
    }

    public String followupDate() {
        return pp + "." + OrderContact.P_FollowupDate;
    }

    public String followupTime() {
        return pp + "." + OrderContact.P_FollowupTime;
    }

    public String followupNote() {
        return pp + "." + OrderContact.P_FollowupNote;
    }

    public String followupCompleted() {
        return pp + "." + OrderContact.P_FollowupCompleted;
    }

    @Override
    public String toString() {
        return pp;
    }
    public String pp() {
        return pp;
    }
}
 
