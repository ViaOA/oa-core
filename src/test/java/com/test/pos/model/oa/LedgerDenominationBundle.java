package com.test.pos.model.oa;
 
import java.util.*;
import java.util.logging.*;
import java.sql.*;
import com.viaoa.datetime.*;
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
    lowerName = "ledgerDenominationBundle",
    pluralName = "LedgerDenominationBundles",
    shortName = "ldb",
    displayName = "Ledger Denomination Bundle",
    displayProperty = "id",
    noPojo = true
)
@OATable(
    indexes = {
        @OAIndex(name = "LedgerDenominationBundleDenominationBundle", fkey = true, columns = { @OAIndexColumn(name = "DenominationBundleId") }), 
        @OAIndex(name = "LedgerDenominationBundleStoreSafeLedgerEntry", fkey = true, columns = { @OAIndexColumn(name = "StoreSafeLedgerEntryId") }), 
        @OAIndex(name = "LedgerDenominationBundleTillLedgerEntry", fkey = true, columns = { @OAIndexColumn(name = "TillLedgerEntryId") })
    }
)
public class LedgerDenominationBundle extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(LedgerDenominationBundle.class.getName());

    public static final String P_Id = "id";
    public static final String P_Created = "created";
    public static final String P_Quantity = "quantity";
     
    public static final String P_TotalAmount = "totalAmount";
    public static final String P_Posted = "posted";
    public static final String P_CalcEnabled = "calcEnabled";
     
    public static final String P_CalcStore = "calcStore";
    public static final String P_DenominationBundle = "denominationBundle";
    public static final String P_DenominationBundleId = "denominationBundleId"; // fkey
    public static final String P_StoreSafeLedgerEntry = "storeSafeLedgerEntry";
    public static final String P_StoreSafeLedgerEntryId = "storeSafeLedgerEntryId"; // fkey
    public static final String P_TillLedgerEntry = "tillLedgerEntry";
    public static final String P_TillLedgerEntryId = "tillLedgerEntryId"; // fkey
     
    protected volatile int id;
    protected volatile OADateTime created;
    protected volatile int quantity;
     
    // Links to other objects.
    protected volatile transient DenominationBundle denominationBundle;
    protected volatile transient StoreSafeLedgerEntry storeSafeLedgerEntry;
    protected volatile transient TillLedgerEntry tillLedgerEntry;
     
    public LedgerDenominationBundle() {
        if (!isLoading()) setObjectDefaults();
    }
    @Override
    public void setObjectDefaults() {
        setCreated(new OADateTime());
    }
     
    public LedgerDenominationBundle(int id) {
        this();
        setId(id);
    }
    @OAObjCallback(enabledProperty = LedgerDenominationBundle.P_CalcEnabled)
    public void callback(final OAObjectCallback callback) {
        if (callback == null) return;
        switch (callback.getType()) {
        }
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

    @OAProperty(lowerName = "quantity", displayLength = 6, uiColumnLength = 8)
    @OAColumn(name = "Quantity", sqlType = java.sql.Types.INTEGER)
    public int getQuantity() {
        return quantity;
    }
    public void setQuantity(int newValue) {
        int old = quantity;
        fireBeforePropertyChange(P_Quantity, old, newValue);
        this.quantity = newValue;
        firePropertyChange(P_Quantity, old, this.quantity);
    }
    @OACalculatedProperty(displayName = "Total Amount", decimalPlaces = 2, isCurrency = true, displayLength = 9, columnLength = 12, properties = {P_Quantity, P_DenominationBundle})
    public double getTotalAmount() {
        return LedgerDenominationBundleDelegate.getTotalAmount(this);
    }
    @OACalculatedProperty(displayLength = 15, properties = {P_StoreSafeLedgerEntry+"."+StoreSafeLedgerEntry.P_Posted, P_TillLedgerEntry+"."+TillLedgerEntry.P_Posted})
    public OADateTime getPosted() {
        return LedgerDenominationBundleDelegate.getPosted(this);
    }
    @OACalculatedProperty(displayName = "Calc Enabled", displayLength = 5, columnLength = 12, properties = {P_Posted})
    public boolean getCalcEnabled() {
        return LedgerDenominationBundleDelegate.getCalcEnabled(this);
    }
    public boolean isCalcEnabled() {
        return getCalcEnabled();
    }

    @OAOne(
        displayName = "Store", 
        isCalculated = true, 
        calcDependentProperties = {P_StoreSafeLedgerEntry, P_TillLedgerEntry}, 
        reverseName = Store.P_CalcLedgerDenominationBundles, 
        allowCreateNew = false, 
        allowAddExisting = false
    )
    public Store getCalcStore() {
        // Custom code here to get CalcStore
        return LedgerDenominationBundleDelegate.getCalcStore(this);
    }

    @OAOne(
        displayName = "Denomination Bundle", 
        reverseName = DenominationBundle.P_LedgerDenominationBundles, 
        allowCreateNew = false, 
        selectFromPath = P_CalcStore + "." + Store.P_CurrencyType + "." + CurrencyType.P_CurrencyDenominations + "." + CurrencyDenomination.P_DenominationBundles, 
        fkeys = {@OAFkey(fromProperty = P_DenominationBundleId, toProperty = DenominationBundle.P_Id)}
    )
    public DenominationBundle getDenominationBundle() {
        if (denominationBundle == null) {
            denominationBundle = (DenominationBundle) getObject(P_DenominationBundle);
        }
        return denominationBundle;
    }
    public void setDenominationBundle(DenominationBundle newValue) {
        DenominationBundle old = this.denominationBundle;
        fireBeforePropertyChange(P_DenominationBundle, old, newValue);
        this.denominationBundle = newValue;
        firePropertyChange(P_DenominationBundle, old, this.denominationBundle);
    }
    @OAProperty(isFkeyOnly = true)
    @OAColumn(name = "DenominationBundleId")
    public Integer getDenominationBundleId() {
        return (Integer) getFkeyProperty(P_DenominationBundleId);
    }
    public void setDenominationBundleId(Integer newValue) {
        this.denominationBundle = null;
        setFkeyProperty(P_DenominationBundleId, newValue);
    }

    @OAOne(
        displayName = "Store Safe Ledger Entry", 
        reverseName = StoreSafeLedgerEntry.P_LedgerDenominationBundles, 
        allowCreateNew = false, 
        allowAddExisting = false, 
        isOneAndOnlyOne = true, 
        fkeys = {@OAFkey(fromProperty = P_StoreSafeLedgerEntryId, toProperty = StoreSafeLedgerEntry.P_Id)}
    )
    public StoreSafeLedgerEntry getStoreSafeLedgerEntry() {
        if (storeSafeLedgerEntry == null) {
            storeSafeLedgerEntry = (StoreSafeLedgerEntry) getObject(P_StoreSafeLedgerEntry);
        }
        return storeSafeLedgerEntry;
    }
    public void setStoreSafeLedgerEntry(StoreSafeLedgerEntry newValue) {
        StoreSafeLedgerEntry old = this.storeSafeLedgerEntry;
        fireBeforePropertyChange(P_StoreSafeLedgerEntry, old, newValue);
        this.storeSafeLedgerEntry = newValue;
        firePropertyChange(P_StoreSafeLedgerEntry, old, this.storeSafeLedgerEntry);
    }
    @OAProperty(isFkeyOnly = true)
    @OAColumn(name = "StoreSafeLedgerEntryId")
    public Integer getStoreSafeLedgerEntryId() {
        return (Integer) getFkeyProperty(P_StoreSafeLedgerEntryId);
    }
    public void setStoreSafeLedgerEntryId(Integer newValue) {
        this.storeSafeLedgerEntry = null;
        setFkeyProperty(P_StoreSafeLedgerEntryId, newValue);
    }

    @OAOne(
        displayName = "Till Ledger Entry", 
        reverseName = TillLedgerEntry.P_LedgerDenominationBundles, 
        allowCreateNew = false, 
        allowAddExisting = false, 
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
    public void load(ResultSet rs, int id) throws SQLException {
        this.id = id;
        java.sql.Timestamp timestamp;
        timestamp = rs.getTimestamp(2);
        if (timestamp != null) this.created = new OADateTime(timestamp);
        this.quantity = rs.getInt(3);
        setPrimitiveNull(P_Quantity, rs.wasNull());
        int denominationBundleFkey = rs.getInt(4);
        setFkeyProperty(P_DenominationBundle, rs.wasNull() ? null : denominationBundleFkey);
        int storeSafeLedgerEntryFkey = rs.getInt(5);
        setFkeyProperty(P_StoreSafeLedgerEntry, rs.wasNull() ? null : storeSafeLedgerEntryFkey);
        int tillLedgerEntryFkey = rs.getInt(6);
        setFkeyProperty(P_TillLedgerEntry, rs.wasNull() ? null : tillLedgerEntryFkey);

        this.changedFlag = false;
        this.newFlag = false;
    }
}
 
