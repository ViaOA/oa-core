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
    lowerName = "store",
    pluralName = "Stores",
    shortName = "str",
    displayName = "Store",
    displayProperty = "storeNumber",
    sortProperty = "storeNumber",
    singleton = true,
    pojoSingleton = true,
    noPojo = true
)
@OATable(
    indexes = {
        @OAIndex(name = "StoreStoreNumber", unique = true, columns = {@OAIndexColumn(name = "StoreNumber")}),
        @OAIndex(name = "StoreAddress", fkey = true, columns = { @OAIndexColumn(name = "AddressId") }), 
        @OAIndex(name = "StoreCurrencyType", fkey = true, columns = { @OAIndexColumn(name = "CurrencyTypeId") })
    }
)
public class Store extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(Store.class.getName());

    public static final String P_Id = "id";
    public static final String P_Created = "created";
    public static final String P_StoreNumber = "storeNumber";
    public static final String P_Name = "name";
     
    public static final String P_Address = "address";
    public static final String P_AddressId = "addressId"; // fkey
    public static final String P_CalcLedgerDenominationBundles = "calcLedgerDenominationBundles";
    public static final String P_CalcStoreSafeLedgerEntries = "calcStoreSafeLedgerEntries";
    public static final String P_CurrencyType = "currencyType";
    public static final String P_CurrencyTypeId = "currencyTypeId"; // fkey
    public static final String P_ManualPurchaseOrders = "manualPurchaseOrders";
    public static final String P_Registers = "registers";
    public static final String P_StoreClosedDates = "storeClosedDates";
    public static final String P_StoreHoursOpens = "storeHoursOpens";
    public static final String P_StoreSafe = "storeSafe";
    public static final String P_StoreSchedules = "storeSchedules";
    public static final String P_StoreToStoreTransfers = "storeToStoreTransfers";
    public static final String P_TeamMembers = "teamMembers";
    public static final String P_Tills = "tills";
     
    protected volatile int id;
    protected volatile OADateTime created;
    protected volatile int storeNumber;
    protected volatile String name;
     
    // Links to other objects.
    protected volatile transient Address address;
    protected volatile transient CurrencyType currencyType;
    protected transient Hub<ManualPurchaseOrder> hubManualPurchaseOrders;
    protected transient Hub<Register> hubRegisters;
    protected transient Hub<StoreClosedDate> hubStoreClosedDates;
    protected transient Hub<StoreHoursOpen> hubStoreHoursOpens;
    protected volatile transient StoreSafe storeSafe;
    protected transient Hub<StoreSchedule> hubStoreSchedules;
    protected transient Hub<TeamMember> hubTeamMembers;
    protected transient Hub<Till> hubTills;
     
    public Store() {
        if (!isLoading()) setObjectDefaults();
    }
    @Override
    public void setObjectDefaults() {
        setCreated(new OADateTime());
        getAddress(); // have it autoCreated
        getStoreSafe(); // have it autoCreated
    }
     
    public Store(int id) {
        this();
        setId(id);
    }
    @OAObjCallback(contextEnabledProperty = AppUser.P_Admin)
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

    @OAProperty(lowerName = "storeNumber", displayName = "Store Number", isUnique = true, displayLength = 6, uiColumnLength = 12)
    @OAColumn(name = "StoreNumber", sqlType = java.sql.Types.INTEGER)
    public int getStoreNumber() {
        return storeNumber;
    }
    public void setStoreNumber(int newValue) {
        int old = storeNumber;
        fireBeforePropertyChange(P_StoreNumber, old, newValue);
        this.storeNumber = newValue;
        firePropertyChange(P_StoreNumber, old, this.storeNumber);
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

    @OAOne(
        reverseName = Address.P_Store, 
        autoCreateNew = true, 
        allowAddExisting = false, 
        fkeys = {@OAFkey(fromProperty = P_AddressId, toProperty = Address.P_Id)}
    )
    public Address getAddress() {
        if (address == null) {
            address = (Address) getObject(P_Address);
        }
        return address;
    }
    public void setAddress(Address newValue) {
        Address old = this.address;
        fireBeforePropertyChange(P_Address, old, newValue);
        this.address = newValue;
        firePropertyChange(P_Address, old, this.address);
    }
    @OAProperty(isFkeyOnly = true)
    @OAColumn(name = "AddressId")
    public Integer getAddressId() {
        return (Integer) getFkeyProperty(P_AddressId);
    }
    public void setAddressId(Integer newValue) {
        this.address = null;
        setFkeyProperty(P_AddressId, newValue);
    }

    @OAMany(
        displayName = "Ledger Denomination Bundles", 
        toClass = LedgerDenominationBundle.class, 
        isCalculated = true, 
        reverseName = LedgerDenominationBundle.P_CalcStore, 
        createMethod = false
    )
    private Hub<LedgerDenominationBundle> getCalcLedgerDenominationBundles() {
        // oamodel has createMethod set to false, this method exists only for annotations.
        return null;
    }

    @OAMany(
        displayName = "Store Safe Ledger Entries", 
        toClass = StoreSafeLedgerEntry.class, 
        isCalculated = true, 
        reverseName = StoreSafeLedgerEntry.P_CalcStore, 
        createMethod = false
    )
    private Hub<StoreSafeLedgerEntry> getCalcStoreSafeLedgerEntries() {
        // oamodel has createMethod set to false, this method exists only for annotations.
        return null;
    }

    @OAOne(
        displayName = "Currency Type", 
        reverseName = CurrencyType.P_Stores, 
        allowCreateNew = false, 
        fkeys = {@OAFkey(fromProperty = P_CurrencyTypeId, toProperty = CurrencyType.P_Id)}
    )
    public CurrencyType getCurrencyType() {
        if (currencyType == null) {
            currencyType = (CurrencyType) getObject(P_CurrencyType);
        }
        return currencyType;
    }
    public void setCurrencyType(CurrencyType newValue) {
        CurrencyType old = this.currencyType;
        fireBeforePropertyChange(P_CurrencyType, old, newValue);
        this.currencyType = newValue;
        firePropertyChange(P_CurrencyType, old, this.currencyType);
    }
    @OAProperty(isFkeyOnly = true)
    @OAColumn(name = "CurrencyTypeId")
    public Integer getCurrencyTypeId() {
        return (Integer) getFkeyProperty(P_CurrencyTypeId);
    }
    public void setCurrencyTypeId(Integer newValue) {
        this.currencyType = null;
        setFkeyProperty(P_CurrencyTypeId, newValue);
    }

    @OAMany(
        displayName = "Manual Purchase Orders", 
        toClass = ManualPurchaseOrder.class, 
        owner = true, 
        reverseName = ManualPurchaseOrder.P_Store, 
        cascadeSave = true, 
        cascadeDelete = true
    )
    public Hub<ManualPurchaseOrder> getManualPurchaseOrders() {
        if (hubManualPurchaseOrders == null) {
            hubManualPurchaseOrders = (Hub<ManualPurchaseOrder>) getHub(P_ManualPurchaseOrders);
        }
        return hubManualPurchaseOrders;
    }

    @OAMany(
        toClass = Register.class, 
        owner = true, 
        reverseName = Register.P_Store, 
        cascadeSave = true, 
        cascadeDelete = true
    )
    public Hub<Register> getRegisters() {
        if (hubRegisters == null) {
            hubRegisters = (Hub<Register>) getHub(P_Registers);
        }
        return hubRegisters;
    }

    @OAMany(
        displayName = "Store Closed Dates", 
        toClass = StoreClosedDate.class, 
        owner = true, 
        reverseName = StoreClosedDate.P_Store, 
        cascadeSave = true, 
        cascadeDelete = true
    )
    public Hub<StoreClosedDate> getStoreClosedDates() {
        if (hubStoreClosedDates == null) {
            hubStoreClosedDates = (Hub<StoreClosedDate>) getHub(P_StoreClosedDates);
        }
        return hubStoreClosedDates;
    }

    @OAMany(
        displayName = "Store Hours Opens", 
        toClass = StoreHoursOpen.class, 
        owner = true, 
        reverseName = StoreHoursOpen.P_Store, 
        cascadeSave = true, 
        cascadeDelete = true, 
        autoCreateProperty = StoreHoursOpen.P_DayOfWeek
    )
    public Hub<StoreHoursOpen> getStoreHoursOpens() {
        if (hubStoreHoursOpens == null) {
            hubStoreHoursOpens = (Hub<StoreHoursOpen>) getHub(P_StoreHoursOpens);
        }
        return hubStoreHoursOpens;
    }

    @OAOne(
        displayName = "Store Safe", 
        owner = true, 
        reverseName = StoreSafe.P_Store, 
        cascadeSave = true, 
        cascadeDelete = true, 
        autoCreateNew = true, 
        allowAddExisting = false
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

    @OAMany(
        displayName = "Store Schedules", 
        toClass = StoreSchedule.class, 
        reverseName = StoreSchedule.P_Store
    )
    public Hub<StoreSchedule> getStoreSchedules() {
        if (hubStoreSchedules == null) {
            hubStoreSchedules = (Hub<StoreSchedule>) getHub(P_StoreSchedules);
        }
        return hubStoreSchedules;
    }

    @OAMany(
        displayName = "Store To Store Transfers", 
        toClass = StoreToStoreTransfer.class, 
        reverseName = StoreToStoreTransfer.P_ToStore, 
        createMethod = false
    )
    private Hub<StoreToStoreTransfer> getStoreToStoreTransfers() {
        // oamodel has createMethod set to false, this method exists only for annotations.
        return null;
    }

    @OAMany(
        displayName = "Team Members", 
        toClass = TeamMember.class, 
        owner = true, 
        reverseName = TeamMember.P_Store, 
        cascadeSave = true, 
        cascadeDelete = true
    )
    public Hub<TeamMember> getTeamMembers() {
        if (hubTeamMembers == null) {
            hubTeamMembers = (Hub<TeamMember>) getHub(P_TeamMembers);
        }
        return hubTeamMembers;
    }

    @OAMany(
        toClass = Till.class, 
        owner = true, 
        reverseName = Till.P_Store, 
        cascadeSave = true, 
        cascadeDelete = true
    )
    public Hub<Till> getTills() {
        if (hubTills == null) {
            hubTills = (Hub<Till>) getHub(P_Tills);
        }
        return hubTills;
    }
    public void load(ResultSet rs, int id) throws SQLException {
        this.id = id;
        java.sql.Timestamp timestamp;
        timestamp = rs.getTimestamp(2);
        if (timestamp != null) this.created = new OADateTime(timestamp);
        this.storeNumber = rs.getInt(3);
        setPrimitiveNull(P_StoreNumber, rs.wasNull());
        this.name = rs.getString(4);
        int addressFkey = rs.getInt(5);
        setFkeyProperty(P_Address, rs.wasNull() ? null : addressFkey);
        int currencyTypeFkey = rs.getInt(6);
        setFkeyProperty(P_CurrencyType, rs.wasNull() ? null : currencyTypeFkey);

        this.changedFlag = false;
        this.newFlag = false;
    }
}
 
