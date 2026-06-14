package com.test.pos.model.oa.propertypath;
 
import java.io.Serializable;
import com.test.pos.model.oa.*;
 
public class RefundPaymentPPx implements PPxInterface, Serializable {
    private static final long serialVersionUID = 1L;
    public final String pp;  // propertyPath
     
    public RefundPaymentPPx(String name) {
        this(null, name);
    }

    public RefundPaymentPPx(PPxInterface parent, String name) {
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
        InvoicePaymentPPx ppx = new InvoicePaymentPPx(this, RefundPayment.P_InvoicePayment);
        return ppx;
    }

    public RefundInvoicePPx refundInvoice() {
        RefundInvoicePPx ppx = new RefundInvoicePPx(this, RefundPayment.P_RefundInvoice);
        return ppx;
    }

    public TillLedgerEntryPPx tillLedgerEntry() {
        TillLedgerEntryPPx ppx = new TillLedgerEntryPPx(this, RefundPayment.P_TillLedgerEntry);
        return ppx;
    }

    public String id() {
        return pp + "." + RefundPayment.P_Id;
    }

    public String created() {
        return pp + "." + RefundPayment.P_Created;
    }

    public String amount() {
        return pp + "." + RefundPayment.P_Amount;
    }

    public String applied() {
        return pp + "." + RefundPayment.P_Applied;
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
 
