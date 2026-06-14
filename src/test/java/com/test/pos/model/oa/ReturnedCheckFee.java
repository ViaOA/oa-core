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
    lowerName = "returnedCheckFee",
    pluralName = "ReturnedCheckFees",
    shortName = "rcf",
    displayName = "Returned Check Fee",
    displayProperty = "id",
    noPojo = true
)
@OATable(
    indexes = {
        @OAIndex(name = "ReturnedCheckFeeInvoicePaymentCheck", fkey = true, columns = { @OAIndexColumn(name = "InvoicePaymentCheckId") })
    }
)
public class ReturnedCheckFee extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(ReturnedCheckFee.class.getName());

    public static final String P_Id = "id";
    public static final String P_Created = "created";
    public static final String P_Amount = "amount";
    public static final String P_CollectedDate = "collectedDate";
    public static final String P_Note = "note";
    public static final String P_Status = "status";
    public static final String P_StatusString = "statusString";
    public static final String P_StatusEnum = "statusEnum";
    public static final String P_StatusDisplay = "statusDisplay";
     
    public static final String P_InvoicePaymentCheck = "invoicePaymentCheck";
    public static final String P_InvoicePaymentCheckId = "invoicePaymentCheckId"; // fkey
     
    protected volatile int id;
    protected volatile OADateTime created;
    protected volatile double amount;
    protected volatile OADate collectedDate;
    protected volatile String note;
    protected volatile int status;

    public static enum Status {
        Unknown("Unknown"),
        Pending("Pending"),
        Collected("Collected"),
        Waived("Waived");

        private String display;
        Status(String display) {
            this.display = display;
        }

        public String getDisplay() {
            return display;
        }
    }
    public static final int STATUS_Unknown = 0;
    public static final int STATUS_Pending = 1;
    public static final int STATUS_Collected = 2;
    public static final int STATUS_Waived = 3;

     
    // Links to other objects.
    protected volatile transient InvoicePaymentCheck invoicePaymentCheck;
     
    public ReturnedCheckFee() {
        if (!isLoading()) setObjectDefaults();
    }
    @Override
    public void setObjectDefaults() {
        setCreated(new OADateTime());
    }
     
    public ReturnedCheckFee(int id) {
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

    @OAProperty(lowerName = "collectedDate", displayName = "Collected Date", displayLength = 8, uiColumnLength = 14)
    @OAColumn(name = "CollectedDate", sqlType = java.sql.Types.DATE)
    public OADate getCollectedDate() {
        return collectedDate;
    }
    public void setCollectedDate(OADate newValue) {
        OADate old = collectedDate;
        fireBeforePropertyChange(P_CollectedDate, old, newValue);
        this.collectedDate = newValue;
        firePropertyChange(P_CollectedDate, old, this.collectedDate);
    }

    @OAProperty(lowerName = "note", displayLength = 20)
    @OAColumn(name = "Note", maxLength = 0)
    public String getNote() {
        return note;
    }
    public void setNote(String newValue) {
        String old = note;
        fireBeforePropertyChange(P_Note, old, newValue);
        this.note = newValue;
        firePropertyChange(P_Note, old, this.note);
    }

    @OAProperty(lowerName = "status", displayLength = 6, isNameValue = true)
    @OAColumn(name = "Status", sqlType = java.sql.Types.INTEGER)
    public int getStatus() {
        return status;
    }
    public void setStatus(int newValue) {
        int old = status;
        fireBeforePropertyChange(P_Status, old, newValue);
        this.status = newValue;
        firePropertyChange(P_Status, old, this.status);
    }

    @OAProperty(enumPropertyName = P_Status)
    public String getStatusString() {
        Status status = getStatusEnum();
        if (status == null) return null;
        return status.name();
    }
    public void setStatusString(String val) {
        int x = -1;
        if (OAString.isNotEmpty(val)) {
            Status status = Status.valueOf(val);
            if (status != null) x = status.ordinal();
        }
        if (x < 0) setNull(P_Status);
        else setStatus(x);
    }
    @OAProperty(enumPropertyName = P_Status)
    public Status getStatusEnum() {
        if (isNull(P_Status)) return null;
        final int val = getStatus();
        if (val < 0 || val >= Status.values().length) return null;
        return Status.values()[val];
    }
    public void setStatusEnum(Status val) {
        if (val == null) {
            setNull(P_Status);
        }
        else {
            setStatus(val.ordinal());
        }
    }
    @OACalculatedProperty(enumPropertyName = P_Status, displayName = "Status", displayLength = 6, columnLength = 6, properties = {P_Status} )
    public String getStatusDisplay() {
        Status status = getStatusEnum();
        if (status == null) return null;
        return status.getDisplay();
    }

    @OAOne(
        displayName = "Invoice Payment Check", 
        reverseName = InvoicePaymentCheck.P_ReturnedCheckFee, 
        required = true, 
        allowCreateNew = false, 
        allowAddExisting = false, 
        fkeys = {@OAFkey(fromProperty = P_InvoicePaymentCheckId, toProperty = InvoicePaymentCheck.P_Id)}
    )
    public InvoicePaymentCheck getInvoicePaymentCheck() {
        if (invoicePaymentCheck == null) {
            invoicePaymentCheck = (InvoicePaymentCheck) getObject(P_InvoicePaymentCheck);
        }
        return invoicePaymentCheck;
    }
    public void setInvoicePaymentCheck(InvoicePaymentCheck newValue) {
        InvoicePaymentCheck old = this.invoicePaymentCheck;
        fireBeforePropertyChange(P_InvoicePaymentCheck, old, newValue);
        this.invoicePaymentCheck = newValue;
        firePropertyChange(P_InvoicePaymentCheck, old, this.invoicePaymentCheck);
    }
    @OAProperty(isFkeyOnly = true)
    @OAColumn(name = "InvoicePaymentCheckId")
    public Integer getInvoicePaymentCheckId() {
        return (Integer) getFkeyProperty(P_InvoicePaymentCheckId);
    }
    public void setInvoicePaymentCheckId(Integer newValue) {
        this.invoicePaymentCheck = null;
        setFkeyProperty(P_InvoicePaymentCheckId, newValue);
    }
    public void load(ResultSet rs, int id) throws SQLException {
        this.id = id;
        java.sql.Timestamp timestamp;
        timestamp = rs.getTimestamp(2);
        if (timestamp != null) this.created = new OADateTime(timestamp);
        this.amount = rs.getDouble(3);
        setPrimitiveNull(P_Amount, rs.wasNull());
        java.sql.Date date;
        date = rs.getDate(4);
        if (date != null) this.collectedDate = new OADate(date);
        this.note = rs.getString(5);
        this.status = rs.getInt(6);
        setPrimitiveNull(P_Status, rs.wasNull());
        int invoicePaymentCheckFkey = rs.getInt(7);
        setFkeyProperty(P_InvoicePaymentCheck, rs.wasNull() ? null : invoicePaymentCheckFkey);

        this.changedFlag = false;
        this.newFlag = false;
    }
}
 
