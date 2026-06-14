package com.test.pos.model.oa.propertypath;
 
import java.io.Serializable;
import com.test.pos.model.oa.*;
 
public class CurrencyExchangeRatePPx implements PPxInterface, Serializable {
    private static final long serialVersionUID = 1L;
    public final String pp;  // propertyPath
     
    public CurrencyExchangeRatePPx(String name) {
        this(null, name);
    }

    public CurrencyExchangeRatePPx(PPxInterface parent, String name) {
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
        CurrencyTypePPx ppx = new CurrencyTypePPx(this, CurrencyExchangeRate.P_CurrencyType);
        return ppx;
    }

    public CurrencyTypePPx toCurrencyType() {
        CurrencyTypePPx ppx = new CurrencyTypePPx(this, CurrencyExchangeRate.P_ToCurrencyType);
        return ppx;
    }

    public String id() {
        return pp + "." + CurrencyExchangeRate.P_Id;
    }

    public String created() {
        return pp + "." + CurrencyExchangeRate.P_Created;
    }

    public String rate() {
        return pp + "." + CurrencyExchangeRate.P_Rate;
    }

    public String beginDate() {
        return pp + "." + CurrencyExchangeRate.P_BeginDate;
    }

    public String endDate() {
        return pp + "." + CurrencyExchangeRate.P_EndDate;
    }

    @Override
    public String toString() {
        return pp;
    }
    public String pp() {
        return pp;
    }
}
 
