package com.test.pos.model.oa.propertypath;
 
import java.io.Serializable;
import com.test.pos.model.oa.*;
 
public class InvoicePaymentPPx implements PPxInterface, Serializable {
    private static final long serialVersionUID = 1L;
    public final String pp;  // propertyPath
     
    public InvoicePaymentPPx(String name) {
        this(null, name);
    }

    public InvoicePaymentPPx(PPxInterface parent, String name) {
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

    public BankDepositCheckPPx bankDepositCheck() {
        BankDepositCheckPPx ppx = new BankDepositCheckPPx(this, InvoicePayment.P_BankDepositCheck);
        return ppx;
    }

    public InvoicePPx invoice() {
        InvoicePPx ppx = new InvoicePPx(this, InvoicePayment.P_Invoice);
        return ppx;
    }

    public InvoicePaymentCheckPPx invoicePaymentCheck() {
        InvoicePaymentCheckPPx ppx = new InvoicePaymentCheckPPx(this, InvoicePayment.P_InvoicePaymentCheck);
        return ppx;
    }

    public RefundPaymentPPx refundPayments() {
        RefundPaymentPPx ppx = new RefundPaymentPPx(this, InvoicePayment.P_RefundPayments);
        return ppx;
    }

    public TillLedgerEntryPPx tillLedgerEntry() {
        TillLedgerEntryPPx ppx = new TillLedgerEntryPPx(this, InvoicePayment.P_TillLedgerEntry);
        return ppx;
    }

    public String id() {
        return pp + "." + InvoicePayment.P_Id;
    }

    public String created() {
        return pp + "." + InvoicePayment.P_Created;
    }

    public String type() {
        return pp + "." + InvoicePayment.P_Type;
    }

    public String inputCode() {
        return pp + "." + InvoicePayment.P_InputCode;
    }

    public String outputCode() {
        return pp + "." + InvoicePayment.P_OutputCode;
    }

    public String amount() {
        return pp + "." + InvoicePayment.P_Amount;
    }

    public String cashIn() {
        return pp + "." + InvoicePayment.P_CashIn;
    }

    public String cashOut() {
        return pp + "." + InvoicePayment.P_CashOut;
    }

    public String applied() {
        return pp + "." + InvoicePayment.P_Applied;
    }

    public String typeIsCash() {
        return pp + "." + InvoicePayment.P_TypeIsCash;
    }

    public String typeIsCheck() {
        return pp + "." + InvoicePayment.P_TypeIsCheck;
    }

    public String apply() {
        return pp + ".apply";
    }

    @Override
    public String toString() {
        return pp;
    }
    public String pp() {
        return pp;
    }
}
 
