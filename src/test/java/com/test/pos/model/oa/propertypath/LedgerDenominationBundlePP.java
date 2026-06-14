package com.test.pos.model.oa.propertypath;
 
import com.test.pos.model.oa.*;
 
public class LedgerDenominationBundlePP {
    private static StorePPx calcStore;
    private static DenominationBundlePPx denominationBundle;
    private static StoreSafeLedgerEntryPPx storeSafeLedgerEntry;
    private static TillLedgerEntryPPx tillLedgerEntry;
     

    public static StorePPx calcStore() {
        if (calcStore == null) calcStore = new StorePPx(LedgerDenominationBundle.P_CalcStore);
        return calcStore;
    }

    public static DenominationBundlePPx denominationBundle() {
        if (denominationBundle == null) denominationBundle = new DenominationBundlePPx(LedgerDenominationBundle.P_DenominationBundle);
        return denominationBundle;
    }

    public static StoreSafeLedgerEntryPPx storeSafeLedgerEntry() {
        if (storeSafeLedgerEntry == null) storeSafeLedgerEntry = new StoreSafeLedgerEntryPPx(LedgerDenominationBundle.P_StoreSafeLedgerEntry);
        return storeSafeLedgerEntry;
    }

    public static TillLedgerEntryPPx tillLedgerEntry() {
        if (tillLedgerEntry == null) tillLedgerEntry = new TillLedgerEntryPPx(LedgerDenominationBundle.P_TillLedgerEntry);
        return tillLedgerEntry;
    }

    public static String id() {
        String s = LedgerDenominationBundle.P_Id;
        return s;
    }

    public static String created() {
        String s = LedgerDenominationBundle.P_Created;
        return s;
    }

    public static String quantity() {
        String s = LedgerDenominationBundle.P_Quantity;
        return s;
    }

    public static String totalAmount() {
        String s = LedgerDenominationBundle.P_TotalAmount;
        return s;
    }

    public static String posted() {
        String s = LedgerDenominationBundle.P_Posted;
        return s;
    }

    public static String calcEnabled() {
        String s = LedgerDenominationBundle.P_CalcEnabled;
        return s;
    }

    public static String pp() {
        return ""; // this
    }
}
 
