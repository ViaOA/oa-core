package com.test.pos.model.oa.propertypath;
 
import java.io.Serializable;
import com.test.pos.model.oa.*;
 
public class DeliveryServicePPx implements PPxInterface, Serializable {
    private static final long serialVersionUID = 1L;
    public final String pp;  // propertyPath
     
    public DeliveryServicePPx(String name) {
        this(null, name);
    }

    public DeliveryServicePPx(PPxInterface parent, String name) {
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

    public OnlineOrderDeliveryPPx onlineOrderDeliveries() {
        OnlineOrderDeliveryPPx ppx = new OnlineOrderDeliveryPPx(this, DeliveryService.P_OnlineOrderDeliveries);
        return ppx;
    }

    public StsDeliveryPPx stsDeliveries() {
        StsDeliveryPPx ppx = new StsDeliveryPPx(this, DeliveryService.P_StsDeliveries);
        return ppx;
    }

    public String id() {
        return pp + "." + DeliveryService.P_Id;
    }

    public String created() {
        return pp + "." + DeliveryService.P_Created;
    }

    public String name() {
        return pp + "." + DeliveryService.P_Name;
    }

    @Override
    public String toString() {
        return pp;
    }
    public String pp() {
        return pp;
    }
}
 
