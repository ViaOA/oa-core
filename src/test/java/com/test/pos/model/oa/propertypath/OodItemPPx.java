package com.test.pos.model.oa.propertypath;
 
import java.io.Serializable;
import com.test.pos.model.oa.*;
 
public class OodItemPPx implements PPxInterface, Serializable {
    private static final long serialVersionUID = 1L;
    public final String pp;  // propertyPath
     
    public OodItemPPx(String name) {
        this(null, name);
    }

    public OodItemPPx(PPxInterface parent, String name) {
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

    public OnlineOrderDeliveryPPx onlineOrderDelivery() {
        OnlineOrderDeliveryPPx ppx = new OnlineOrderDeliveryPPx(this, OodItem.P_OnlineOrderDelivery);
        return ppx;
    }

    public OnlineOrderItemPPx onlineOrderItem() {
        OnlineOrderItemPPx ppx = new OnlineOrderItemPPx(this, OodItem.P_OnlineOrderItem);
        return ppx;
    }

    public OodItemEachPPx oodItemEaches() {
        OodItemEachPPx ppx = new OodItemEachPPx(this, OodItem.P_OodItemEaches);
        return ppx;
    }

    public String id() {
        return pp + "." + OodItem.P_Id;
    }

    public String created() {
        return pp + "." + OodItem.P_Created;
    }

    public String quantity() {
        return pp + "." + OodItem.P_Quantity;
    }

    @Override
    public String toString() {
        return pp;
    }
    public String pp() {
        return pp;
    }
}
 
