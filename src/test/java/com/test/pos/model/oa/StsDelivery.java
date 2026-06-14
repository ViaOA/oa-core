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
    lowerName = "stsDelivery",
    pluralName = "StsDeliveries",
    shortName = "std",
    displayName = "Sts Delivery",
    displayProperty = "id",
    noPojo = true
)
@OATable(
    indexes = {
        @OAIndex(name = "StsDeliveryDeliveryService", fkey = true, columns = { @OAIndexColumn(name = "DeliveryServiceId") }), 
        @OAIndex(name = "StsDeliveryStoreToStoreTransfer", fkey = true, columns = { @OAIndexColumn(name = "StoreToStoreTransferId") })
    }
)
public class StsDelivery extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(StsDelivery.class.getName());

    public static final String P_Id = "id";
    public static final String P_Created = "created";
     
    public static final String P_DeliveryService = "deliveryService";
    public static final String P_DeliveryServiceId = "deliveryServiceId"; // fkey
    public static final String P_StoreToStoreTransfer = "storeToStoreTransfer";
    public static final String P_StoreToStoreTransferId = "storeToStoreTransferId"; // fkey
    public static final String P_StsdItems = "stsdItems";
     
    protected volatile int id;
    protected volatile OADateTime created;
     
    // Links to other objects.
    protected volatile transient DeliveryService deliveryService;
    protected volatile transient StoreToStoreTransfer storeToStoreTransfer;
    protected transient Hub<StsdItem> hubStsdItems;
     
    public StsDelivery() {
        if (!isLoading()) setObjectDefaults();
    }
    @Override
    public void setObjectDefaults() {
        setCreated(new OADateTime());
    }
     
    public StsDelivery(int id) {
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

    @OAOne(
        displayName = "Delivery Service", 
        reverseName = DeliveryService.P_StsDeliveries, 
        allowCreateNew = false, 
        isOneAndOnlyOne = true, 
        fkeys = {@OAFkey(fromProperty = P_DeliveryServiceId, toProperty = DeliveryService.P_Id)}
    )
    public DeliveryService getDeliveryService() {
        if (deliveryService == null) {
            deliveryService = (DeliveryService) getObject(P_DeliveryService);
        }
        return deliveryService;
    }
    public void setDeliveryService(DeliveryService newValue) {
        DeliveryService old = this.deliveryService;
        fireBeforePropertyChange(P_DeliveryService, old, newValue);
        this.deliveryService = newValue;
        firePropertyChange(P_DeliveryService, old, this.deliveryService);
    }
    @OAProperty(isFkeyOnly = true)
    @OAColumn(name = "DeliveryServiceId")
    public Integer getDeliveryServiceId() {
        return (Integer) getFkeyProperty(P_DeliveryServiceId);
    }
    public void setDeliveryServiceId(Integer newValue) {
        this.deliveryService = null;
        setFkeyProperty(P_DeliveryServiceId, newValue);
    }

    @OAOne(
        displayName = "Store To Store Transfer", 
        reverseName = StoreToStoreTransfer.P_StsDeliveries, 
        required = true, 
        allowCreateNew = false, 
        fkeys = {@OAFkey(fromProperty = P_StoreToStoreTransferId, toProperty = StoreToStoreTransfer.P_Id)}
    )
    public StoreToStoreTransfer getStoreToStoreTransfer() {
        if (storeToStoreTransfer == null) {
            storeToStoreTransfer = (StoreToStoreTransfer) getObject(P_StoreToStoreTransfer);
        }
        return storeToStoreTransfer;
    }
    public void setStoreToStoreTransfer(StoreToStoreTransfer newValue) {
        StoreToStoreTransfer old = this.storeToStoreTransfer;
        fireBeforePropertyChange(P_StoreToStoreTransfer, old, newValue);
        this.storeToStoreTransfer = newValue;
        firePropertyChange(P_StoreToStoreTransfer, old, this.storeToStoreTransfer);
    }
    @OAProperty(isFkeyOnly = true)
    @OAColumn(name = "StoreToStoreTransferId")
    public Integer getStoreToStoreTransferId() {
        return (Integer) getFkeyProperty(P_StoreToStoreTransferId);
    }
    public void setStoreToStoreTransferId(Integer newValue) {
        this.storeToStoreTransfer = null;
        setFkeyProperty(P_StoreToStoreTransferId, newValue);
    }

    @OAMany(
        displayName = "Stsd Items", 
        toClass = StsdItem.class, 
        owner = true, 
        reverseName = StsdItem.P_StsDelivery, 
        cascadeSave = true, 
        cascadeDelete = true
    )
    public Hub<StsdItem> getStsdItems() {
        if (hubStsdItems == null) {
            hubStsdItems = (Hub<StsdItem>) getHub(P_StsdItems);
        }
        return hubStsdItems;
    }
    public void load(ResultSet rs, int id) throws SQLException {
        this.id = id;
        java.sql.Timestamp timestamp;
        timestamp = rs.getTimestamp(2);
        if (timestamp != null) this.created = new OADateTime(timestamp);
        int deliveryServiceFkey = rs.getInt(3);
        setFkeyProperty(P_DeliveryService, rs.wasNull() ? null : deliveryServiceFkey);
        int storeToStoreTransferFkey = rs.getInt(4);
        setFkeyProperty(P_StoreToStoreTransfer, rs.wasNull() ? null : storeToStoreTransferFkey);

        this.changedFlag = false;
        this.newFlag = false;
    }
}
 
