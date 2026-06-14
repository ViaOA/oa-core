package com.test.pos.model.oa.propertypath;
 
import java.io.Serializable;
import com.test.pos.model.oa.*;
 
public class ReturnedCheckFeePPx implements PPxInterface, Serializable {
    private static final long serialVersionUID = 1L;
    public final String pp;  // propertyPath
     
    public ReturnedCheckFeePPx(String name) {
        this(null, name);
    }

    public ReturnedCheckFeePPx(PPxInterface parent, String name) {
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

    public InvoicePaymentCheckPPx invoicePaymentCheck() {
        InvoicePaymentCheckPPx ppx = new InvoicePaymentCheckPPx(this, ReturnedCheckFee.P_InvoicePaymentCheck);
        return ppx;
    }

    public String id() {
        return pp + "." + ReturnedCheckFee.P_Id;
    }

    public String created() {
        return pp + "." + ReturnedCheckFee.P_Created;
    }

    public String amount() {
        return pp + "." + ReturnedCheckFee.P_Amount;
    }

    public String collectedDate() {
        return pp + "." + ReturnedCheckFee.P_CollectedDate;
    }

    public String note() {
        return pp + "." + ReturnedCheckFee.P_Note;
    }

    public String status() {
        return pp + "." + ReturnedCheckFee.P_Status;
    }

    @Override
    public String toString() {
        return pp;
    }
    public String pp() {
        return pp;
    }
}
 
