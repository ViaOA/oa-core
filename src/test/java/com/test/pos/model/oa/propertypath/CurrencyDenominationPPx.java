package com.test.pos.model.oa.propertypath;
 
import java.io.Serializable;
import com.test.pos.model.oa.*;
 
public class CurrencyDenominationPPx implements PPxInterface, Serializable {
    private static final long serialVersionUID = 1L;
    public final String pp;  // propertyPath
     
    public CurrencyDenominationPPx(String name) {
        this(null, name);
    }

    public CurrencyDenominationPPx(PPxInterface parent, String name) {
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

    public CurrencyTypePPx currencyType() {
        CurrencyTypePPx ppx = new CurrencyTypePPx(this, CurrencyDenomination.P_CurrencyType);
        return ppx;
    }

    public DenominationBundlePPx denominationBundles() {
        DenominationBundlePPx ppx = new DenominationBundlePPx(this, CurrencyDenomination.P_DenominationBundles);
        return ppx;
    }

    public String id() {
        return pp + "." + CurrencyDenomination.P_Id;
    }

    public String created() {
        return pp + "." + CurrencyDenomination.P_Created;
    }

    public String type() {
        return pp + "." + CurrencyDenomination.P_Type;
    }

    public String name() {
        return pp + "." + CurrencyDenomination.P_Name;
    }

    public String unitValue() {
        return pp + "." + CurrencyDenomination.P_UnitValue;
    }

    @Override
    public String toString() {
        return pp;
    }
    public String pp() {
        return pp;
    }
}
 
