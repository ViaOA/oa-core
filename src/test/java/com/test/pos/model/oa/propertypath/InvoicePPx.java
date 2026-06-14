package com.test.pos.model.oa.propertypath;
 
import java.io.Serializable;
import com.test.pos.model.oa.*;
 
public class InvoicePPx implements PPxInterface, Serializable {
    private static final long serialVersionUID = 1L;
    public final String pp;  // propertyPath
     
    public InvoicePPx(String name) {
        this(null, name);
    }

    public InvoicePPx(PPxInterface parent, String name) {
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

    public CustomerPPx customer() {
        CustomerPPx ppx = new CustomerPPx(this, Invoice.P_Customer);
        return ppx;
    }

    public InvoiceBasketPPx invoiceBaskets() {
        InvoiceBasketPPx ppx = new InvoiceBasketPPx(this, Invoice.P_InvoiceBaskets);
        return ppx;
    }

    public InvoicePaymentPPx invoicePayments() {
        InvoicePaymentPPx ppx = new InvoicePaymentPPx(this, Invoice.P_InvoicePayments);
        return ppx;
    }

    public PurchaseOrderPPx purchaseOrders() {
        PurchaseOrderPPx ppx = new PurchaseOrderPPx(this, Invoice.P_PurchaseOrders);
        return ppx;
    }

    public QuotePPx quote() {
        QuotePPx ppx = new QuotePPx(this, Invoice.P_Quote);
        return ppx;
    }

    public RefundInvoicePPx refundInvoices() {
        RefundInvoicePPx ppx = new RefundInvoicePPx(this, Invoice.P_RefundInvoices);
        return ppx;
    }

    public RegisterSessionPPx registerSession() {
        RegisterSessionPPx ppx = new RegisterSessionPPx(this, Invoice.P_RegisterSession);
        return ppx;
    }

    public String id() {
        return pp + "." + Invoice.P_Id;
    }

    public String created() {
        return pp + "." + Invoice.P_Created;
    }

    public String completed() {
        return pp + "." + Invoice.P_Completed;
    }

    public String canBeCompleted() {
        return pp + "." + Invoice.P_CanBeCompleted;
    }

    public String totalItemAmount() {
        return pp + "." + Invoice.P_TotalItemAmount;
    }

    public String totalDiscountAmount() {
        return pp + "." + Invoice.P_TotalDiscountAmount;
    }

    public String totalTaxAmount() {
        return pp + "." + Invoice.P_TotalTaxAmount;
    }

    public String totalAmountDue() {
        return pp + "." + Invoice.P_TotalAmountDue;
    }

    public String totalPaymentAmount() {
        return pp + "." + Invoice.P_TotalPaymentAmount;
    }

    public String remainingBalanceAmount() {
        return pp + "." + Invoice.P_RemainingBalanceAmount;
    }

    public String totalRefundAmount() {
        return pp + "." + Invoice.P_TotalRefundAmount;
    }

    public String isPaidInFull() {
        return pp + "." + Invoice.P_IsPaidInFull;
    }

    public String updateWithNetPriceCaclulator() {
        return pp + ".updateWithNetPriceCaclulator";
    }

    public String completeSale() {
        return pp + ".completeSale";
    }

    public InvoicePPx openFilter() {
        InvoicePPx ppx = new InvoicePPx(this, ":open()");
        return ppx;
    }

    @Override
    public String toString() {
        return pp;
    }
    public String pp() {
        return pp;
    }
}
 
