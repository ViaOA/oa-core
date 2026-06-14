package com.test.pos.model.oa.propertypath;
 
import com.test.pos.model.oa.*;
 
public class ManualPurchaseOrderPP {
    private static StorePPx store;
    private static StoreSafeLedgerEntryPPx storeSafeLedgerEntry;
     

    public static StorePPx store() {
        if (store == null) store = new StorePPx(ManualPurchaseOrder.P_Store);
        return store;
    }

    public static StoreSafeLedgerEntryPPx storeSafeLedgerEntry() {
        if (storeSafeLedgerEntry == null) storeSafeLedgerEntry = new StoreSafeLedgerEntryPPx(ManualPurchaseOrder.P_StoreSafeLedgerEntry);
        return storeSafeLedgerEntry;
    }

    public static String id() {
        String s = ManualPurchaseOrder.P_Id;
        return s;
    }

    public static String created() {
        String s = ManualPurchaseOrder.P_Created;
        return s;
    }

    public static String cashAmount() {
        String s = ManualPurchaseOrder.P_CashAmount;
        return s;
    }

    public static String note() {
        String s = ManualPurchaseOrder.P_Note;
        return s;
    }

    public static String applied() {
        String s = ManualPurchaseOrder.P_Applied;
        return s;
    }

    public static String apply() {
        String s = "apply";
        return s;
    }

    public static String pp() {
        return ""; // this
    }
}
 
