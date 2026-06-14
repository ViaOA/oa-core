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
    lowerName = "manualPurchaseOrder",
    pluralName = "ManualPurchaseOrders",
    shortName = "mpo",
    displayName = "Manual Purchase Order",
    displayProperty = "id",
    noPojo = true
)
@OATable(
    indexes = {
        @OAIndex(name = "ManualPurchaseOrderStore", fkey = true, columns = { @OAIndexColumn(name = "StoreId") }), 
        @OAIndex(name = "ManualPurchaseOrderStoreSafeLedgerEntry", fkey = true, columns = { @OAIndexColumn(name = "StoreSafeLedgerEntryId") })
    }
)
public class ManualPurchaseOrder extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(ManualPurchaseOrder.class.getName());

    public static final String P_Id = "id";
    public static final String P_Created = "created";
    public static final String P_CashAmount = "cashAmount";
    public static final String P_Note = "note";
    public static final String P_Applied = "applied";
     
    public static final String P_Store = "store";
    public static final String P_StoreId = "storeId"; // fkey
    public static final String P_StoreSafeLedgerEntry = "storeSafeLedgerEntry";
    public static final String P_StoreSafeLedgerEntryId = "storeSafeLedgerEntryId"; // fkey
     
    public static final String M_Apply = "apply";
    protected volatile int id;
    protected volatile OADateTime created;
    protected volatile double cashAmount;
    protected volatile String note;
    protected volatile OADateTime applied;
     
    // Links to other objects.
    protected volatile transient Store store;
    protected volatile transient StoreSafeLedgerEntry storeSafeLedgerEntry;
     
    public ManualPurchaseOrder() {
        if (!isLoading()) setObjectDefaults();
    }
    @Override
    public void setObjectDefaults() {
        setCreated(new OADateTime());
    }
     
    public ManualPurchaseOrder(int id) {
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

    @OAProperty(lowerName = "cashAmount", displayName = "Cash Amount", decimalPlaces = 2, isCurrency = true, displayLength = 9, uiColumnLength = 11, isProcessed = true)
    @OAColumn(name = "CashAmount", sqlType = java.sql.Types.NUMERIC)
    public double getCashAmount() {
        return cashAmount;
    }
    public void setCashAmount(double newValue) {
        double old = cashAmount;
        fireBeforePropertyChange(P_CashAmount, old, newValue);
        this.cashAmount = newValue;
        firePropertyChange(P_CashAmount, old, this.cashAmount);
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

    @OAProperty(lowerName = "applied", displayLength = 15, ignoreTimeZone = true)
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
        reverseName = Store.P_ManualPurchaseOrders, 
        required = true, 
        allowCreateNew = false, 
        fkeys = {@OAFkey(fromProperty = P_StoreId, toProperty = Store.P_Id)}
    )
    public Store getStore() {
        if (store == null) {
            store = (Store) getObject(P_Store);
        }
        return store;
    }
    public void setStore(Store newValue) {
        Store old = this.store;
        fireBeforePropertyChange(P_Store, old, newValue);
        this.store = newValue;
        firePropertyChange(P_Store, old, this.store);
    }
    @OAProperty(isFkeyOnly = true)
    @OAColumn(name = "StoreId")
    public Integer getStoreId() {
        return (Integer) getFkeyProperty(P_StoreId);
    }
    public void setStoreId(Integer newValue) {
        this.store = null;
        setFkeyProperty(P_StoreId, newValue);
    }

    @OAOne(
        displayName = "Store Safe Ledger Entry", 
        reverseName = StoreSafeLedgerEntry.P_ManualPurchaseOrder, 
        allowCreateNew = false, 
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
    @OAMethod(displayName = "Apply")
    public void apply() throws Exception {
        // custom code
        ManualPurchaseOrderDelegate.apply(this);
    }

    public void load(ResultSet rs, int id) throws SQLException {
        this.id = id;
        java.sql.Timestamp timestamp;
        timestamp = rs.getTimestamp(2);
        if (timestamp != null) this.created = new OADateTime(timestamp);
        this.cashAmount = rs.getDouble(3);
        setPrimitiveNull(P_CashAmount, rs.wasNull());
        this.note = rs.getString(4);
        timestamp = rs.getTimestamp(5);
        if (timestamp != null) this.applied = new OADateTime(timestamp);
        int storeFkey = rs.getInt(6);
        setFkeyProperty(P_Store, rs.wasNull() ? null : storeFkey);
        int storeSafeLedgerEntryFkey = rs.getInt(7);
        setFkeyProperty(P_StoreSafeLedgerEntry, rs.wasNull() ? null : storeSafeLedgerEntryFkey);

        this.changedFlag = false;
        this.newFlag = false;
    }
}
 
