package com.test.pos.model.oa.propertypath;
 
import java.io.Serializable;
import com.test.pos.model.oa.*;
 
public class StoreSafePPx implements PPxInterface, Serializable {
    private static final long serialVersionUID = 1L;
    public final String pp;  // propertyPath
     
    public StoreSafePPx(String name) {
        this(null, name);
    }

    public StoreSafePPx(PPxInterface parent, String name) {
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

    public BankDepositPPx bankDeposits() {
        BankDepositPPx ppx = new BankDepositPPx(this, StoreSafe.P_BankDeposits);
        return ppx;
    }

    public InvoicePaymentCheckPPx invoicePaymentChecks() {
        InvoicePaymentCheckPPx ppx = new InvoicePaymentCheckPPx(this, StoreSafe.P_InvoicePaymentChecks);
        return ppx;
    }

    public StorePPx store() {
        StorePPx ppx = new StorePPx(this, StoreSafe.P_Store);
        return ppx;
    }

    public StoreSafeLedgerEntryPPx storeSafeLedgerEntries() {
        StoreSafeLedgerEntryPPx ppx = new StoreSafeLedgerEntryPPx(this, StoreSafe.P_StoreSafeLedgerEntries);
        return ppx;
    }

    public String id() {
        return pp + "." + StoreSafe.P_Id;
    }

    public String created() {
        return pp + "." + StoreSafe.P_Created;
    }

    public String name() {
        return pp + "." + StoreSafe.P_Name;
    }

    public String cashAmount() {
        return pp + "." + StoreSafe.P_CashAmount;
    }

    public String pettyCashAmount() {
        return pp + "." + StoreSafe.P_PettyCashAmount;
    }

    public String checkCount() {
        return pp + "." + StoreSafe.P_CheckCount;
    }

    public String totalCheckAmount() {
        return pp + "." + StoreSafe.P_TotalCheckAmount;
    }

    @Override
    public String toString() {
        return pp;
    }
    public String pp() {
        return pp;
    }
}
 
