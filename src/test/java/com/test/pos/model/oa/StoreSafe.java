package com.test.pos.model.oa;
 
import java.util.*;
import java.util.logging.*;
import java.sql.*;
import com.viaoa.converter.*;
import com.viaoa.object.*;
import com.viaoa.hub.*;
import com.viaoa.annotation.*;
import com.viaoa.lang.*;
import com.viaoa.math.OAMath;
import com.viaoa.callback.OAObjectCallback;
import com.viaoa.datetime.OADateTime;
import com.test.pos.delegate.oa.*;
import com.test.pos.model.oa.filter.*;
import com.test.pos.model.oa.propertypath.*;
 
@OAClass(
    lowerName = "storeSafe",
    pluralName = "StoreSafes",
    shortName = "sts",
    displayName = "Store Safe",
    displayProperty = "name",
    noPojo = true
)
@OATable(
    indexes = {
        @OAIndex(name = "StoreSafeStore", fkey = true, columns = { @OAIndexColumn(name = "StoreId") })
    }
)
public class StoreSafe extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(StoreSafe.class.getName());

    public static final String P_Id = "id";
    public static final String P_Created = "created";
    public static final String P_Name = "name";
    public static final String P_CashAmount = "cashAmount";
    public static final String P_PettyCashAmount = "pettyCashAmount";
    public static final String P_AllowDirectChanges = "allowDirectChanges";
     
    public static final String P_CheckCount = "checkCount";
    public static final String P_TotalCheckAmount = "totalCheckAmount";
     
    public static final String P_BankDeposits = "bankDeposits";
    public static final String P_InvoicePaymentChecks = "invoicePaymentChecks";
    public static final String P_Store = "store";
    public static final String P_StoreId = "storeId"; // fkey
    public static final String P_StoreSafeLedgerEntries = "storeSafeLedgerEntries";
     
    protected volatile int id;
    protected volatile OADateTime created;
    protected volatile String name;
    protected volatile double cashAmount;
    protected volatile double pettyCashAmount;
    protected volatile boolean allowDirectChanges;
     
    // Links to other objects.
    protected transient Hub<BankDeposit> hubBankDeposits;
    protected transient Hub<InvoicePaymentCheck> hubInvoicePaymentChecks;
    protected volatile transient Store store;
    protected transient Hub<StoreSafeLedgerEntry> hubStoreSafeLedgerEntries;
     
    public StoreSafe() {
        if (!isLoading()) setObjectDefaults();
    }
    @Override
    public void setObjectDefaults() {
        setCreated(new OADateTime());
    }
     
    public StoreSafe(int id) {
        this();
        setId(id);
    }
    @OAObjCallback(modelUserEnabledProperty = AppUser.P_TeamMember+"."+TeamMember.P_AccessSafePermission
    )
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

    @OAProperty(lowerName = "name", maxLength = 25, displayLength = 12)
    @OAColumn(name = "Name", maxLength = 25)
    public String getName() {
        return name;
    }
    public void setName(String newValue) {
        String old = name;
        fireBeforePropertyChange(P_Name, old, newValue);
        this.name = newValue;
        firePropertyChange(P_Name, old, this.name);
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
     
    @OAObjCallback(enabledProperty = StoreSafe.P_AllowDirectChanges, modelUserEnabledProperty = AppUser.P_TeamMember+"."+TeamMember.P_AccessSafePermission)
    public void cashAmountCallback(OAObjectCallback callback) {
        if (callback == null) return;
        switch (callback.getType()) {
        }
    }

    @OAProperty(lowerName = "pettyCashAmount", displayName = "Petty Cash Amount", decimalPlaces = 2, isCurrency = true, displayLength = 9, uiColumnLength = 17, isProcessed = true)
    @OAColumn(name = "PettyCashAmount", sqlType = java.sql.Types.NUMERIC)
    public double getPettyCashAmount() {
        return pettyCashAmount;
    }
    public void setPettyCashAmount(double newValue) {
        double old = pettyCashAmount;
        fireBeforePropertyChange(P_PettyCashAmount, old, newValue);
        this.pettyCashAmount = newValue;
        firePropertyChange(P_PettyCashAmount, old, this.pettyCashAmount);
    }
     
    @OAObjCallback(enabledProperty = StoreSafe.P_AllowDirectChanges, modelUserEnabledProperty = AppUser.P_TeamMember+"."+TeamMember.P_AccessSafePermission)
    public void pettyCashAmountCallback(OAObjectCallback callback) {
        if (callback == null) return;
        switch (callback.getType()) {
        }
    }

    @OAProperty(lowerName = "allowDirectChanges", displayName = "Allow Direct Changes", displayLength = 5, uiColumnLength = 20)
    @OAColumn(name = "AllowDirectChanges", sqlType = java.sql.Types.BOOLEAN)
    public boolean getAllowDirectChanges() {
        return allowDirectChanges;
    }
    public boolean isAllowDirectChanges() {
        return getAllowDirectChanges();
    }
    public void setAllowDirectChanges(boolean newValue) {
        boolean old = allowDirectChanges;
        fireBeforePropertyChange(P_AllowDirectChanges, old, newValue);
        this.allowDirectChanges = newValue;
        firePropertyChange(P_AllowDirectChanges, old, this.allowDirectChanges);
    }
     
    @OAObjCallback(modelUserEnabledProperty = AppUser.P_TeamMember+"."+TeamMember.P_AccessSafePermission, 
        modelUserVisibleProperty = AppUser.P_TeamMember+"."+TeamMember.P_AccessSafePermission
    )
    public void allowDirectChangesCallback(OAObjectCallback callback) {
        if (callback == null) return;
        switch (callback.getType()) {
        }
    }
    @OACalculatedProperty(displayName = "Check Count", displayLength = 6, columnLength = 11, properties = {P_InvoicePaymentChecks})
    public int getCheckCount() {
        return this.getInvoicePaymentChecks().size();
    }
    @OACalculatedProperty(displayName = "Total Check Amount", decimalPlaces = 2, isCurrency = true, displayLength = 9, columnLength = 18, properties = {P_InvoicePaymentChecks+"."+InvoicePaymentCheck.P_InvoicePayment+"."+InvoicePayment.P_Amount})
    public double getTotalCheckAmount() {
        double d = 0;
        for (InvoicePaymentCheck ipc : getInvoicePaymentChecks()) {
            InvoicePayment ip = ipc.getInvoicePayment();
            if (ip != null) {
                d = OAMath.add(d, ip.getAmount(), 2);
            }
        }
        return d;
    }

    @OAMany(
        displayName = "Bank Deposits", 
        toClass = BankDeposit.class, 
        owner = true, 
        reverseName = BankDeposit.P_StoreSafe, 
        cascadeSave = true, 
        cascadeDelete = true
    )
    public Hub<BankDeposit> getBankDeposits() {
        if (hubBankDeposits == null) {
            hubBankDeposits = (Hub<BankDeposit>) getHub(P_BankDeposits);
        }
        return hubBankDeposits;
    }
    @OAObjCallback(enabledProperty = StoreSafe.P_AllowDirectChanges, modelUserEnabledProperty = AppUser.P_TeamMember+"."+TeamMember.P_AccessSafePermission, 
    		modelUserVisibleProperty = AppUser.P_TeamMember+"."+TeamMember.P_AccessSafePermission
    )
    public void bankDepositsCallback(OAObjectCallback cb) {
        if (cb == null) return;
        switch (cb.getType()) {
        }
    }

    @OAMany(
        displayName = "Invoice Payment Checks", 
        toClass = InvoicePaymentCheck.class, 
        reverseName = InvoicePaymentCheck.P_StoreSafe, 
        isProcessed = true
    )
    public Hub<InvoicePaymentCheck> getInvoicePaymentChecks() {
        if (hubInvoicePaymentChecks == null) {
            hubInvoicePaymentChecks = (Hub<InvoicePaymentCheck>) getHub(P_InvoicePaymentChecks);
        }
        return hubInvoicePaymentChecks;
    }

    @OAOne(
        reverseName = Store.P_StoreSafe, 
        required = true, 
        allowCreateNew = false, 
        allowAddExisting = false, 
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

    @OAMany(
        displayName = "Store Safe Ledger Entries", 
        toClass = StoreSafeLedgerEntry.class, 
        owner = true, 
        reverseName = StoreSafeLedgerEntry.P_StoreSafe, 
        cascadeSave = true, 
        cascadeDelete = true
    )
    public Hub<StoreSafeLedgerEntry> getStoreSafeLedgerEntries() {
        if (hubStoreSafeLedgerEntries == null) {
            hubStoreSafeLedgerEntries = (Hub<StoreSafeLedgerEntry>) getHub(P_StoreSafeLedgerEntries);
        }
        return hubStoreSafeLedgerEntries;
    }
    public void load(ResultSet rs, int id) throws SQLException {
        this.id = id;
        java.sql.Timestamp timestamp;
        timestamp = rs.getTimestamp(2);
        if (timestamp != null) this.created = new OADateTime(timestamp);
        this.name = rs.getString(3);
        this.cashAmount = rs.getDouble(4);
        setPrimitiveNull(P_CashAmount, rs.wasNull());
        this.pettyCashAmount = rs.getDouble(5);
        setPrimitiveNull(P_PettyCashAmount, rs.wasNull());
        this.allowDirectChanges = rs.getBoolean(6);
        setPrimitiveNull(P_AllowDirectChanges, rs.wasNull());
        int storeFkey = rs.getInt(7);
        setFkeyProperty(P_Store, rs.wasNull() ? null : storeFkey);

        this.changedFlag = false;
        this.newFlag = false;
    }
}
 
