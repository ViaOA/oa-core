package com.test.pos.model.oa.propertypath;
 
import java.io.Serializable;
import com.test.pos.model.oa.*;
 
public class ProductSerialCodePPx implements PPxInterface, Serializable {
    private static final long serialVersionUID = 1L;
    public final String pp;  // propertyPath
     
    public ProductSerialCodePPx(String name) {
        this(null, name);
    }

    public ProductSerialCodePPx(PPxInterface parent, String name) {
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

    public ProductPPx product() {
        ProductPPx ppx = new ProductPPx(this, ProductSerialCode.P_Product);
        return ppx;
    }

    public String id() {
        return pp + "." + ProductSerialCode.P_Id;
    }

    public String created() {
        return pp + "." + ProductSerialCode.P_Created;
    }

    public String receivedDate() {
        return pp + "." + ProductSerialCode.P_ReceivedDate;
    }

    public String serialCode() {
        return pp + "." + ProductSerialCode.P_SerialCode;
    }

    public String soldDate() {
        return pp + "." + ProductSerialCode.P_SoldDate;
    }

    @Override
    public String toString() {
        return pp;
    }
    public String pp() {
        return pp;
    }
}
 
