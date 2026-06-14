package com.test.pos.model.oa.propertypath;
 
import java.io.Serializable;
import com.test.pos.model.oa.*;
 
public class OnlineOrderPPx implements PPxInterface, Serializable {
    private static final long serialVersionUID = 1L;
    public final String pp;  // propertyPath
     
    public OnlineOrderPPx(String name) {
        this(null, name);
    }

    public OnlineOrderPPx(PPxInterface parent, String name) {
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

    public CustomerPPx customer() {
        CustomerPPx ppx = new CustomerPPx(this, OnlineOrder.P_Customer);
        return ppx;
    }

    public OnlineOrderDeliveryPPx onlineOrderDeliveries() {
        OnlineOrderDeliveryPPx ppx = new OnlineOrderDeliveryPPx(this, OnlineOrder.P_OnlineOrderDeliveries);
        return ppx;
    }

    public OnlineOrderItemPPx onlineOrderItems() {
        OnlineOrderItemPPx ppx = new OnlineOrderItemPPx(this, OnlineOrder.P_OnlineOrderItems);
        return ppx;
    }

    public String id() {
        return pp + "." + OnlineOrder.P_Id;
    }

    public String created() {
        return pp + "." + OnlineOrder.P_Created;
    }

    @Override
    public String toString() {
        return pp;
    }
    public String pp() {
        return pp;
    }
}
 
