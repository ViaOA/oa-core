package com.test.pos.model.oa.propertypath;
 
import java.io.Serializable;
import com.test.pos.model.oa.*;
 
public class StoreSafeLedgerEntryPPx implements PPxInterface, Serializable {
    private static final long serialVersionUID = 1L;
    public final String pp;  // propertyPath
     
    public StoreSafeLedgerEntryPPx(String name) {
        this(null, name);
    }

    public StoreSafeLedgerEntryPPx(PPxInterface parent, String name) {
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

    public StorePPx calcStore() {
        StorePPx ppx = new StorePPx(this, StoreSafeLedgerEntry.P_CalcStore);
        return ppx;
    }

    public InvoicePaymentCheckPPx invoicePaymentChecks() {
        InvoicePaymentCheckPPx ppx = new InvoicePaymentCheckPPx(this, StoreSafeLedgerEntry.P_InvoicePaymentChecks);
        return ppx;
    }

    public LedgerDenominationBundlePPx ledgerDenominationBundles() {
        LedgerDenominationBundlePPx ppx = new LedgerDenominationBundlePPx(this, StoreSafeLedgerEntry.P_LedgerDenominationBundles);
        return ppx;
    }

    public ManualPurchaseOrderPPx manualPurchaseOrder() {
        ManualPurchaseOrderPPx ppx = new ManualPurchaseOrderPPx(this, StoreSafeLedgerEntry.P_ManualPurchaseOrder);
        return ppx;
    }

    public StoreDayOpenPPx storeDayOpen() {
        StoreDayOpenPPx ppx = new StoreDayOpenPPx(this, StoreSafeLedgerEntry.P_StoreDayOpen);
        return ppx;
    }

    public StoreSafePPx storeSafe() {
        StoreSafePPx ppx = new StoreSafePPx(this, StoreSafeLedgerEntry.P_StoreSafe);
        return ppx;
    }

    public TeamMemberPPx teamMember() {
        TeamMemberPPx ppx = new TeamMemberPPx(this, StoreSafeLedgerEntry.P_TeamMember);
        return ppx;
    }

    public TillLedgerEntryPPx tillLedgerEntry() {
        TillLedgerEntryPPx ppx = new TillLedgerEntryPPx(this, StoreSafeLedgerEntry.P_TillLedgerEntry);
        return ppx;
    }

    public String id() {
        return pp + "." + StoreSafeLedgerEntry.P_Id;
    }

    public String created() {
        return pp + "." + StoreSafeLedgerEntry.P_Created;
    }

    public String type() {
        return pp + "." + StoreSafeLedgerEntry.P_Type;
    }

    public String looseCashAmount() {
        return pp + "." + StoreSafeLedgerEntry.P_LooseCashAmount;
    }

    public String checkCount() {
        return pp + "." + StoreSafeLedgerEntry.P_CheckCount;
    }

    public String checkAmount() {
        return pp + "." + StoreSafeLedgerEntry.P_CheckAmount;
    }

    public String pettyCashAmount() {
        return pp + "." + StoreSafeLedgerEntry.P_PettyCashAmount;
    }

    public String note() {
        return pp + "." + StoreSafeLedgerEntry.P_Note;
    }

    public String posted() {
        return pp + "." + StoreSafeLedgerEntry.P_Posted;
    }

    public String totalCashAmount() {
        return pp + "." + StoreSafeLedgerEntry.P_TotalCashAmount;
    }

    public String calcCheckCount() {
        return pp + "." + StoreSafeLedgerEntry.P_CalcCheckCount;
    }

    public String totalCheckAmount() {
        return pp + "." + StoreSafeLedgerEntry.P_TotalCheckAmount;
    }

    public String totalAmount() {
        return pp + "." + StoreSafeLedgerEntry.P_TotalAmount;
    }

    public String canPost() {
        return pp + "." + StoreSafeLedgerEntry.P_CanPost;
    }

    public String cantPostReason() {
        return pp + "." + StoreSafeLedgerEntry.P_CantPostReason;
    }

    public String usesCash() {
        return pp + "." + StoreSafeLedgerEntry.P_UsesCash;
    }

    public String usesChecks() {
        return pp + "." + StoreSafeLedgerEntry.P_UsesChecks;
    }

    public String usesPettyCash() {
        return pp + "." + StoreSafeLedgerEntry.P_UsesPettyCash;
    }

    public String usesLedgerDenominationBundle() {
        return pp + "." + StoreSafeLedgerEntry.P_UsesLedgerDenominationBundle;
    }

    public String needsToCreateTillLedgerEntry() {
        return pp + "." + StoreSafeLedgerEntry.P_NeedsToCreateTillLedgerEntry;
    }

    public String usesInvoicePaymentChecks() {
        return pp + "." + StoreSafeLedgerEntry.P_UsesInvoicePaymentChecks;
    }

    public String post() {
        return pp + ".post";
    }

    public String createTillLedgerEntry() {
        return pp + ".createTillLedgerEntry";
    }

    @Override
    public String toString() {
        return pp;
    }
    public String pp() {
        return pp;
    }
}
 
