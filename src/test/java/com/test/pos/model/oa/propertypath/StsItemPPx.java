package com.test.pos.model.oa.propertypath;
 
import java.io.Serializable;
import com.test.pos.model.oa.*;
 
public class StsItemPPx implements PPxInterface, Serializable {
    private static final long serialVersionUID = 1L;
    public final String pp;  // propertyPath
     
    public StsItemPPx(String name) {
        this(null, name);
    }

    public StsItemPPx(PPxInterface parent, String name) {
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
        ItemPPx ppx = new ItemPPx(this, StsItem.P_Item);
        return ppx;
    }

    public StoreToStoreTransferPPx storeToStoreTransfer() {
        StoreToStoreTransferPPx ppx = new StoreToStoreTransferPPx(this, StsItem.P_StoreToStoreTransfer);
        return ppx;
    }

    public StsdItemPPx stsdItems() {
        StsdItemPPx ppx = new StsdItemPPx(this, StsItem.P_StsdItems);
        return ppx;
    }

    public String id() {
        return pp + "." + StsItem.P_Id;
    }

    public String created() {
        return pp + "." + StsItem.P_Created;
    }

    public String quantity() {
        return pp + "." + StsItem.P_Quantity;
    }

    @Override
    public String toString() {
        return pp;
    }
    public String pp() {
        return pp;
    }
}
 
