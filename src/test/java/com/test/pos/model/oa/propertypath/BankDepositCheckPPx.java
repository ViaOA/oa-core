package com.test.pos.model.oa.propertypath;
 
import java.io.Serializable;
import com.test.pos.model.oa.*;
 
public class BankDepositCheckPPx implements PPxInterface, Serializable {
    private static final long serialVersionUID = 1L;
    public final String pp;  // propertyPath
     
    public BankDepositCheckPPx(String name) {
        this(null, name);
    }

    public BankDepositCheckPPx(PPxInterface parent, String name) {
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

    public BankDepositPPx bankDeposit() {
        BankDepositPPx ppx = new BankDepositPPx(this, BankDepositCheck.P_BankDeposit);
        return ppx;
    }

    public InvoicePaymentPPx invoicePaymentCheck() {
        InvoicePaymentPPx ppx = new InvoicePaymentPPx(this, BankDepositCheck.P_InvoicePaymentCheck);
        return ppx;
    }

    public String id() {
        return pp + "." + BankDepositCheck.P_Id;
    }

    public String created() {
        return pp + "." + BankDepositCheck.P_Created;
    }

    public String cleared() {
        return pp + "." + BankDepositCheck.P_Cleared;
    }

    public String rejected() {
        return pp + "." + BankDepositCheck.P_Rejected;
    }

    public String rejectedFeeCollected() {
        return pp + "." + BankDepositCheck.P_RejectedFeeCollected;
    }

    public String feeAmountCollected() {
        return pp + "." + BankDepositCheck.P_FeeAmountCollected;
    }

    @Override
    public String toString() {
        return pp;
    }
    public String pp() {
        return pp;
    }
}
 
