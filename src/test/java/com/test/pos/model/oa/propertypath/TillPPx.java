package com.test.pos.model.oa.propertypath;
 
import java.io.Serializable;
import com.test.pos.model.oa.*;
 
public class TillPPx implements PPxInterface, Serializable {
    private static final long serialVersionUID = 1L;
    public final String pp;  // propertyPath
     
    public TillPPx(String name) {
        this(null, name);
    }

    public TillPPx(PPxInterface parent, String name) {
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

    public InvoicePaymentCheckPPx invoicePaymentChecks() {
        InvoicePaymentCheckPPx ppx = new InvoicePaymentCheckPPx(this, Till.P_InvoicePaymentChecks);
        return ppx;
    }

    public RegisterPPx register() {
        RegisterPPx ppx = new RegisterPPx(this, Till.P_Register);
        return ppx;
    }

    public StorePPx store() {
        StorePPx ppx = new StorePPx(this, Till.P_Store);
        return ppx;
    }

    public TillLedgerEntryPPx tillLedgerEntries() {
        TillLedgerEntryPPx ppx = new TillLedgerEntryPPx(this, Till.P_TillLedgerEntries);
        return ppx;
    }

    public String id() {
        return pp + "." + Till.P_Id;
    }

    public String created() {
        return pp + "." + Till.P_Created;
    }

    public String code() {
        return pp + "." + Till.P_Code;
    }

    public String cashAmount() {
        return pp + "." + Till.P_CashAmount;
    }

    public String totalCheckAmount() {
        return pp + "." + Till.P_TotalCheckAmount;
    }

    public String moveCashToSafe() {
        return pp + ".moveCashToSafe";
    }

    @Override
    public String toString() {
        return pp;
    }
    public String pp() {
        return pp;
    }
}
 
