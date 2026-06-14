package com.test.pos.model.oa.propertypath;
 
import com.test.pos.model.oa.*;
 
public class CurrencyExchangeRatePP {
    private static CurrencyTypePPx currencyType;
    private static CurrencyTypePPx toCurrencyType;
     

    public static CurrencyTypePPx currencyType() {
        if (currencyType == null) currencyType = new CurrencyTypePPx(CurrencyExchangeRate.P_CurrencyType);
        return currencyType;
    }

    public static CurrencyTypePPx toCurrencyType() {
        if (toCurrencyType == null) toCurrencyType = new CurrencyTypePPx(CurrencyExchangeRate.P_ToCurrencyType);
        return toCurrencyType;
    }

    public static String id() {
        String s = CurrencyExchangeRate.P_Id;
        return s;
    }

    public static String created() {
        String s = CurrencyExchangeRate.P_Created;
        return s;
    }

    public static String rate() {
        String s = CurrencyExchangeRate.P_Rate;
        return s;
    }

    public static String beginDate() {
        String s = CurrencyExchangeRate.P_BeginDate;
        return s;
    }

    public static String endDate() {
        String s = CurrencyExchangeRate.P_EndDate;
        return s;
    }

    public static String pp() {
        return ""; // this
    }
}
 
