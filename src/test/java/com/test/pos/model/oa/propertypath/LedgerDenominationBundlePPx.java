package com.test.pos.model.oa.propertypath;
 
import java.io.Serializable;
import com.test.pos.model.oa.*;
 
public class LedgerDenominationBundlePPx implements PPxInterface, Serializable {
    private static final long serialVersionUID = 1L;
    public final String pp;  // propertyPath
     
    public LedgerDenominationBundlePPx(String name) {
        this(null, name);
    }

    public LedgerDenominationBundlePPx(PPxInterface parent, String name) {
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

    public StorePPx calcStore() {
        StorePPx ppx = new StorePPx(this, LedgerDenominationBundle.P_CalcStore);
        return ppx;
    }

    public DenominationBundlePPx denominationBundle() {
        DenominationBundlePPx ppx = new DenominationBundlePPx(this, LedgerDenominationBundle.P_DenominationBundle);
        return ppx;
    }

    public StoreSafeLedgerEntryPPx storeSafeLedgerEntry() {
        StoreSafeLedgerEntryPPx ppx = new StoreSafeLedgerEntryPPx(this, LedgerDenominationBundle.P_StoreSafeLedgerEntry);
        return ppx;
    }

    public TillLedgerEntryPPx tillLedgerEntry() {
        TillLedgerEntryPPx ppx = new TillLedgerEntryPPx(this, LedgerDenominationBundle.P_TillLedgerEntry);
        return ppx;
    }

    public String id() {
        return pp + "." + LedgerDenominationBundle.P_Id;
    }

    public String created() {
        return pp + "." + LedgerDenominationBundle.P_Created;
    }

    public String quantity() {
        return pp + "." + LedgerDenominationBundle.P_Quantity;
    }

    public String totalAmount() {
        return pp + "." + LedgerDenominationBundle.P_TotalAmount;
    }

    public String posted() {
        return pp + "." + LedgerDenominationBundle.P_Posted;
    }

    public String calcEnabled() {
        return pp + "." + LedgerDenominationBundle.P_CalcEnabled;
    }

    @Override
    public String toString() {
        return pp;
    }
    public String pp() {
        return pp;
    }
}
 
