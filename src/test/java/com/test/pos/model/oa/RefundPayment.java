package com.test.pos.model.oa;
 
import java.util.*;
import java.util.logging.*;
import java.sql.*;
import com.viaoa.object.*;
import com.viaoa.hub.*;
import com.viaoa.annotation.*;
import com.viaoa.lang.*;
import com.viaoa.callback.OAObjectCallback;
import com.viaoa.datetime.OADateTime;
import com.test.pos.delegate.oa.*;
import com.test.pos.model.oa.filter.*;
import com.test.pos.model.oa.propertypath.*;
 
@OAClass(
    lowerName = "refundPayment",
    pluralName = "RefundPayments",
    shortName = "rfp",
    displayName = "Refund Payment",
    displayProperty = "id",
    noPojo = true
)
@OATable(
    indexes = {
        @OAIndex(name = "RefundPaymentInvoicePayment", fkey = true, columns = { @OAIndexColumn(name = "InvoicePaymentId") }), 
        @OAIndex(name = "RefundPaymentRefundInvoice", fkey = true, columns = { @OAIndexColumn(name = "RefundInvoiceId") }), 
        @OAIndex(name = "RefundPaymentTillLedgerEntry", fkey = true, columns = { @OAIndexColumn(name = "TillLedgerEntryId") })
    }
)
public class RefundPayment extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(RefundPayment.class.getName());

    public static final String P_Id = "id";
    public static final String P_Created = "created";
    public static final String P_Amount = "amount";
    public static final String P_Applied = "applied";
     
    public static final String P_InvoicePayment = "invoicePayment";
    public static final String P_InvoicePaymentId = "invoicePaymentId"; // fkey
    public static final String P_RefundInvoice = "refundInvoice";
    public static final String P_RefundInvoiceId = "refundInvoiceId"; // fkey
    public static final String P_TillLedgerEntry = "tillLedgerEntry";
    public static final String P_TillLedgerEntryId = "tillLedgerEntryId"; // fkey
     
    public static final String M_Apply = "apply";
    protected volatile int id;
    protected volatile OADateTime created;
    protected volatile double amount;
    protected volatile OADateTime applied;
     
    // Links to other objects.
    protected volatile transient InvoicePayment invoicePayment;
    protected volatile transient RefundInvoice refundInvoice;
    protected volatile transient TillLedgerEntry tillLedgerEntry;
     
    public RefundPayment() {
        if (!isLoading()) setObjectDefaults();
    }
    @Override
    public void setObjectDefaults() {
        setCreated(new OADateTime());
    }
     
    public RefundPayment(int id) {
        this();
        setId(id);
    }

    @OAProperty(lowerName = "id", isUnique = true, trackPrimitiveNull = false, displayLength = 6)
    @OAId
    @OAColumn(name = "Id", sqlType = java.sql.Types.INTEGER)
    public int getId() {
        return id;
    }
    public void setId(int newValue) {
        int old = id;
        fireBeforePropertyChange(P_Id, old, newValue);
        this.id = newValue;
        firePropertyChange(P_Id, old, this.id);
    }

    @OAProperty(lowerName = "created", defaultValue = "new OADateTime()", displayLength = 15, isProcessed = true, ignoreTimeZone = true)
    @OAColumn(name = "Created", sqlType = java.sql.Types.TIMESTAMP)
    public OADateTime getCreated() {
        return created;
    }
    public void setCreated(OADateTime newValue) {
        OADateTime old = created;
        fireBeforePropertyChange(P_Created, old, newValue);
        this.created = newValue;
        firePropertyChange(P_Created, old, this.created);
    }

    @OAProperty(lowerName = "amount", decimalPlaces = 2, isCurrency = true, displayLength = 9, uiColumnLength = 8)
    @OAColumn(name = "Amount", sqlType = java.sql.Types.NUMERIC)
    public double getAmount() {
        return amount;
    }
    public void setAmount(double newValue) {
        double old = amount;
        fireBeforePropertyChange(P_Amount, old, newValue);
        this.amount = newValue;
        firePropertyChange(P_Amount, old, this.amount);
    }

    @OAProperty(lowerName = "applied", displayLength = 15, isProcessed = true, ignoreTimeZone = true)
    @OAColumn(name = "Applied", sqlType = java.sql.Types.TIMESTAMP)
    public OADateTime getApplied() {
        return applied;
    }
    public void setApplied(OADateTime newValue) {
        OADateTime old = applied;
        fireBeforePropertyChange(P_Applied, old, newValue);
        this.applied = newValue;
        firePropertyChange(P_Applied, old, this.applied);
    }

    @OAOne(
        displayName = "Invoice Payment", 
        reverseName = InvoicePayment.P_RefundPayments, 
        allowCreateNew = false, 
        selectFromPropertyPath = P_RefundInvoice + "." + RefundInvoice.P_Invoice + "." + Invoice.P_InvoicePayments, 
        fkeys = {@OAFkey(fromProperty = P_InvoicePaymentId, toProperty = InvoicePayment.P_Id)}
    )
    public InvoicePayment getInvoicePayment() {
        if (invoicePayment == null) {
            invoicePayment = (InvoicePayment) getObject(P_InvoicePayment);
        }
        return invoicePayment;
    }
    public void setInvoicePayment(InvoicePayment newValue) {
        InvoicePayment old = this.invoicePayment;
        fireBeforePropertyChange(P_InvoicePayment, old, newValue);
        this.invoicePayment = newValue;
        firePropertyChange(P_InvoicePayment, old, this.invoicePayment);
    }
    @OAProperty(isFkeyOnly = true)
    @OAColumn(name = "InvoicePaymentId")
    public Integer getInvoicePaymentId() {
        return (Integer) getFkeyProperty(P_InvoicePaymentId);
    }
    public void setInvoicePaymentId(Integer newValue) {
        this.invoicePayment = null;
        setFkeyProperty(P_InvoicePaymentId, newValue);
    }

    @OAOne(
        displayName = "Refund Invoice", 
        reverseName = RefundInvoice.P_RefundPayments, 
        required = true, 
        allowCreateNew = false, 
        fkeys = {@OAFkey(fromProperty = P_RefundInvoiceId, toProperty = RefundInvoice.P_Id)}
    )
    public RefundInvoice getRefundInvoice() {
        if (refundInvoice == null) {
            refundInvoice = (RefundInvoice) getObject(P_RefundInvoice);
        }
        return refundInvoice;
    }
    public void setRefundInvoice(RefundInvoice newValue) {
        RefundInvoice old = this.refundInvoice;
        fireBeforePropertyChange(P_RefundInvoice, old, newValue);
        this.refundInvoice = newValue;
        firePropertyChange(P_RefundInvoice, old, this.refundInvoice);
    }
    @OAProperty(isFkeyOnly = true)
    @OAColumn(name = "RefundInvoiceId")
    public Integer getRefundInvoiceId() {
        return (Integer) getFkeyProperty(P_RefundInvoiceId);
    }
    public void setRefundInvoiceId(Integer newValue) {
        this.refundInvoice = null;
        setFkeyProperty(P_RefundInvoiceId, newValue);
    }

    @OAOne(
        displayName = "Till Ledger Entry", 
        reverseName = TillLedgerEntry.P_RefundPayment, 
        isOneAndOnlyOne = true, 
        fkeys = {@OAFkey(fromProperty = P_TillLedgerEntryId, toProperty = TillLedgerEntry.P_Id)}
    )
    public TillLedgerEntry getTillLedgerEntry() {
        if (tillLedgerEntry == null) {
            tillLedgerEntry = (TillLedgerEntry) getObject(P_TillLedgerEntry);
        }
        return tillLedgerEntry;
    }
    public void setTillLedgerEntry(TillLedgerEntry newValue) {
        TillLedgerEntry old = this.tillLedgerEntry;
        fireBeforePropertyChange(P_TillLedgerEntry, old, newValue);
        this.tillLedgerEntry = newValue;
        firePropertyChange(P_TillLedgerEntry, old, this.tillLedgerEntry);
    }
    @OAProperty(isFkeyOnly = true)
    @OAColumn(name = "TillLedgerEntryId")
    public Integer getTillLedgerEntryId() {
        return (Integer) getFkeyProperty(P_TillLedgerEntryId);
    }
    public void setTillLedgerEntryId(Integer newValue) {
        this.tillLedgerEntry = null;
        setFkeyProperty(P_TillLedgerEntryId, newValue);
    }
    @OAMethod(displayName = "Apply")
    public void apply() throws Exception {
        // custom code
        RefundPaymentDelegate.apply(this);
    }

    public void load(ResultSet rs, int id) throws SQLException {
        this.id = id;
        java.sql.Timestamp timestamp;
        timestamp = rs.getTimestamp(2);
        if (timestamp != null) this.created = new OADateTime(timestamp);
        this.amount = rs.getDouble(3);
        setPrimitiveNull(P_Amount, rs.wasNull());
        timestamp = rs.getTimestamp(4);
        if (timestamp != null) this.applied = new OADateTime(timestamp);
        int invoicePaymentFkey = rs.getInt(5);
        setFkeyProperty(P_InvoicePayment, rs.wasNull() ? null : invoicePaymentFkey);
        int refundInvoiceFkey = rs.getInt(6);
        setFkeyProperty(P_RefundInvoice, rs.wasNull() ? null : refundInvoiceFkey);
        int tillLedgerEntryFkey = rs.getInt(7);
        setFkeyProperty(P_TillLedgerEntry, rs.wasNull() ? null : tillLedgerEntryFkey);

        this.changedFlag = false;
        this.newFlag = false;
    }
}
 
