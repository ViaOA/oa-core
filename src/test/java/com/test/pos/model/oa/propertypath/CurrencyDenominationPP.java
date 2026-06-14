package com.test.pos.model.oa.propertypath;
 
import com.test.pos.model.oa.*;
 
public class CurrencyDenominationPP {
    private static CurrencyTypePPx currencyType;
    private static DenominationBundlePPx denominationBundles;
     

    public static CurrencyTypePPx currencyType() {
        if (currencyType == null) currencyType = new CurrencyTypePPx(CurrencyDenomination.P_CurrencyType);
        return currencyType;
    }

    public static DenominationBundlePPx denominationBundles() {
        if (denominationBundles == null) denominationBundles = new DenominationBundlePPx(CurrencyDenomination.P_DenominationBundles);
        return denominationBundles;
    }

    public static String id() {
        String s = CurrencyDenomination.P_Id;
        return s;
    }

    public static String created() {
        String s = CurrencyDenomination.P_Created;
        return s;
    }

    public static String type() {
        String s = CurrencyDenomination.P_Type;
        return s;
    }

    public static String name() {
        String s = CurrencyDenomination.P_Name;
        return s;
    }

    public static String unitValue() {
        String s = CurrencyDenomination.P_UnitValue;
        return s;
    }

    public static String pp() {
        return ""; // this
    }
}
 
