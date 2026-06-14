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
    lowerName = "refundInvoice",
    pluralName = "RefundInvoices",
    shortName = "rfi",
    displayName = "Refund Invoice",
    displayProperty = "id",
    noPojo = true
)
@OATable(
    indexes = {
        @OAIndex(name = "RefundInvoiceInvoice", fkey = true, columns = { @OAIndexColumn(name = "InvoiceId") }), 
        @OAIndex(name = "RefundInvoiceRefund", fkey = true, columns = { @OAIndexColumn(name = "RefundId") })
    }
)
public class RefundInvoice extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(RefundInvoice.class.getName());

    public static final String P_Id = "id";
    public static final String P_Created = "created";
     
    public static final String P_Invoice = "invoice";
    public static final String P_InvoiceId = "invoiceId"; // fkey
    public static final String P_Refund = "refund";
    public static final String P_RefundId = "refundId"; // fkey
    public static final String P_RefundLineItems = "refundLineItems";
    public static final String P_RefundPayments = "refundPayments";
     
    protected volatile int id;
    protected volatile OADateTime created;
     
    // Links to other objects.
    protected volatile transient Invoice invoice;
    protected volatile transient Refund refund;
    protected transient Hub<RefundLineItem> hubRefundLineItems;
    protected transient Hub<RefundPayment> hubRefundPayments;
     
    public RefundInvoice() {
        if (!isLoading()) setObjectDefaults();
    }
    @Override
    public void setObjectDefaults() {
        setCreated(new OADateTime());
    }
     
    public RefundInvoice(int id) {
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

    @OAOne(
        reverseName = Invoice.P_RefundInvoices, 
        allowCreateNew = false, 
        fkeys = {@OAFkey(fromProperty = P_InvoiceId, toProperty = Invoice.P_Id)}
    )
    public Invoice getInvoice() {
        if (invoice == null) {
            invoice = (Invoice) getObject(P_Invoice);
        }
        return invoice;
    }
    public void setInvoice(Invoice newValue) {
        Invoice old = this.invoice;
        fireBeforePropertyChange(P_Invoice, old, newValue);
        this.invoice = newValue;
        firePropertyChange(P_Invoice, old, this.invoice);
    }
    @OAProperty(isFkeyOnly = true)
    @OAColumn(name = "InvoiceId")
    public Integer getInvoiceId() {
        return (Integer) getFkeyProperty(P_InvoiceId);
    }
    public void setInvoiceId(Integer newValue) {
        this.invoice = null;
        setFkeyProperty(P_InvoiceId, newValue);
    }

    @OAOne(
        reverseName = Refund.P_RefundInvoices, 
        required = true, 
        allowCreateNew = false, 
        fkeys = {@OAFkey(fromProperty = P_RefundId, toProperty = Refund.P_Id)}
    )
    public Refund getRefund() {
        if (refund == null) {
            refund = (Refund) getObject(P_Refund);
        }
        return refund;
    }
    public void setRefund(Refund newValue) {
        Refund old = this.refund;
        fireBeforePropertyChange(P_Refund, old, newValue);
        this.refund = newValue;
        firePropertyChange(P_Refund, old, this.refund);
    }
    @OAProperty(isFkeyOnly = true)
    @OAColumn(name = "RefundId")
    public Integer getRefundId() {
        return (Integer) getFkeyProperty(P_RefundId);
    }
    public void setRefundId(Integer newValue) {
        this.refund = null;
        setFkeyProperty(P_RefundId, newValue);
    }

    @OAMany(
        displayName = "Refund Line Items", 
        toClass = RefundLineItem.class, 
        owner = true, 
        reverseName = RefundLineItem.P_RefundInvoice, 
        cascadeSave = true, 
        cascadeDelete = true
    )
    public Hub<RefundLineItem> getRefundLineItems() {
        if (hubRefundLineItems == null) {
            hubRefundLineItems = (Hub<RefundLineItem>) getHub(P_RefundLineItems);
        }
        return hubRefundLineItems;
    }

    @OAMany(
        displayName = "Refund Payments", 
        toClass = RefundPayment.class, 
        owner = true, 
        reverseName = RefundPayment.P_RefundInvoice, 
        cascadeSave = true, 
        cascadeDelete = true
    )
    public Hub<RefundPayment> getRefundPayments() {
        if (hubRefundPayments == null) {
            hubRefundPayments = (Hub<RefundPayment>) getHub(P_RefundPayments);
        }
        return hubRefundPayments;
    }
    public void load(ResultSet rs, int id) throws SQLException {
        this.id = id;
        java.sql.Timestamp timestamp;
        timestamp = rs.getTimestamp(2);
        if (timestamp != null) this.created = new OADateTime(timestamp);
        int invoiceFkey = rs.getInt(3);
        setFkeyProperty(P_Invoice, rs.wasNull() ? null : invoiceFkey);
        int refundFkey = rs.getInt(4);
        setFkeyProperty(P_Refund, rs.wasNull() ? null : refundFkey);

        this.changedFlag = false;
        this.newFlag = false;
    }
}
 
