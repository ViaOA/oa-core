package com.test.pos.model.oa.propertypath;
 
import java.io.Serializable;
import com.test.pos.model.oa.*;
 
public class BarcodeTypePPx implements PPxInterface, Serializable {
    private static final long serialVersionUID = 1L;
    public final String pp;  // propertyPath
     
    public BarcodeTypePPx(String name) {
        this(null, name);
    }

    public BarcodeTypePPx(PPxInterface parent, String name) {
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

    public ProductUpcPPx productUpcs() {
        ProductUpcPPx ppx = new ProductUpcPPx(this, BarcodeType.P_ProductUpcs);
        return ppx;
    }

    public String id() {
        return pp + "." + BarcodeType.P_Id;
    }

    public String created() {
        return pp + "." + BarcodeType.P_Created;
    }

    public String name() {
        return pp + "." + BarcodeType.P_Name;
    }

    public String type() {
        return pp + "." + BarcodeType.P_Type;
    }

    public String rule() {
        return pp + "." + BarcodeType.P_Rule;
    }

    public String convertUpc() {
        return pp + ".convertUpc";
    }

    @Override
    public String toString() {
        return pp;
    }
    public String pp() {
        return pp;
    }
}
 
