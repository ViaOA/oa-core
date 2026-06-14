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
    lowerName = "storeDayOpen",
    pluralName = "StoreDayOpens",
    shortName = "sdo",
    displayName = "Store Day Open",
    displayProperty = "storeSchedule",
    noPojo = true
)
@OATable(
    indexes = {
        @OAIndex(name = "StoreDayOpenStoreSchedule", fkey = true, columns = { @OAIndexColumn(name = "StoreScheduleId") })
    }
)
public class StoreDayOpen extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(StoreDayOpen.class.getName());

    public static final String P_Id = "id";
    public static final String P_Created = "created";
     
    public static final String P_StoreSafeLedgerEntries = "storeSafeLedgerEntries";
    public static final String P_StoreSchedule = "storeSchedule";
    public static final String P_StoreScheduleId = "storeScheduleId"; // fkey
     
    public static final String M_CreateStoreSafeAudit = "createStoreSafeAudit";
    protected volatile int id;
    protected volatile OADateTime created;
     
    // Links to other objects.
    protected transient Hub<StoreSafeLedgerEntry> hubStoreSafeLedgerEntries;
    protected volatile transient StoreSchedule storeSchedule;
     
    public StoreDayOpen() {
        if (!isLoading()) setObjectDefaults();
    }
    @Override
    public void setObjectDefaults() {
        setCreated(new OADateTime());
    }
     
    public StoreDayOpen(int id) {
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

    @OAMany(
        displayName = "Store Safe Ledger Entries", 
        toClass = StoreSafeLedgerEntry.class, 
        reverseName = StoreSafeLedgerEntry.P_StoreDayOpen, 
        isProcessed = true
    )
    public Hub<StoreSafeLedgerEntry> getStoreSafeLedgerEntries() {
        if (hubStoreSafeLedgerEntries == null) {
            hubStoreSafeLedgerEntries = (Hub<StoreSafeLedgerEntry>) getHub(P_StoreSafeLedgerEntries);
        }
        return hubStoreSafeLedgerEntries;
    }

    @OAOne(
        displayName = "Store Schedule", 
        reverseName = StoreSchedule.P_StoreDayOpen, 
        required = true, 
        allowCreateNew = false, 
        allowAddExisting = false, 
        fkeys = {@OAFkey(fromProperty = P_StoreScheduleId, toProperty = StoreSchedule.P_Id)}
    )
    public StoreSchedule getStoreSchedule() {
        if (storeSchedule == null) {
            storeSchedule = (StoreSchedule) getObject(P_StoreSchedule);
        }
        return storeSchedule;
    }
    public void setStoreSchedule(StoreSchedule newValue) {
        StoreSchedule old = this.storeSchedule;
        fireBeforePropertyChange(P_StoreSchedule, old, newValue);
        this.storeSchedule = newValue;
        firePropertyChange(P_StoreSchedule, old, this.storeSchedule);
    }
    @OAProperty(isFkeyOnly = true)
    @OAColumn(name = "StoreScheduleId")
    public Integer getStoreScheduleId() {
        return (Integer) getFkeyProperty(P_StoreScheduleId);
    }
    public void setStoreScheduleId(Integer newValue) {
        this.storeSchedule = null;
        setFkeyProperty(P_StoreScheduleId, newValue);
    }
    @OAMethod(displayName = "Create Store Safe Audit")
    public void createStoreSafeAudit() throws Exception {
        // use this to run on server
        if (isRemoteAvailable()) {
            remote();
            return;
        }
        // custom code
        StoreDayOpenDelegate.createStoreSafeAudit(this);
    }
    @OAObjCallback(enabledProperty = StoreDayOpen.P_StoreSafeLedgerEntries, enabledValue = false
    )
    public void createStoreSafeAuditCallback(OAObjectCallback cb) {
    }

    public void load(ResultSet rs, int id) throws SQLException {
        this.id = id;
        java.sql.Timestamp timestamp;
        timestamp = rs.getTimestamp(2);
        if (timestamp != null) this.created = new OADateTime(timestamp);
        int storeScheduleFkey = rs.getInt(3);
        setFkeyProperty(P_StoreSchedule, rs.wasNull() ? null : storeScheduleFkey);

        this.changedFlag = false;
        this.newFlag = false;
    }
}
 
