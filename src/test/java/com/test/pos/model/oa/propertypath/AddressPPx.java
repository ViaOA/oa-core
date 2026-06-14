package com.test.pos.model.oa.propertypath;
 
import java.io.Serializable;
import com.test.pos.model.oa.*;
 
public class AddressPPx implements PPxInterface, Serializable {
    private static final long serialVersionUID = 1L;
    public final String pp;  // propertyPath
     
    public AddressPPx(String name) {
        this(null, name);
    }

    public AddressPPx(PPxInterface parent, String name) {
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
        CustomerPPx ppx = new CustomerPPx(this, Address.P_Customer);
        return ppx;
    }

    public InvoiceShipToPPx invoiceShipTos() {
        InvoiceShipToPPx ppx = new InvoiceShipToPPx(this, Address.P_InvoiceShipTos);
        return ppx;
    }

    public StorePPx store() {
        StorePPx ppx = new StorePPx(this, Address.P_Store);
        return ppx;
    }

    public String id() {
        return pp + "." + Address.P_Id;
    }

    public String created() {
        return pp + "." + Address.P_Created;
    }

    public String name() {
        return pp + "." + Address.P_Name;
    }

    public String address1() {
        return pp + "." + Address.P_Address1;
    }

    public String address2() {
        return pp + "." + Address.P_Address2;
    }

    public String city() {
        return pp + "." + Address.P_City;
    }

    public String state() {
        return pp + "." + Address.P_State;
    }

    public String zip() {
        return pp + "." + Address.P_Zip;
    }

    public String zip4() {
        return pp + "." + Address.P_Zip4;
    }

    public String type() {
        return pp + "." + Address.P_Type;
    }

    public String gis() {
        return pp + "." + Address.P_GIS;
    }

    public String timezone() {
        return pp + "." + Address.P_Timezone;
    }

    public String calcCityStateZip() {
        return pp + "." + Address.P_CalcCityStateZip;
    }

    @Override
    public String toString() {
        return pp;
    }
    public String pp() {
        return pp;
    }
}
 
