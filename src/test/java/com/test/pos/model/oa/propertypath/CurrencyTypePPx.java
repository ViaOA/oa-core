package com.test.pos.model.oa.propertypath;
 
import java.io.Serializable;
import com.test.pos.model.oa.*;
 
public class CurrencyTypePPx implements PPxInterface, Serializable {
    private static final long serialVersionUID = 1L;
    public final String pp;  // propertyPath
     
    public CurrencyTypePPx(String name) {
        this(null, name);
    }

    public CurrencyTypePPx(PPxInterface parent, String name) {
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

    public CurrencyDenominationPPx currencyDenominations() {
        CurrencyDenominationPPx ppx = new CurrencyDenominationPPx(this, CurrencyType.P_CurrencyDenominations);
        return ppx;
    }

    public CurrencyExchangeRatePPx currencyExchangeRates() {
        CurrencyExchangeRatePPx ppx = new CurrencyExchangeRatePPx(this, CurrencyType.P_CurrencyExchangeRates);
        return ppx;
    }

    public StorePPx stores() {
        StorePPx ppx = new StorePPx(this, CurrencyType.P_Stores);
        return ppx;
    }

    public CurrencyExchangeRatePPx toCurrencyExchangeRates() {
        CurrencyExchangeRatePPx ppx = new CurrencyExchangeRatePPx(this, CurrencyType.P_ToCurrencyExchangeRates);
        return ppx;
    }

    public String id() {
        return pp + "." + CurrencyType.P_Id;
    }

    public String created() {
        return pp + "." + CurrencyType.P_Created;
    }

    public String code() {
        return pp + "." + CurrencyType.P_Code;
    }

    public String name() {
        return pp + "." + CurrencyType.P_Name;
    }

    public String description() {
        return pp + "." + CurrencyType.P_Description;
    }

    public String symbol() {
        return pp + "." + CurrencyType.P_Symbol;
    }

    public String javaFormatCode() {
        return pp + "." + CurrencyType.P_JavaFormatCode;
    }

    public String minorUnit() {
        return pp + "." + CurrencyType.P_MinorUnit;
    }

    public String roundingRule() {
        return pp + "." + CurrencyType.P_RoundingRule;
    }

    @Override
    public String toString() {
        return pp;
    }
    public String pp() {
        return pp;
    }
}
 
