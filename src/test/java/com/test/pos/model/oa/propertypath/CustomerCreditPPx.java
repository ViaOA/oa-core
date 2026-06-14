package com.test.pos.model.oa.propertypath;
 
import java.io.Serializable;
import com.test.pos.model.oa.*;
 
public class CustomerCreditPPx implements PPxInterface, Serializable {
    private static final long serialVersionUID = 1L;
    public final String pp;  // propertyPath
     
    public CustomerCreditPPx(String name) {
        this(null, name);
    }

    public CustomerCreditPPx(PPxInterface parent, String name) {
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
        CustomerPPx ppx = new CustomerPPx(this, CustomerCredit.P_Customer);
        return ppx;
    }

    public String id() {
        return pp + "." + CustomerCredit.P_Id;
    }

    public String created() {
        return pp + "." + CustomerCredit.P_Created;
    }

    public String limit() {
        return pp + "." + CustomerCredit.P_Limit;
    }

    @Override
    public String toString() {
        return pp;
    }
    public String pp() {
        return pp;
    }
}
 
