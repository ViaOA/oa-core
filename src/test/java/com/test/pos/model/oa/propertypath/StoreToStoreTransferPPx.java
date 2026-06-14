package com.test.pos.model.oa.propertypath;
 
import java.io.Serializable;
import com.test.pos.model.oa.*;
 
public class StoreToStoreTransferPPx implements PPxInterface, Serializable {
    private static final long serialVersionUID = 1L;
    public final String pp;  // propertyPath
     
    public StoreToStoreTransferPPx(String name) {
        this(null, name);
    }

    public StoreToStoreTransferPPx(PPxInterface parent, String name) {
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

    public StsDeliveryPPx stsDeliveries() {
        StsDeliveryPPx ppx = new StsDeliveryPPx(this, StoreToStoreTransfer.P_StsDeliveries);
        return ppx;
    }

    public StsItemPPx stsItems() {
        StsItemPPx ppx = new StsItemPPx(this, StoreToStoreTransfer.P_StsItems);
        return ppx;
    }

    public StorePPx toStore() {
        StorePPx ppx = new StorePPx(this, StoreToStoreTransfer.P_ToStore);
        return ppx;
    }

    public String id() {
        return pp + "." + StoreToStoreTransfer.P_Id;
    }

    public String created() {
        return pp + "." + StoreToStoreTransfer.P_Created;
    }

    @Override
    public String toString() {
        return pp;
    }
    public String pp() {
        return pp;
    }
}
 
