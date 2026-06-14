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
import com.viaoa.datetime.OADate;
import com.test.pos.delegate.oa.*;
import com.test.pos.model.oa.filter.*;
import com.test.pos.model.oa.propertypath.*;
 
@OAClass(
    lowerName = "quote",
    pluralName = "Quotes",
    shortName = "qt",
    displayName = "Quote",
    displayProperty = "name",
    noPojo = true
)
@OATable(
    indexes = {
        @OAIndex(name = "QuoteCustomer", fkey = true, columns = { @OAIndexColumn(name = "CustomerId") }), 
        @OAIndex(name = "QuoteInvoice", fkey = true, columns = { @OAIndexColumn(name = "InvoiceId") })
    }
)
public class Quote extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(Quote.class.getName());

    public static final String P_Id = "id";
    public static final String P_Created = "created";
    public static final String P_Name = "name";
    public static final String P_Note = "note";
    public static final String P_EndDate = "endDate";
     
    public static final String P_Customer = "customer";
    public static final String P_CustomerId = "customerId"; // fkey
    public static final String P_Invoice = "invoice";
    public static final String P_InvoiceId = "invoiceId"; // fkey
     
    protected volatile int id;
    protected volatile OADateTime created;
    protected volatile String name;
    protected volatile String note;
    protected volatile OADate endDate;
     
    // Links to other objects.
    protected volatile transient Customer customer;
    protected volatile transient Invoice invoice;
     
    public Quote() {
        if (!isLoading()) setObjectDefaults();
    }
    @Override
    public void setObjectDefaults() {
        setCreated(new OADateTime());
    }
     
    public Quote(int id) {
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

    @OAProperty(lowerName = "name", maxLength = 50, displayLength = 18)
    @OAColumn(name = "Name", maxLength = 50)
    public String getName() {
        return name;
    }
    public void setName(String newValue) {
        String old = name;
        fireBeforePropertyChange(P_Name, old, newValue);
        this.name = newValue;
        firePropertyChange(P_Name, old, this.name);
    }

    @OAProperty(lowerName = "note", displayLength = 30, uiColumnLength = 20, isHtml = true)
    @OAColumn(name = "Note", sqlType = java.sql.Types.CLOB)
    public String getNote() {
        return note;
    }
    public void setNote(String newValue) {
        String old = note;
        fireBeforePropertyChange(P_Note, old, newValue);
        this.note = newValue;
        firePropertyChange(P_Note, old, this.note);
    }

    @OAProperty(lowerName = "endDate", displayName = "End Date", displayLength = 8)
    @OAColumn(name = "EndDate", sqlType = java.sql.Types.DATE)
    public OADate getEndDate() {
        return endDate;
    }
    public void setEndDate(OADate newValue) {
        OADate old = endDate;
        fireBeforePropertyChange(P_EndDate, old, newValue);
        this.endDate = newValue;
        firePropertyChange(P_EndDate, old, this.endDate);
    }

    @OAOne(
        reverseName = Customer.P_Quotes, 
        allowCreateNew = false, 
        fkeys = {@OAFkey(fromProperty = P_CustomerId, toProperty = Customer.P_Id)}
    )
    public Customer getCustomer() {
        if (customer == null) {
            customer = (Customer) getObject(P_Customer);
        }
        return customer;
    }
    public void setCustomer(Customer newValue) {
        Customer old = this.customer;
        fireBeforePropertyChange(P_Customer, old, newValue);
        this.customer = newValue;
        firePropertyChange(P_Customer, old, this.customer);
    }
    @OAProperty(isFkeyOnly = true)
    @OAColumn(name = "CustomerId")
    public Integer getCustomerId() {
        return (Integer) getFkeyProperty(P_CustomerId);
    }
    public void setCustomerId(Integer newValue) {
        this.customer = null;
        setFkeyProperty(P_CustomerId, newValue);
    }

    @OAOne(
        reverseName = Invoice.P_Quote, 
        allowAddExisting = false, 
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
    public void load(ResultSet rs, int id) throws SQLException {
        this.id = id;
        java.sql.Timestamp timestamp;
        timestamp = rs.getTimestamp(2);
        if (timestamp != null) this.created = new OADateTime(timestamp);
        this.name = rs.getString(3);
        this.note = rs.getString(4);
        java.sql.Date date;
        date = rs.getDate(5);
        if (date != null) this.endDate = new OADate(date);
        int customerFkey = rs.getInt(6);
        setFkeyProperty(P_Customer, rs.wasNull() ? null : customerFkey);
        int invoiceFkey = rs.getInt(7);
        setFkeyProperty(P_Invoice, rs.wasNull() ? null : invoiceFkey);

        this.changedFlag = false;
        this.newFlag = false;
    }
}
 
