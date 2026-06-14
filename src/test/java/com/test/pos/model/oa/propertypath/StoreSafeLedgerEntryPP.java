package com.test.pos.model.oa.propertypath;
 
import com.test.pos.model.oa.*;
 
public class StoreSafeLedgerEntryPP {
    private static StorePPx calcStore;
    private static InvoicePaymentCheckPPx invoicePaymentChecks;
    private static LedgerDenominationBundlePPx ledgerDenominationBundles;
    private static ManualPurchaseOrderPPx manualPurchaseOrder;
    private static StoreDayOpenPPx storeDayOpen;
    private static StoreSafePPx storeSafe;
    private static TeamMemberPPx teamMember;
    private static TillLedgerEntryPPx tillLedgerEntry;
     

    public static StorePPx calcStore() {
        if (calcStore == null) calcStore = new StorePPx(StoreSafeLedgerEntry.P_CalcStore);
        return calcStore;
    }

    public static InvoicePaymentCheckPPx invoicePaymentChecks() {
        if (invoicePaymentChecks == null) invoicePaymentChecks = new InvoicePaymentCheckPPx(StoreSafeLedgerEntry.P_InvoicePaymentChecks);
        return invoicePaymentChecks;
    }

    public static LedgerDenominationBundlePPx ledgerDenominationBundles() {
        if (ledgerDenominationBundles == null) ledgerDenominationBundles = new LedgerDenominationBundlePPx(StoreSafeLedgerEntry.P_LedgerDenominationBundles);
        return ledgerDenominationBundles;
    }

    public static ManualPurchaseOrderPPx manualPurchaseOrder() {
        if (manualPurchaseOrder == null) manualPurchaseOrder = new ManualPurchaseOrderPPx(StoreSafeLedgerEntry.P_ManualPurchaseOrder);
        return manualPurchaseOrder;
    }

    public static StoreDayOpenPPx storeDayOpen() {
        if (storeDayOpen == null) storeDayOpen = new StoreDayOpenPPx(StoreSafeLedgerEntry.P_StoreDayOpen);
        return storeDayOpen;
    }

    public static StoreSafePPx storeSafe() {
        if (storeSafe == null) storeSafe = new StoreSafePPx(StoreSafeLedgerEntry.P_StoreSafe);
        return storeSafe;
    }

    public static TeamMemberPPx teamMember() {
        if (teamMember == null) teamMember = new TeamMemberPPx(StoreSafeLedgerEntry.P_TeamMember);
        return teamMember;
    }

    public static TillLedgerEntryPPx tillLedgerEntry() {
        if (tillLedgerEntry == null) tillLedgerEntry = new TillLedgerEntryPPx(StoreSafeLedgerEntry.P_TillLedgerEntry);
        return tillLedgerEntry;
    }

    public static String id() {
        String s = StoreSafeLedgerEntry.P_Id;
        return s;
    }

    public static String created() {
        String s = StoreSafeLedgerEntry.P_Created;
        return s;
    }

    public static String type() {
        String s = StoreSafeLedgerEntry.P_Type;
        return s;
    }

    public static String looseCashAmount() {
        String s = StoreSafeLedgerEntry.P_LooseCashAmount;
        return s;
    }

    public static String checkCount() {
        String s = StoreSafeLedgerEntry.P_CheckCount;
        return s;
    }

    public static String checkAmount() {
        String s = StoreSafeLedgerEntry.P_CheckAmount;
        return s;
    }

    public static String pettyCashAmount() {
        String s = StoreSafeLedgerEntry.P_PettyCashAmount;
        return s;
    }

    public static String note() {
        String s = StoreSafeLedgerEntry.P_Note;
        return s;
    }

    public static String posted() {
        String s = StoreSafeLedgerEntry.P_Posted;
        return s;
    }

    public static String totalCashAmount() {
        String s = StoreSafeLedgerEntry.P_TotalCashAmount;
        return s;
    }

    public static String calcCheckCount() {
        String s = StoreSafeLedgerEntry.P_CalcCheckCount;
        return s;
    }

    public static String totalCheckAmount() {
        String s = StoreSafeLedgerEntry.P_TotalCheckAmount;
        return s;
    }

    public static String totalAmount() {
        String s = StoreSafeLedgerEntry.P_TotalAmount;
        return s;
    }

    public static String canPost() {
        String s = StoreSafeLedgerEntry.P_CanPost;
        return s;
    }

    public static String cantPostReason() {
        String s = StoreSafeLedgerEntry.P_CantPostReason;
        return s;
    }

    public static String usesCash() {
        String s = StoreSafeLedgerEntry.P_UsesCash;
        return s;
    }

    public static String usesChecks() {
        String s = StoreSafeLedgerEntry.P_UsesChecks;
        return s;
    }

    public static String usesPettyCash() {
        String s = StoreSafeLedgerEntry.P_UsesPettyCash;
        return s;
    }

    public static String usesLedgerDenominationBundle() {
        String s = StoreSafeLedgerEntry.P_UsesLedgerDenominationBundle;
        return s;
    }

    public static String needsToCreateTillLedgerEntry() {
        String s = StoreSafeLedgerEntry.P_NeedsToCreateTillLedgerEntry;
        return s;
    }

    public static String usesInvoicePaymentChecks() {
        String s = StoreSafeLedgerEntry.P_UsesInvoicePaymentChecks;
        return s;
    }

    public static String post() {
        String s = "post";
        return s;
    }

    public static String createTillLedgerEntry() {
        String s = "createTillLedgerEntry";
        return s;
    }

    public static String pp() {
        return ""; // this
    }
}
 
