package com.test.pos.model.oa.propertypath;
 
import java.io.Serializable;
import com.test.pos.model.oa.*;
 
public class DenominationBundlePPx implements PPxInterface, Serializable {
    private static final long serialVersionUID = 1L;
    public final String pp;  // propertyPath
     
    public DenominationBundlePPx(String name) {
        this(null, name);
    }

    public DenominationBundlePPx(PPxInterface parent, String name) {
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

    public CurrencyDenominationPPx currencyDenomination() {
        CurrencyDenominationPPx ppx = new CurrencyDenominationPPx(this, DenominationBundle.P_CurrencyDenomination);
        return ppx;
    }

    public LedgerDenominationBundlePPx ledgerDenominationBundles() {
        LedgerDenominationBundlePPx ppx = new LedgerDenominationBundlePPx(this, DenominationBundle.P_LedgerDenominationBundles);
        return ppx;
    }

    public String id() {
        return pp + "." + DenominationBundle.P_Id;
    }

    public String created() {
        return pp + "." + DenominationBundle.P_Created;
    }

    public String name() {
        return pp + "." + DenominationBundle.P_Name;
    }

    public String type() {
        return pp + "." + DenominationBundle.P_Type;
    }

    public String packSize() {
        return pp + "." + DenominationBundle.P_PackSize;
    }

    @Override
    public String toString() {
        return pp;
    }
    public String pp() {
        return pp;
    }
}
 
