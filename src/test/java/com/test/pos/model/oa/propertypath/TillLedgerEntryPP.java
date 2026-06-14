package com.test.pos.model.oa.propertypath;
 
import com.test.pos.model.oa.*;
 
public class TillLedgerEntryPP {
    private static InvoicePaymentPPx invoicePayment;
    private static InvoicePaymentCheckPPx invoicePaymentChecks;
    private static LedgerDenominationBundlePPx ledgerDenominationBundles;
    private static RefundPaymentPPx refundPayment;
    private static RegisterSessionPPx registerSession;
    private static StoreSafeLedgerEntryPPx storeSafeLedgerEntry;
    private static TeamMemberPPx teamMember;
    private static TillPPx till;
     

    public static InvoicePaymentPPx invoicePayment() {
        if (invoicePayment == null) invoicePayment = new InvoicePaymentPPx(TillLedgerEntry.P_InvoicePayment);
        return invoicePayment;
    }

    public static InvoicePaymentCheckPPx invoicePaymentChecks() {
        if (invoicePaymentChecks == null) invoicePaymentChecks = new InvoicePaymentCheckPPx(TillLedgerEntry.P_InvoicePaymentChecks);
        return invoicePaymentChecks;
    }

    public static LedgerDenominationBundlePPx ledgerDenominationBundles() {
        if (ledgerDenominationBundles == null) ledgerDenominationBundles = new LedgerDenominationBundlePPx(TillLedgerEntry.P_LedgerDenominationBundles);
        return ledgerDenominationBundles;
    }

    public static RefundPaymentPPx refundPayment() {
        if (refundPayment == null) refundPayment = new RefundPaymentPPx(TillLedgerEntry.P_RefundPayment);
        return refundPayment;
    }

    public static RegisterSessionPPx registerSession() {
        if (registerSession == null) registerSession = new RegisterSessionPPx(TillLedgerEntry.P_RegisterSession);
        return registerSession;
    }

    public static StoreSafeLedgerEntryPPx storeSafeLedgerEntry() {
        if (storeSafeLedgerEntry == null) storeSafeLedgerEntry = new StoreSafeLedgerEntryPPx(TillLedgerEntry.P_StoreSafeLedgerEntry);
        return storeSafeLedgerEntry;
    }

    public static TeamMemberPPx teamMember() {
        if (teamMember == null) teamMember = new TeamMemberPPx(TillLedgerEntry.P_TeamMember);
        return teamMember;
    }

    public static TillPPx till() {
        if (till == null) till = new TillPPx(TillLedgerEntry.P_Till);
        return till;
    }

    public static String id() {
        String s = TillLedgerEntry.P_Id;
        return s;
    }

    public static String created() {
        String s = TillLedgerEntry.P_Created;
        return s;
    }

    public static String type() {
        String s = TillLedgerEntry.P_Type;
        return s;
    }

    public static String looseCashAmount() {
        String s = TillLedgerEntry.P_LooseCashAmount;
        return s;
    }

    public static String checkCount() {
        String s = TillLedgerEntry.P_CheckCount;
        return s;
    }

    public static String checkAmount() {
        String s = TillLedgerEntry.P_CheckAmount;
        return s;
    }

    public static String posted() {
        String s = TillLedgerEntry.P_Posted;
        return s;
    }

    public static String note() {
        String s = TillLedgerEntry.P_Note;
        return s;
    }

    public static String totalCashAmount() {
        String s = TillLedgerEntry.P_TotalCashAmount;
        return s;
    }

    public static String calcCheckCount() {
        String s = TillLedgerEntry.P_CalcCheckCount;
        return s;
    }

    public static String calcTotalCheckAmount() {
        String s = TillLedgerEntry.P_CalcTotalCheckAmount;
        return s;
    }

    public static String totalAmount() {
        String s = TillLedgerEntry.P_TotalAmount;
        return s;
    }

    public static String canPost() {
        String s = TillLedgerEntry.P_CanPost;
        return s;
    }

    public static String cantPostReason() {
        String s = TillLedgerEntry.P_CantPostReason;
        return s;
    }

    public static String usesCash() {
        String s = TillLedgerEntry.P_UsesCash;
        return s;
    }

    public static String usesChecks() {
        String s = TillLedgerEntry.P_UsesChecks;
        return s;
    }

    public static String usesLedgerDenominationBundle() {
        String s = TillLedgerEntry.P_UsesLedgerDenominationBundle;
        return s;
    }

    public static String usesInvoicePayment() {
        String s = TillLedgerEntry.P_UsesInvoicePayment;
        return s;
    }

    public static String usesInvoicePaymentChecks() {
        String s = TillLedgerEntry.P_UsesInvoicePaymentChecks;
        return s;
    }

    public static String post() {
        String s = "post";
        return s;
    }

    public static String pp() {
        return ""; // this
    }
}
 
