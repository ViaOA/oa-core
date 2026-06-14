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
    lowerName = "onlineOrderDelivery",
    pluralName = "OnlineOrderDeliveries",
    shortName = "ood",
    displayName = "Online Order Delivery",
    displayProperty = "id",
    noPojo = true
)
@OATable(
    indexes = {
        @OAIndex(name = "OnlineOrderDeliveryDeliveryService", fkey = true, columns = { @OAIndexColumn(name = "DeliveryServiceId") }), 
        @OAIndex(name = "OnlineOrderDeliveryOnlineOrder", fkey = true, columns = { @OAIndexColumn(name = "OnlineOrderId") })
    }
)
public class OnlineOrderDelivery extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(OnlineOrderDelivery.class.getName());

    public static final String P_Id = "id";
    public static final String P_Created = "created";
     
    public static final String P_DeliveryService = "deliveryService";
    public static final String P_DeliveryServiceId = "deliveryServiceId"; // fkey
    public static final String P_OnlineOrder = "onlineOrder";
    public static final String P_OnlineOrderId = "onlineOrderId"; // fkey
    public static final String P_OodItems = "oodItems";
     
    protected volatile int id;
    protected volatile OADateTime created;
     
    // Links to other objects.
    protected volatile transient DeliveryService deliveryService;
    protected volatile transient OnlineOrder onlineOrder;
    protected transient Hub<OodItem> hubOodItems;
     
    public OnlineOrderDelivery() {
        if (!isLoading()) setObjectDefaults();
    }
    @Override
    public void setObjectDefaults() {
        setCreated(new OADateTime());
    }
     
    public OnlineOrderDelivery(int id) {
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
        reverseName = DeliveryService.P_OnlineOrderDeliveries, 
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
        displayName = "Online Order", 
        reverseName = OnlineOrder.P_OnlineOrderDeliveries, 
        allowCreateNew = false, 
        fkeys = {@OAFkey(fromProperty = P_OnlineOrderId, toProperty = OnlineOrder.P_Id)}
    )
    public OnlineOrder getOnlineOrder() {
        if (onlineOrder == null) {
            onlineOrder = (OnlineOrder) getObject(P_OnlineOrder);
        }
        return onlineOrder;
    }
    public void setOnlineOrder(OnlineOrder newValue) {
        OnlineOrder old = this.onlineOrder;
        fireBeforePropertyChange(P_OnlineOrder, old, newValue);
        this.onlineOrder = newValue;
        firePropertyChange(P_OnlineOrder, old, this.onlineOrder);
    }
    @OAProperty(isFkeyOnly = true)
    @OAColumn(name = "OnlineOrderId")
    public Integer getOnlineOrderId() {
        return (Integer) getFkeyProperty(P_OnlineOrderId);
    }
    public void setOnlineOrderId(Integer newValue) {
        this.onlineOrder = null;
        setFkeyProperty(P_OnlineOrderId, newValue);
    }

    @OAMany(
        displayName = "Ood Items", 
        toClass = OodItem.class, 
        owner = true, 
        reverseName = OodItem.P_OnlineOrderDelivery, 
        cascadeSave = true, 
        cascadeDelete = true
    )
    public Hub<OodItem> getOodItems() {
        if (hubOodItems == null) {
            hubOodItems = (Hub<OodItem>) getHub(P_OodItems);
        }
        return hubOodItems;
    }
    public void load(ResultSet rs, int id) throws SQLException {
        this.id = id;
        java.sql.Timestamp timestamp;
        timestamp = rs.getTimestamp(2);
        if (timestamp != null) this.created = new OADateTime(timestamp);
        int deliveryServiceFkey = rs.getInt(3);
        setFkeyProperty(P_DeliveryService, rs.wasNull() ? null : deliveryServiceFkey);
        int onlineOrderFkey = rs.getInt(4);
        setFkeyProperty(P_OnlineOrder, rs.wasNull() ? null : onlineOrderFkey);

        this.changedFlag = false;
        this.newFlag = false;
    }
}
 
