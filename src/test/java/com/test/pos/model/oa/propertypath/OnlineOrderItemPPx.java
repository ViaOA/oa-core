package com.test.pos.model.oa.propertypath;
 
import java.io.Serializable;
import com.test.pos.model.oa.*;
 
public class OnlineOrderItemPPx implements PPxInterface, Serializable {
    private static final long serialVersionUID = 1L;
    public final String pp;  // propertyPath
     
    public OnlineOrderItemPPx(String name) {
        this(null, name);
    }

    public OnlineOrderItemPPx(PPxInterface parent, String name) {
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

    public ItemPPx item() {
        ItemPPx ppx = new ItemPPx(this, OnlineOrderItem.P_Item);
        return ppx;
    }

    public OnlineOrderPPx onlineOrder() {
        OnlineOrderPPx ppx = new OnlineOrderPPx(this, OnlineOrderItem.P_OnlineOrder);
        return ppx;
    }

    public OodItemPPx oodItems() {
        OodItemPPx ppx = new OodItemPPx(this, OnlineOrderItem.P_OodItems);
        return ppx;
    }

    public String id() {
        return pp + "." + OnlineOrderItem.P_Id;
    }

    public String created() {
        return pp + "." + OnlineOrderItem.P_Created;
    }

    public String quantity() {
        return pp + "." + OnlineOrderItem.P_Quantity;
    }

    @Override
    public String toString() {
        return pp;
    }
    public String pp() {
        return pp;
    }
}
 
