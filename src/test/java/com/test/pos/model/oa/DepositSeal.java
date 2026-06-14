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
    lowerName = "depositSeal",
    pluralName = "DepositSeals",
    shortName = "dps",
    displayName = "Deposit Seal",
    displayProperty = "id",
    noPojo = true
)
@OATable(
    indexes = {
        @OAIndex(name = "DepositSealBankDeposit", fkey = true, columns = { @OAIndexColumn(name = "BankDepositId") })
    }
)
public class DepositSeal extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(DepositSeal.class.getName());

    public static final String P_Id = "id";
    public static final String P_Created = "created";
    public static final String P_SealNumber = "sealNumber";
    public static final String P_IssuedTo = "issuedTo";
    public static final String P_UsedOn = "usedOn";
    public static final String P_Status = "status";
    public static final String P_StatusString = "statusString";
    public static final String P_StatusEnum = "statusEnum";
    public static final String P_StatusDisplay = "statusDisplay";
     
    public static final String P_BankDeposit = "bankDeposit";
    public static final String P_BankDepositId = "bankDepositId"; // fkey
     
    protected volatile int id;
    protected volatile OADateTime created;
    protected volatile String sealNumber;
    protected volatile String issuedTo;
    protected volatile OADate usedOn;
    protected volatile int status;

    public static enum Status {
        Unknown("Unknown"),
        Issued("Issued"),
        Used("Used"),
        Voided("Voided");

        private String display;
        Status(String display) {
            this.display = display;
        }

        public String getDisplay() {
            return display;
        }
    }
    public static final int STATUS_Unknown = 0;
    public static final int STATUS_Issued = 1;
    public static final int STATUS_Used = 2;
    public static final int STATUS_Voided = 3;

     
    // Links to other objects.
    protected volatile transient BankDeposit bankDeposit;
     
    public DepositSeal() {
        if (!isLoading()) setObjectDefaults();
    }
    @Override
    public void setObjectDefaults() {
        setCreated(new OADateTime());
    }
     
    public DepositSeal(int id) {
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

    @OAProperty(lowerName = "sealNumber", displayName = "Seal Number", maxLength = 20, displayLength = 20)
    @OAColumn(name = "SealNumber", maxLength = 20)
    public String getSealNumber() {
        return sealNumber;
    }
    public void setSealNumber(String newValue) {
        String old = sealNumber;
        fireBeforePropertyChange(P_SealNumber, old, newValue);
        this.sealNumber = newValue;
        firePropertyChange(P_SealNumber, old, this.sealNumber);
    }

    @OAProperty(lowerName = "issuedTo", displayName = "Issued To", displayLength = 20)
    @OAColumn(name = "IssuedTo", maxLength = 0)
    public String getIssuedTo() {
        return issuedTo;
    }
    public void setIssuedTo(String newValue) {
        String old = issuedTo;
        fireBeforePropertyChange(P_IssuedTo, old, newValue);
        this.issuedTo = newValue;
        firePropertyChange(P_IssuedTo, old, this.issuedTo);
    }

    @OAProperty(lowerName = "usedOn", displayName = "Used On", displayLength = 8)
    @OAColumn(name = "UsedOn", sqlType = java.sql.Types.DATE)
    public OADate getUsedOn() {
        return usedOn;
    }
    public void setUsedOn(OADate newValue) {
        OADate old = usedOn;
        fireBeforePropertyChange(P_UsedOn, old, newValue);
        this.usedOn = newValue;
        firePropertyChange(P_UsedOn, old, this.usedOn);
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
        displayName = "Bank Deposit", 
        reverseName = BankDeposit.P_DepositSeal, 
        required = true, 
        allowCreateNew = false, 
        allowAddExisting = false, 
        fkeys = {@OAFkey(fromProperty = P_BankDepositId, toProperty = BankDeposit.P_Id)}
    )
    public BankDeposit getBankDeposit() {
        if (bankDeposit == null) {
            bankDeposit = (BankDeposit) getObject(P_BankDeposit);
        }
        return bankDeposit;
    }
    public void setBankDeposit(BankDeposit newValue) {
        BankDeposit old = this.bankDeposit;
        fireBeforePropertyChange(P_BankDeposit, old, newValue);
        this.bankDeposit = newValue;
        firePropertyChange(P_BankDeposit, old, this.bankDeposit);
    }
    @OAProperty(isFkeyOnly = true)
    @OAColumn(name = "BankDepositId")
    public Integer getBankDepositId() {
        return (Integer) getFkeyProperty(P_BankDepositId);
    }
    public void setBankDepositId(Integer newValue) {
        this.bankDeposit = null;
        setFkeyProperty(P_BankDepositId, newValue);
    }
    public void load(ResultSet rs, int id) throws SQLException {
        this.id = id;
        java.sql.Timestamp timestamp;
        timestamp = rs.getTimestamp(2);
        if (timestamp != null) this.created = new OADateTime(timestamp);
        this.sealNumber = rs.getString(3);
        this.issuedTo = rs.getString(4);
        java.sql.Date date;
        date = rs.getDate(5);
        if (date != null) this.usedOn = new OADate(date);
        this.status = rs.getInt(6);
        setPrimitiveNull(P_Status, rs.wasNull());
        int bankDepositFkey = rs.getInt(7);
        setFkeyProperty(P_BankDeposit, rs.wasNull() ? null : bankDepositFkey);

        this.changedFlag = false;
        this.newFlag = false;
    }
}
 
