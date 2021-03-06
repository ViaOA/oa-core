// Generated by OABuilder
package com.cdi.model.oa.propertypath;
 
import java.io.Serializable;
import com.cdi.model.oa.*;
 
public class TruckPPx implements PPxInterface, Serializable {
    private static final long serialVersionUID = 1L;
    public final String pp;  // propertyPath
     
    public TruckPPx(String name) {
        this(null, name);
    }

    public TruckPPx(PPxInterface parent, String name) {
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

    public DeliveryTruckPPx deliveryTrucks() {
        DeliveryTruckPPx ppx = new DeliveryTruckPPx(this, Truck.P_DeliveryTrucks);
        return ppx;
    }

    public String id() {
        return pp + "." + Truck.P_Id;
    }

    public String carrier() {
        return pp + "." + Truck.P_Carrier;
    }

    public String contact() {
        return pp + "." + Truck.P_Contact;
    }

    public String contactPhone() {
        return pp + "." + Truck.P_ContactPhone;
    }

    public String capacity() {
        return pp + "." + Truck.P_Capacity;
    }

    @Override
    public String toString() {
        return pp;
    }
    public String pp() {
        return pp;
    }
}
 
