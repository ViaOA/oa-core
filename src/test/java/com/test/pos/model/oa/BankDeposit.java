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
    lowerName = "bankDeposit",
    pluralName = "BankDeposits",
    shortName = "bnd",
    displayName = "Bank Deposit",
    displayProperty = "id",
    noPojo = true
)
@OATable(
    indexes = {
        @OAIndex(name = "BankDepositStoreSafe", fkey = true, columns = { @OAIndexColumn(name = "StoreSafeId") })
    }
)
public class BankDeposit extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(BankDeposit.class.getName());

    public static final String P_Id = "id";
    public static final String P_Created = "created";
    public static final String P_Cash = "cash";
    public static final String P_ReferenceCode = "referenceCode";
    public static final String P_Confirmed = "confirmed";
     
    public static final String P_BankDepositChecks = "bankDepositChecks";
    public static final String P_DepositSeal = "depositSeal";
    public static final String P_StoreSafe = "storeSafe";
    public static final String P_StoreSafeId = "storeSafeId"; // fkey
     
    protected volatile int id;
    protected volatile OADateTime created;
    protected volatile double cash;
    protected volatile String referenceCode;
    protected volatile OADateTime confirmed;
     
    // Links to other objects.
    protected transient Hub<BankDepositCheck> hubBankDepositChecks;
    protected volatile transient DepositSeal depositSeal;
    protected volatile transient StoreSafe storeSafe;
     
    public BankDeposit() {
        if (!isLoading()) setObjectDefaults();
    }
    @Override
    public void setObjectDefaults() {
        setCreated(new OADateTime());
    }
     
    public BankDeposit(int id) {
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

    @OAProperty(lowerName = "cash", decimalPlaces = 2, isCurrency = true, displayLength = 9, uiColumnLength = 8)
    @OAColumn(name = "Cash", sqlType = java.sql.Types.NUMERIC)
    public double getCash() {
        return cash;
    }
    public void setCash(double newValue) {
        double old = cash;
        fireBeforePropertyChange(P_Cash, old, newValue);
        this.cash = newValue;
        firePropertyChange(P_Cash, old, this.cash);
    }

    @OAProperty(lowerName = "referenceCode", displayName = "Reference Code", maxLength = 50, displayLength = 18)
    @OAColumn(name = "ReferenceCode", maxLength = 50)
    public String getReferenceCode() {
        return referenceCode;
    }
    public void setReferenceCode(String newValue) {
        String old = referenceCode;
        fireBeforePropertyChange(P_ReferenceCode, old, newValue);
        this.referenceCode = newValue;
        firePropertyChange(P_ReferenceCode, old, this.referenceCode);
    }

    @OAProperty(lowerName = "confirmed", displayLength = 15, ignoreTimeZone = true)
    @OAColumn(name = "Confirmed", sqlType = java.sql.Types.TIMESTAMP)
    public OADateTime getConfirmed() {
        return confirmed;
    }
    public void setConfirmed(OADateTime newValue) {
        OADateTime old = confirmed;
        fireBeforePropertyChange(P_Confirmed, old, newValue);
        this.confirmed = newValue;
        firePropertyChange(P_Confirmed, old, this.confirmed);
    }

    @OAMany(
        displayName = "Bank Deposit Checks", 
        toClass = BankDepositCheck.class, 
        reverseName = BankDepositCheck.P_BankDeposit
    )
    public Hub<BankDepositCheck> getBankDepositChecks() {
        if (hubBankDepositChecks == null) {
            hubBankDepositChecks = (Hub<BankDepositCheck>) getHub(P_BankDepositChecks);
        }
        return hubBankDepositChecks;
    }

    @OAOne(
        displayName = "Deposit Seal", 
        owner = true, 
        reverseName = DepositSeal.P_BankDeposit, 
        cascadeSave = true, 
        cascadeDelete = true, 
        allowAddExisting = false
    )
    public DepositSeal getDepositSeal() {
        if (depositSeal == null) {
            depositSeal = (DepositSeal) getObject(P_DepositSeal);
        }
        return depositSeal;
    }
    public void setDepositSeal(DepositSeal newValue) {
        DepositSeal old = this.depositSeal;
        fireBeforePropertyChange(P_DepositSeal, old, newValue);
        this.depositSeal = newValue;
        firePropertyChange(P_DepositSeal, old, this.depositSeal);
    }

    @OAOne(
        displayName = "Store Safe", 
        reverseName = StoreSafe.P_BankDeposits, 
        required = true, 
        allowCreateNew = false, 
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
    public void load(ResultSet rs, int id) throws SQLException {
        this.id = id;
        java.sql.Timestamp timestamp;
        timestamp = rs.getTimestamp(2);
        if (timestamp != null) this.created = new OADateTime(timestamp);
        this.cash = rs.getDouble(3);
        setPrimitiveNull(P_Cash, rs.wasNull());
        this.referenceCode = rs.getString(4);
        timestamp = rs.getTimestamp(5);
        if (timestamp != null) this.confirmed = new OADateTime(timestamp);
        int storeSafeFkey = rs.getInt(6);
        setFkeyProperty(P_StoreSafe, rs.wasNull() ? null : storeSafeFkey);

        this.changedFlag = false;
        this.newFlag = false;
    }
}
 
