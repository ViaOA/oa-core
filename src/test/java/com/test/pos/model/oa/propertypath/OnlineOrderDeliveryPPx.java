package com.test.pos.model.oa.propertypath;
 
import java.io.Serializable;
import com.test.pos.model.oa.*;
 
public class OnlineOrderDeliveryPPx implements PPxInterface, Serializable {
    private static final long serialVersionUID = 1L;
    public final String pp;  // propertyPath
     
    public OnlineOrderDeliveryPPx(String name) {
        this(null, name);
    }

    public OnlineOrderDeliveryPPx(PPxInterface parent, String name) {
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

    public DeliveryServicePPx deliveryService() {
        DeliveryServicePPx ppx = new DeliveryServicePPx(this, OnlineOrderDelivery.P_DeliveryService);
        return ppx;
    }

    public OnlineOrderPPx onlineOrder() {
        OnlineOrderPPx ppx = new OnlineOrderPPx(this, OnlineOrderDelivery.P_OnlineOrder);
        return ppx;
    }

    public OodItemPPx oodItems() {
        OodItemPPx ppx = new OodItemPPx(this, OnlineOrderDelivery.P_OodItems);
        return ppx;
    }

    public String id() {
        return pp + "." + OnlineOrderDelivery.P_Id;
    }

    public String created() {
        return pp + "." + OnlineOrderDelivery.P_Created;
    }

    @Override
    public String toString() {
        return pp;
    }
    public String pp() {
        return pp;
    }
}
 
