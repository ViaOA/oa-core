package com.test.pos.model.oa.propertypath;
 
import java.io.Serializable;
import com.test.pos.model.oa.*;
 
public class InvoicePaymentCheckPPx implements PPxInterface, Serializable {
    private static final long serialVersionUID = 1L;
    public final String pp;  // propertyPath
     
    public InvoicePaymentCheckPPx(String name) {
        this(null, name);
    }

    public InvoicePaymentCheckPPx(PPxInterface parent, String name) {
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
        InvoicePaymentPPx ppx = new InvoicePaymentPPx(this, InvoicePaymentCheck.P_InvoicePayment);
        return ppx;
    }

    public ReturnedCheckFeePPx returnedCheckFee() {
        ReturnedCheckFeePPx ppx = new ReturnedCheckFeePPx(this, InvoicePaymentCheck.P_ReturnedCheckFee);
        return ppx;
    }

    public StoreSafePPx storeSafe() {
        StoreSafePPx ppx = new StoreSafePPx(this, InvoicePaymentCheck.P_StoreSafe);
        return ppx;
    }

    public StoreSafeLedgerEntryPPx storeSafeLedgerEntries() {
        StoreSafeLedgerEntryPPx ppx = new StoreSafeLedgerEntryPPx(this, InvoicePaymentCheck.P_StoreSafeLedgerEntries);
        return ppx;
    }

    public TillPPx till() {
        TillPPx ppx = new TillPPx(this, InvoicePaymentCheck.P_Till);
        return ppx;
    }

    public TillLedgerEntryPPx tillLedgerEntries() {
        TillLedgerEntryPPx ppx = new TillLedgerEntryPPx(this, InvoicePaymentCheck.P_TillLedgerEntries);
        return ppx;
    }

    public String id() {
        return pp + "." + InvoicePaymentCheck.P_Id;
    }

    public String created() {
        return pp + "." + InvoicePaymentCheck.P_Created;
    }

    public String location() {
        return pp + "." + InvoicePaymentCheck.P_Location;
    }

    public String checkNumber() {
        return pp + "." + InvoicePaymentCheck.P_CheckNumber;
    }

    public String bankName() {
        return pp + "." + InvoicePaymentCheck.P_BankName;
    }

    public String routingNumber() {
        return pp + "." + InvoicePaymentCheck.P_RoutingNumber;
    }

    public String accountNumber() {
        return pp + "." + InvoicePaymentCheck.P_AccountNumber;
    }

    public String checkDate() {
        return pp + "." + InvoicePaymentCheck.P_CheckDate;
    }

    public String status() {
        return pp + "." + InvoicePaymentCheck.P_Status;
    }

    public String clearDate() {
        return pp + "." + InvoicePaymentCheck.P_ClearDate;
    }

    public String bouncedDate() {
        return pp + "." + InvoicePaymentCheck.P_BouncedDate;
    }

    public String bouncedReason() {
        return pp + "." + InvoicePaymentCheck.P_BouncedReason;
    }

    public String licenseNumber() {
        return pp + "." + InvoicePaymentCheck.P_LicenseNumber;
    }

    public String licenseState() {
        return pp + "." + InvoicePaymentCheck.P_LicenseState;
    }

    @Override
    public String toString() {
        return pp;
    }
    public String pp() {
        return pp;
    }
}
 
