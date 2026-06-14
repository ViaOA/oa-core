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
    lowerName = "purchaseOrder",
    pluralName = "PurchaseOrders",
    shortName = "pro",
    displayName = "Purchase Order",
    displayProperty = "reference",
    noPojo = true
)
@OATable(
)
public class PurchaseOrder extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(PurchaseOrder.class.getName());

    public static final String P_Id = "id";
    public static final String P_Created = "created";
    public static final String P_Reference = "reference";
     
    public static final String P_Invoices = "invoices";
    public static final String P_InvoicesId = "invoicesId"; // fkey
     
    protected volatile int id;
    protected volatile OADateTime created;
    protected volatile String reference;
     
    // Links to other objects.
    protected transient Hub<Invoice> hubInvoices;
     
    public PurchaseOrder() {
        if (!isLoading()) setObjectDefaults();
    }
    @Override
    public void setObjectDefaults() {
        setCreated(new OADateTime());
    }
     
    public PurchaseOrder(int id) {
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

    @OAProperty(lowerName = "reference", maxLength = 35, displayLength = 18)
    @OAColumn(name = "Reference", maxLength = 35)
    public String getReference() {
        return reference;
    }
    public void setReference(String newValue) {
        String old = reference;
        fireBeforePropertyChange(P_Reference, old, newValue);
        this.reference = newValue;
        firePropertyChange(P_Reference, old, this.reference);
    }

    @OAMany(
        toClass = Invoice.class, 
        reverseName = Invoice.P_PurchaseOrders
    )
    @OALinkTable(name = "InvoicePurchaseOrder", indexName = "InvoicePurchaseOrder", columns = {"PurchaseOrderId"})
    public Hub<Invoice> getInvoices() {
        if (hubInvoices == null) {
            hubInvoices = (Hub<Invoice>) getHub(P_Invoices);
        }
        return hubInvoices;
    }
    public void load(ResultSet rs, int id) throws SQLException {
        this.id = id;
        java.sql.Timestamp timestamp;
        timestamp = rs.getTimestamp(2);
        if (timestamp != null) this.created = new OADateTime(timestamp);
        this.reference = rs.getString(3);

        this.changedFlag = false;
        this.newFlag = false;
    }
}
 
