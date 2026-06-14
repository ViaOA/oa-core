package com.test.pos.model.oa.propertypath;
 
import java.io.Serializable;
import com.test.pos.model.oa.*;
 
public class TillLedgerEntryPPx implements PPxInterface, Serializable {
    private static final long serialVersionUID = 1L;
    public final String pp;  // propertyPath
     
    public TillLedgerEntryPPx(String name) {
        this(null, name);
    }

    public TillLedgerEntryPPx(PPxInterface parent, String name) {
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

    public InvoicePaymentPPx invoicePayment() {
        InvoicePaymentPPx ppx = new InvoicePaymentPPx(this, TillLedgerEntry.P_InvoicePayment);
        return ppx;
    }

    public InvoicePaymentCheckPPx invoicePaymentChecks() {
        InvoicePaymentCheckPPx ppx = new InvoicePaymentCheckPPx(this, TillLedgerEntry.P_InvoicePaymentChecks);
        return ppx;
    }

    public LedgerDenominationBundlePPx ledgerDenominationBundles() {
        LedgerDenominationBundlePPx ppx = new LedgerDenominationBundlePPx(this, TillLedgerEntry.P_LedgerDenominationBundles);
        return ppx;
    }

    public RefundPaymentPPx refundPayment() {
        RefundPaymentPPx ppx = new RefundPaymentPPx(this, TillLedgerEntry.P_RefundPayment);
        return ppx;
    }

    public RegisterSessionPPx registerSession() {
        RegisterSessionPPx ppx = new RegisterSessionPPx(this, TillLedgerEntry.P_RegisterSession);
        return ppx;
    }

    public StoreSafeLedgerEntryPPx storeSafeLedgerEntry() {
        StoreSafeLedgerEntryPPx ppx = new StoreSafeLedgerEntryPPx(this, TillLedgerEntry.P_StoreSafeLedgerEntry);
        return ppx;
    }

    public TeamMemberPPx teamMember() {
        TeamMemberPPx ppx = new TeamMemberPPx(this, TillLedgerEntry.P_TeamMember);
        return ppx;
    }

    public TillPPx till() {
        TillPPx ppx = new TillPPx(this, TillLedgerEntry.P_Till);
        return ppx;
    }

    public String id() {
        return pp + "." + TillLedgerEntry.P_Id;
    }

    public String created() {
        return pp + "." + TillLedgerEntry.P_Created;
    }

    public String type() {
        return pp + "." + TillLedgerEntry.P_Type;
    }

    public String looseCashAmount() {
        return pp + "." + TillLedgerEntry.P_LooseCashAmount;
    }

    public String checkCount() {
        return pp + "." + TillLedgerEntry.P_CheckCount;
    }

    public String checkAmount() {
        return pp + "." + TillLedgerEntry.P_CheckAmount;
    }

    public String posted() {
        return pp + "." + TillLedgerEntry.P_Posted;
    }

    public String note() {
        return pp + "." + TillLedgerEntry.P_Note;
    }

    public String totalCashAmount() {
        return pp + "." + TillLedgerEntry.P_TotalCashAmount;
    }

    public String calcCheckCount() {
        return pp + "." + TillLedgerEntry.P_CalcCheckCount;
    }

    public String calcTotalCheckAmount() {
        return pp + "." + TillLedgerEntry.P_CalcTotalCheckAmount;
    }

    public String totalAmount() {
        return pp + "." + TillLedgerEntry.P_TotalAmount;
    }

    public String canPost() {
        return pp + "." + TillLedgerEntry.P_CanPost;
    }

    public String cantPostReason() {
        return pp + "." + TillLedgerEntry.P_CantPostReason;
    }

    public String usesCash() {
        return pp + "." + TillLedgerEntry.P_UsesCash;
    }

    public String usesChecks() {
        return pp + "." + TillLedgerEntry.P_UsesChecks;
    }

    public String usesLedgerDenominationBundle() {
        return pp + "." + TillLedgerEntry.P_UsesLedgerDenominationBundle;
    }

    public String usesInvoicePayment() {
        return pp + "." + TillLedgerEntry.P_UsesInvoicePayment;
    }

    public String usesInvoicePaymentChecks() {
        return pp + "." + TillLedgerEntry.P_UsesInvoicePaymentChecks;
    }

    public String post() {
        return pp + ".post";
    }

    @Override
    public String toString() {
        return pp;
    }
    public String pp() {
        return pp;
    }
}
 
