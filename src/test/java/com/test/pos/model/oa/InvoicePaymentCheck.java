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
    lowerName = "invoicePaymentCheck",
    pluralName = "InvoicePaymentChecks",
    shortName = "ipc",
    displayName = "Invoice Payment Check",
    displayProperty = "checkNumber",
    noPojo = true
)
@OATable(
    indexes = {
        @OAIndex(name = "InvoicePaymentCheckInvoicePayment", fkey = true, columns = { @OAIndexColumn(name = "InvoicePaymentId") }), 
        @OAIndex(name = "InvoicePaymentCheckStoreSafe", fkey = true, columns = { @OAIndexColumn(name = "StoreSafeId") }), 
        @OAIndex(name = "InvoicePaymentCheckTill", fkey = true, columns = { @OAIndexColumn(name = "TillId") })
    }
)
public class InvoicePaymentCheck extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(InvoicePaymentCheck.class.getName());

    public static final String P_Id = "id";
    public static final String P_Created = "created";
    public static final String P_Location = "location";
    public static final String P_LocationString = "locationString";
    public static final String P_LocationEnum = "locationEnum";
    public static final String P_LocationDisplay = "locationDisplay";
    public static final String P_CheckNumber = "checkNumber";
    public static final String P_BankName = "bankName";
    public static final String P_RoutingNumber = "routingNumber";
    public static final String P_AccountNumber = "accountNumber";
    public static final String P_CheckDate = "checkDate";
    public static final String P_Status = "status";
    public static final String P_StatusString = "statusString";
    public static final String P_StatusEnum = "statusEnum";
    public static final String P_StatusDisplay = "statusDisplay";
    public static final String P_ClearDate = "clearDate";
    public static final String P_BouncedDate = "bouncedDate";
    public static final String P_BouncedReason = "bouncedReason";
    public static final String P_LicenseNumber = "licenseNumber";
    public static final String P_LicenseState = "licenseState";
     
    public static final String P_InvoicePayment = "invoicePayment";
    public static final String P_InvoicePaymentId = "invoicePaymentId"; // fkey
    public static final String P_ReturnedCheckFee = "returnedCheckFee";
    public static final String P_StoreSafe = "storeSafe";
    public static final String P_StoreSafeId = "storeSafeId"; // fkey
    public static final String P_StoreSafeLedgerEntries = "storeSafeLedgerEntries";
    public static final String P_StoreSafeLedgerEntriesId = "storeSafeLedgerEntriesId"; // fkey
    public static final String P_Till = "till";
    public static final String P_TillId = "tillId"; // fkey
    public static final String P_TillLedgerEntries = "tillLedgerEntries";
    public static final String P_TillLedgerEntriesId = "tillLedgerEntriesId"; // fkey
     
    protected volatile int id;
    protected volatile OADateTime created;
    protected volatile int location;

    public static enum Location {
        unknown("Unknown"),
        Till("Till"),
        Safe("Safe"),
        Bank("Bank"),
        NSF("NSF"),
        Cleared("Cleared"),
        ReturnedToCustomer("Returned To Customer");

        private String display;
        Location(String display) {
            this.display = display;
        }

        public String getDisplay() {
            return display;
        }
    }
    public static final int LOCATION_unknown = 0;
    public static final int LOCATION_Till = 1;
    public static final int LOCATION_Safe = 2;
    public static final int LOCATION_Bank = 3;
    public static final int LOCATION_NSF = 4;
    public static final int LOCATION_Cleared = 5;
    public static final int LOCATION_ReturnedToCustomer = 6;

    protected volatile int checkNumber;
    protected volatile String bankName;
    protected volatile String routingNumber;
    protected volatile String accountNumber;
    protected volatile OADate checkDate;
    protected volatile int status;

    public static enum Status {
        Unknown("Unknown"),
        Pending("Pending"),
        Cleared("Cleared"),
        Bounced("Bounced"),
        Lost("Lost");

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
    public static final int STATUS_Cleared = 2;
    public static final int STATUS_Bounced = 3;
    public static final int STATUS_Lost = 4;

    protected volatile OADate clearDate;
    protected volatile OADate bouncedDate;
    protected volatile String bouncedReason;
    protected volatile String licenseNumber;
    protected volatile String licenseState;
     
    // Links to other objects.
    protected volatile transient InvoicePayment invoicePayment;
    protected volatile transient ReturnedCheckFee returnedCheckFee;
    protected volatile transient StoreSafe storeSafe;
    protected volatile transient Till till;
     
    public InvoicePaymentCheck() {
        if (!isLoading()) setObjectDefaults();
    }
    @Override
    public void setObjectDefaults() {
        setCreated(new OADateTime());
    }
     
    public InvoicePaymentCheck(int id) {
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

    @OAProperty(lowerName = "location", trackPrimitiveNull = false, displayLength = 18, uiColumnLength = 14, isNameValue = true)
    @OAColumn(name = "Location", sqlType = java.sql.Types.INTEGER)
    public int getLocation() {
        return location;
    }
    public void setLocation(int newValue) {
        int old = location;
        fireBeforePropertyChange(P_Location, old, newValue);
        this.location = newValue;
        firePropertyChange(P_Location, old, this.location);
    }

    @OAProperty(enumPropertyName = P_Location)
    public String getLocationString() {
        Location location = getLocationEnum();
        if (location == null) return null;
        return location.name();
    }
    public void setLocationString(String val) {
        int x = -1;
        if (OAString.isNotEmpty(val)) {
            Location location = Location.valueOf(val);
            if (location != null) x = location.ordinal();
        }
        if (x < 0) x = 0;
        setLocation(x);
    }
    @OAProperty(enumPropertyName = P_Location)
    public Location getLocationEnum() {
        final int val = getLocation();
        if (val < 0 || val >= Location.values().length) return null;
        return Location.values()[val];
    }
    public void setLocationEnum(Location val) {
        if (val == null) {
            setLocation(0);
        }
        else {
            setLocation(val.ordinal());
        }
    }
    @OACalculatedProperty(enumPropertyName = P_Location, displayName = "Location", displayLength = 18, columnLength = 14, properties = {P_Location} )
    public String getLocationDisplay() {
        Location location = getLocationEnum();
        if (location == null) return null;
        return location.getDisplay();
    }

    @OAProperty(lowerName = "checkNumber", displayName = "Check Number", displayLength = 6, uiColumnLength = 12)
    @OAColumn(name = "CheckNumber", sqlType = java.sql.Types.INTEGER)
    public int getCheckNumber() {
        return checkNumber;
    }
    public void setCheckNumber(int newValue) {
        int old = checkNumber;
        fireBeforePropertyChange(P_CheckNumber, old, newValue);
        this.checkNumber = newValue;
        firePropertyChange(P_CheckNumber, old, this.checkNumber);
    }

    @OAProperty(lowerName = "bankName", displayName = "Bank Name", displayLength = 20)
    @OAColumn(name = "BankName", maxLength = 0)
    public String getBankName() {
        return bankName;
    }
    public void setBankName(String newValue) {
        String old = bankName;
        fireBeforePropertyChange(P_BankName, old, newValue);
        this.bankName = newValue;
        firePropertyChange(P_BankName, old, this.bankName);
    }

    @OAProperty(lowerName = "routingNumber", displayName = "Routing Number", maxLength = 20, displayLength = 20)
    @OAColumn(name = "RoutingNumber", maxLength = 20)
    public String getRoutingNumber() {
        return routingNumber;
    }
    public void setRoutingNumber(String newValue) {
        String old = routingNumber;
        fireBeforePropertyChange(P_RoutingNumber, old, newValue);
        this.routingNumber = newValue;
        firePropertyChange(P_RoutingNumber, old, this.routingNumber);
    }

    @OAProperty(lowerName = "accountNumber", displayName = "Account Number", maxLength = 20, displayLength = 20)
    @OAColumn(name = "AccountNumber", maxLength = 20)
    public String getAccountNumber() {
        return accountNumber;
    }
    public void setAccountNumber(String newValue) {
        String old = accountNumber;
        fireBeforePropertyChange(P_AccountNumber, old, newValue);
        this.accountNumber = newValue;
        firePropertyChange(P_AccountNumber, old, this.accountNumber);
    }

    @OAProperty(lowerName = "checkDate", displayName = "Check Date", displayLength = 8, uiColumnLength = 10)
    @OAColumn(name = "CheckDate", sqlType = java.sql.Types.DATE)
    public OADate getCheckDate() {
        return checkDate;
    }
    public void setCheckDate(OADate newValue) {
        OADate old = checkDate;
        fireBeforePropertyChange(P_CheckDate, old, newValue);
        this.checkDate = newValue;
        firePropertyChange(P_CheckDate, old, this.checkDate);
    }

    @OAProperty(lowerName = "status", trackPrimitiveNull = false, displayLength = 6, isNameValue = true)
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
        if (x < 0) x = 0;
        setStatus(x);
    }
    @OAProperty(enumPropertyName = P_Status)
    public Status getStatusEnum() {
        final int val = getStatus();
        if (val < 0 || val >= Status.values().length) return null;
        return Status.values()[val];
    }
    public void setStatusEnum(Status val) {
        if (val == null) {
            setStatus(0);
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

    @OAProperty(lowerName = "clearDate", displayName = "Clear Date", displayLength = 8, uiColumnLength = 10)
    @OAColumn(name = "ClearDate", sqlType = java.sql.Types.DATE)
    public OADate getClearDate() {
        return clearDate;
    }
    public void setClearDate(OADate newValue) {
        OADate old = clearDate;
        fireBeforePropertyChange(P_ClearDate, old, newValue);
        this.clearDate = newValue;
        firePropertyChange(P_ClearDate, old, this.clearDate);
    }

    @OAProperty(lowerName = "bouncedDate", displayName = "Bounced Date", displayLength = 8, uiColumnLength = 12)
    @OAColumn(name = "BouncedDate", sqlType = java.sql.Types.DATE)
    public OADate getBouncedDate() {
        return bouncedDate;
    }
    public void setBouncedDate(OADate newValue) {
        OADate old = bouncedDate;
        fireBeforePropertyChange(P_BouncedDate, old, newValue);
        this.bouncedDate = newValue;
        firePropertyChange(P_BouncedDate, old, this.bouncedDate);
    }

    @OAProperty(lowerName = "bouncedReason", displayName = "Bounced Reason", displayLength = 20)
    @OAColumn(name = "BouncedReason", maxLength = 0)
    public String getBouncedReason() {
        return bouncedReason;
    }
    public void setBouncedReason(String newValue) {
        String old = bouncedReason;
        fireBeforePropertyChange(P_BouncedReason, old, newValue);
        this.bouncedReason = newValue;
        firePropertyChange(P_BouncedReason, old, this.bouncedReason);
    }

    @OAProperty(lowerName = "licenseNumber", displayName = "License Number", maxLength = 20, displayLength = 20)
    @OAColumn(name = "LicenseNumber", maxLength = 20)
    public String getLicenseNumber() {
        return licenseNumber;
    }
    public void setLicenseNumber(String newValue) {
        String old = licenseNumber;
        fireBeforePropertyChange(P_LicenseNumber, old, newValue);
        this.licenseNumber = newValue;
        firePropertyChange(P_LicenseNumber, old, this.licenseNumber);
    }

    @OAProperty(lowerName = "licenseState", displayName = "License State", displayLength = 20)
    @OAColumn(name = "LicenseState", maxLength = 0)
    public String getLicenseState() {
        return licenseState;
    }
    public void setLicenseState(String newValue) {
        String old = licenseState;
        fireBeforePropertyChange(P_LicenseState, old, newValue);
        this.licenseState = newValue;
        firePropertyChange(P_LicenseState, old, this.licenseState);
    }

    @OAOne(
        displayName = "Invoice Payment", 
        reverseName = InvoicePayment.P_InvoicePaymentCheck, 
        allowCreateNew = false, 
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
        displayName = "Returned Check Fee", 
        owner = true, 
        reverseName = ReturnedCheckFee.P_InvoicePaymentCheck, 
        cascadeSave = true, 
        cascadeDelete = true, 
        allowAddExisting = false
    )
    public ReturnedCheckFee getReturnedCheckFee() {
        if (returnedCheckFee == null) {
            returnedCheckFee = (ReturnedCheckFee) getObject(P_ReturnedCheckFee);
        }
        return returnedCheckFee;
    }
    public void setReturnedCheckFee(ReturnedCheckFee newValue) {
        ReturnedCheckFee old = this.returnedCheckFee;
        fireBeforePropertyChange(P_ReturnedCheckFee, old, newValue);
        this.returnedCheckFee = newValue;
        firePropertyChange(P_ReturnedCheckFee, old, this.returnedCheckFee);
    }

    @OAOne(
        displayName = "Store Safe", 
        reverseName = StoreSafe.P_InvoicePaymentChecks, 
        isProcessed = true, 
        allowCreateNew = false, 
        allowAddExisting = false, 
        fkeys = {@OAFkey(fromProperty = P_StoreSafeId, toProperty = StoreSafe.P_Id)}
    )
    public StoreSafe getStoreSafe() {
        if (storeSafe == null) {
            storeSafe = (StoreSafe) getObject(P_StoreSafe);
        }
        return storeSafe;
    }
    public void setStoreSafe(StoreSafe newValue) {
        StoreSafe old = this.storeSafe;
        fireBeforePropertyChange(P_StoreSafe, old, newValue);
        this.storeSafe = newValue;
        firePropertyChange(P_StoreSafe, old, this.storeSafe);
    }
    @OAProperty(isFkeyOnly = true)
    @OAColumn(name = "StoreSafeId")
    public Integer getStoreSafeId() {
        return (Integer) getFkeyProperty(P_StoreSafeId);
    }
    public void setStoreSafeId(Integer newValue) {
        this.storeSafe = null;
        setFkeyProperty(P_StoreSafeId, newValue);
    }

    @OAMany(
        displayName = "Store Safe Ledger Entries", 
        toClass = StoreSafeLedgerEntry.class, 
        reverseName = StoreSafeLedgerEntry.P_InvoicePaymentChecks, 
        createMethod = false
    )
    @OALinkTable(name = "StoreSafeLedgerEntryInvoicePaymentCheck", indexName = "StoreSafeLedgerEntryInvoicePaymentCheck", columns = {"InvoicePaymentCheckId"})
    private Hub<StoreSafeLedgerEntry> getStoreSafeLedgerEntries() {
        // oamodel has createMethod set to false, this method exists only for annotations.
        return null;
    }

    @OAOne(
        reverseName = Till.P_InvoicePaymentChecks, 
        isProcessed = true, 
        allowCreateNew = false, 
        allowAddExisting = false, 
        fkeys = {@OAFkey(fromProperty = P_TillId, toProperty = Till.P_Id)}
    )
    public Till getTill() {
        if (till == null) {
            till = (Till) getObject(P_Till);
        }
        return till;
    }
    public void setTill(Till newValue) {
        Till old = this.till;
        fireBeforePropertyChange(P_Till, old, newValue);
        this.till = newValue;
        firePropertyChange(P_Till, old, this.till);
    }
    @OAProperty(isFkeyOnly = true)
    @OAColumn(name = "TillId")
    public Integer getTillId() {
        return (Integer) getFkeyProperty(P_TillId);
    }
    public void setTillId(Integer newValue) {
        this.till = null;
        setFkeyProperty(P_TillId, newValue);
    }

    @OAMany(
        displayName = "Till Ledger Entries", 
        toClass = TillLedgerEntry.class, 
        reverseName = TillLedgerEntry.P_InvoicePaymentChecks, 
        createMethod = false
    )
    @OALinkTable(name = "TillLedgerEntryInvoicePaymentCheck", indexName = "TillLedgerEntryInvoicePaymentCheck", columns = {"InvoicePaymentCheckId"})
    private Hub<TillLedgerEntry> getTillLedgerEntries() {
        // oamodel has createMethod set to false, this method exists only for annotations.
        return null;
    }
    public void load(ResultSet rs, int id) throws SQLException {
        this.id = id;
        java.sql.Timestamp timestamp;
        timestamp = rs.getTimestamp(2);
        if (timestamp != null) this.created = new OADateTime(timestamp);
        this.location = rs.getInt(3);
        this.checkNumber = rs.getInt(4);
        setPrimitiveNull(P_CheckNumber, rs.wasNull());
        this.bankName = rs.getString(5);
        this.routingNumber = rs.getString(6);
        this.accountNumber = rs.getString(7);
        java.sql.Date date;
        date = rs.getDate(8);
        if (date != null) this.checkDate = new OADate(date);
        this.status = rs.getInt(9);
        date = rs.getDate(10);
        if (date != null) this.clearDate = new OADate(date);
        date = rs.getDate(11);
        if (date != null) this.bouncedDate = new OADate(date);
        this.bouncedReason = rs.getString(12);
        this.licenseNumber = rs.getString(13);
        this.licenseState = rs.getString(14);
        int invoicePaymentFkey = rs.getInt(15);
        setFkeyProperty(P_InvoicePayment, rs.wasNull() ? null : invoicePaymentFkey);
        int storeSafeFkey = rs.getInt(16);
        setFkeyProperty(P_StoreSafe, rs.wasNull() ? null : storeSafeFkey);
        int tillFkey = rs.getInt(17);
        setFkeyProperty(P_Till, rs.wasNull() ? null : tillFkey);

        this.changedFlag = false;
        this.newFlag = false;
    }
}
 
