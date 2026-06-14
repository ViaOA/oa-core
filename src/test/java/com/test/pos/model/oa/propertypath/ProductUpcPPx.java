package com.test.pos.model.oa.propertypath;
 
import java.io.Serializable;
import com.test.pos.model.oa.*;
 
public class ProductUpcPPx implements PPxInterface, Serializable {
    private static final long serialVersionUID = 1L;
    public final String pp;  // propertyPath
     
    public ProductUpcPPx(String name) {
        this(null, name);
    }

    public ProductUpcPPx(PPxInterface parent, String name) {
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

    public BarcodeTypePPx barcodeType() {
        BarcodeTypePPx ppx = new BarcodeTypePPx(this, ProductUpc.P_BarcodeType);
        return ppx;
    }

    public ProductPPx product() {
        ProductPPx ppx = new ProductPPx(this, ProductUpc.P_Product);
        return ppx;
    }

    public String id() {
        return pp + "." + ProductUpc.P_Id;
    }

    public String created() {
        return pp + "." + ProductUpc.P_Created;
    }

    public String upc() {
        return pp + "." + ProductUpc.P_UPC;
    }

    @Override
    public String toString() {
        return pp;
    }
    public String pp() {
        return pp;
    }
}
 
