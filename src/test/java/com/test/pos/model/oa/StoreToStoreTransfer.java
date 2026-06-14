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
    lowerName = "storeToStoreTransfer",
    pluralName = "StoreToStoreTransfers",
    shortName = "sts",
    displayName = "Store To Store Transfer",
    displayProperty = "id",
    noPojo = true
)
@OATable(
    indexes = {
        @OAIndex(name = "StoreToStoreTransferToStore", fkey = true, columns = { @OAIndexColumn(name = "ToStoreId") })
    }
)
public class StoreToStoreTransfer extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(StoreToStoreTransfer.class.getName());

    public static final String P_Id = "id";
    public static final String P_Created = "created";
     
    public static final String P_StsDeliveries = "stsDeliveries";
    public static final String P_StsItems = "stsItems";
    public static final String P_ToStore = "toStore";
    public static final String P_ToStoreId = "toStoreId"; // fkey
     
    protected volatile int id;
    protected volatile OADateTime created;
     
    // Links to other objects.
    protected transient Hub<StsDelivery> hubStsDeliveries;
    protected transient Hub<StsItem> hubStsItems;
    protected volatile transient Store toStore;
     
    public StoreToStoreTransfer() {
        if (!isLoading()) setObjectDefaults();
    }
    @Override
    public void setObjectDefaults() {
        setCreated(new OADateTime());
    }
     
    public StoreToStoreTransfer(int id) {
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
        displayName = "Sts Deliveries", 
        toClass = StsDelivery.class, 
        owner = true, 
        reverseName = StsDelivery.P_StoreToStoreTransfer, 
        cascadeSave = true, 
        cascadeDelete = true
    )
    public Hub<StsDelivery> getStsDeliveries() {
        if (hubStsDeliveries == null) {
            hubStsDeliveries = (Hub<StsDelivery>) getHub(P_StsDeliveries);
        }
        return hubStsDeliveries;
    }

    @OAMany(
        displayName = "Sts Items", 
        toClass = StsItem.class, 
        owner = true, 
        reverseName = StsItem.P_StoreToStoreTransfer, 
        cascadeSave = true, 
        cascadeDelete = true
    )
    public Hub<StsItem> getStsItems() {
        if (hubStsItems == null) {
            hubStsItems = (Hub<StsItem>) getHub(P_StsItems);
        }
        return hubStsItems;
    }

    @OAOne(
        displayName = "To Store", 
        reverseName = Store.P_StoreToStoreTransfers, 
        allowCreateNew = false, 
        fkeys = {@OAFkey(fromProperty = P_ToStoreId, toProperty = Store.P_Id)}
    )
    public Store getToStore() {
        if (toStore == null) {
            toStore = (Store) getObject(P_ToStore);
        }
        return toStore;
    }
    public void setToStore(Store newValue) {
        Store old = this.toStore;
        fireBeforePropertyChange(P_ToStore, old, newValue);
        this.toStore = newValue;
        firePropertyChange(P_ToStore, old, this.toStore);
    }
    @OAProperty(isFkeyOnly = true)
    @OAColumn(name = "ToStoreId")
    public Integer getToStoreId() {
        return (Integer) getFkeyProperty(P_ToStoreId);
    }
    public void setToStoreId(Integer newValue) {
        this.toStore = null;
        setFkeyProperty(P_ToStoreId, newValue);
    }
    public void load(ResultSet rs, int id) throws SQLException {
        this.id = id;
        java.sql.Timestamp timestamp;
        timestamp = rs.getTimestamp(2);
        if (timestamp != null) this.created = new OADateTime(timestamp);
        int toStoreFkey = rs.getInt(3);
        setFkeyProperty(P_ToStore, rs.wasNull() ? null : toStoreFkey);

        this.changedFlag = false;
        this.newFlag = false;
    }
}
 
