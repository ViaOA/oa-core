package com.test.pos.model.oa.propertypath;
 
import com.test.pos.model.oa.*;
 
public class CurrencyTypePP {
    private static CurrencyDenominationPPx currencyDenominations;
    private static CurrencyExchangeRatePPx currencyExchangeRates;
    private static StorePPx stores;
    private static CurrencyExchangeRatePPx toCurrencyExchangeRates;
     

    public static CurrencyDenominationPPx currencyDenominations() {
        if (currencyDenominations == null) currencyDenominations = new CurrencyDenominationPPx(CurrencyType.P_CurrencyDenominations);
        return currencyDenominations;
    }

    public static CurrencyExchangeRatePPx currencyExchangeRates() {
        if (currencyExchangeRates == null) currencyExchangeRates = new CurrencyExchangeRatePPx(CurrencyType.P_CurrencyExchangeRates);
        return currencyExchangeRates;
    }

    public static StorePPx stores() {
        if (stores == null) stores = new StorePPx(CurrencyType.P_Stores);
        return stores;
    }

    public static CurrencyExchangeRatePPx toCurrencyExchangeRates() {
        if (toCurrencyExchangeRates == null) toCurrencyExchangeRates = new CurrencyExchangeRatePPx(CurrencyType.P_ToCurrencyExchangeRates);
        return toCurrencyExchangeRates;
    }

    public static String id() {
        String s = CurrencyType.P_Id;
        return s;
    }

    public static String created() {
        String s = CurrencyType.P_Created;
        return s;
    }

    public static String code() {
        String s = CurrencyType.P_Code;
        return s;
    }

    public static String name() {
        String s = CurrencyType.P_Name;
        return s;
    }

    public static String description() {
        String s = CurrencyType.P_Description;
        return s;
    }

    public static String symbol() {
        String s = CurrencyType.P_Symbol;
        return s;
    }

    public static String javaFormatCode() {
        String s = CurrencyType.P_JavaFormatCode;
        return s;
    }

    public static String minorUnit() {
        String s = CurrencyType.P_MinorUnit;
        return s;
    }

    public static String roundingRule() {
        String s = CurrencyType.P_RoundingRule;
        return s;
    }

    public static String pp() {
        return ""; // this
    }
}
 
