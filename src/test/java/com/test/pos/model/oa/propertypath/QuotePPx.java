package com.test.pos.model.oa.propertypath;
 
import java.io.Serializable;
import com.test.pos.model.oa.*;
 
public class QuotePPx implements PPxInterface, Serializable {
    private static final long serialVersionUID = 1L;
    public final String pp;  // propertyPath
     
    public QuotePPx(String name) {
        this(null, name);
    }

    public QuotePPx(PPxInterface parent, String name) {
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
        CustomerPPx ppx = new CustomerPPx(this, Quote.P_Customer);
        return ppx;
    }

    public InvoicePPx invoice() {
        InvoicePPx ppx = new InvoicePPx(this, Quote.P_Invoice);
        return ppx;
    }

    public String id() {
        return pp + "." + Quote.P_Id;
    }

    public String created() {
        return pp + "." + Quote.P_Created;
    }

    public String name() {
        return pp + "." + Quote.P_Name;
    }

    public String note() {
        return pp + "." + Quote.P_Note;
    }

    public String endDate() {
        return pp + "." + Quote.P_EndDate;
    }

    @Override
    public String toString() {
        return pp;
    }
    public String pp() {
        return pp;
    }
}
 
