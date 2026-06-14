package com.test.pos.model.oa.propertypath;
 
import com.test.pos.model.oa.*;
 
public class DenominationBundlePP {
    private static CurrencyDenominationPPx currencyDenomination;
    private static LedgerDenominationBundlePPx ledgerDenominationBundles;
     

    public static CurrencyDenominationPPx currencyDenomination() {
        if (currencyDenomination == null) currencyDenomination = new CurrencyDenominationPPx(DenominationBundle.P_CurrencyDenomination);
        return currencyDenomination;
    }

    public static LedgerDenominationBundlePPx ledgerDenominationBundles() {
        if (ledgerDenominationBundles == null) ledgerDenominationBundles = new LedgerDenominationBundlePPx(DenominationBundle.P_LedgerDenominationBundles);
        return ledgerDenominationBundles;
    }

    public static String id() {
        String s = DenominationBundle.P_Id;
        return s;
    }

    public static String created() {
        String s = DenominationBundle.P_Created;
        return s;
    }

    public static String name() {
        String s = DenominationBundle.P_Name;
        return s;
    }

    public static String type() {
        String s = DenominationBundle.P_Type;
        return s;
    }

    public static String packSize() {
        String s = DenominationBundle.P_PackSize;
        return s;
    }

    public static String pp() {
        return ""; // this
    }
}
 
