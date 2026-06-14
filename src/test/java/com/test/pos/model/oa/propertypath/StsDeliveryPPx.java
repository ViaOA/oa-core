package com.test.pos.model.oa.propertypath;
 
import java.io.Serializable;
import com.test.pos.model.oa.*;
 
public class StsDeliveryPPx implements PPxInterface, Serializable {
    private static final long serialVersionUID = 1L;
    public final String pp;  // propertyPath
     
    public StsDeliveryPPx(String name) {
        this(null, name);
    }

    public StsDeliveryPPx(PPxInterface parent, String name) {
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
        DeliveryServicePPx ppx = new DeliveryServicePPx(this, StsDelivery.P_DeliveryService);
        return ppx;
    }

    public StoreToStoreTransferPPx storeToStoreTransfer() {
        StoreToStoreTransferPPx ppx = new StoreToStoreTransferPPx(this, StsDelivery.P_StoreToStoreTransfer);
        return ppx;
    }

    public StsdItemPPx stsdItems() {
        StsdItemPPx ppx = new StsdItemPPx(this, StsDelivery.P_StsdItems);
        return ppx;
    }

    public String id() {
        return pp + "." + StsDelivery.P_Id;
    }

    public String created() {
        return pp + "." + StsDelivery.P_Created;
    }

    @Override
    public String toString() {
        return pp;
    }
    public String pp() {
        return pp;
    }
}
 
