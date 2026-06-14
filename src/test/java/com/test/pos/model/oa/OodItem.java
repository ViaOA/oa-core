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
    lowerName = "oodItem",
    pluralName = "OodItems",
    shortName = "odi",
    displayName = "Ood Item",
    displayProperty = "id",
    noPojo = true
)
@OATable(
    indexes = {
        @OAIndex(name = "OodItemOnlineOrderDelivery", fkey = true, columns = { @OAIndexColumn(name = "OnlineOrderDeliveryId") }), 
        @OAIndex(name = "OodItemOnlineOrderItem", fkey = true, columns = { @OAIndexColumn(name = "OnlineOrderItemId") })
    }
)
public class OodItem extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(OodItem.class.getName());

    public static final String P_Id = "id";
    public static final String P_Created = "created";
    public static final String P_Quantity = "quantity";
     
    public static final String P_OnlineOrderDelivery = "onlineOrderDelivery";
    public static final String P_OnlineOrderDeliveryId = "onlineOrderDeliveryId"; // fkey
    public static final String P_OnlineOrderItem = "onlineOrderItem";
    public static final String P_OnlineOrderItemId = "onlineOrderItemId"; // fkey
    public static final String P_OodItemEaches = "oodItemEaches";
     
    protected volatile int id;
    protected volatile OADateTime created;
    protected volatile int quantity;
     
    // Links to other objects.
    protected volatile transient OnlineOrderDelivery onlineOrderDelivery;
    protected volatile transient OnlineOrderItem onlineOrderItem;
    protected transient Hub<OodItemEach> hubOodItemEaches;
     
    public OodItem() {
        if (!isLoading()) setObjectDefaults();
    }
    @Override
    public void setObjectDefaults() {
        setCreated(new OADateTime());
    }
     
    public OodItem(int id) {
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

    @OAOne(
        displayName = "Online Order Delivery", 
        reverseName = OnlineOrderDelivery.P_OodItems, 
        required = true, 
        allowCreateNew = false, 
        fkeys = {@OAFkey(fromProperty = P_OnlineOrderDeliveryId, toProperty = OnlineOrderDelivery.P_Id)}
    )
    public OnlineOrderDelivery getOnlineOrderDelivery() {
        if (onlineOrderDelivery == null) {
            onlineOrderDelivery = (OnlineOrderDelivery) getObject(P_OnlineOrderDelivery);
        }
        return onlineOrderDelivery;
    }
    public void setOnlineOrderDelivery(OnlineOrderDelivery newValue) {
        OnlineOrderDelivery old = this.onlineOrderDelivery;
        fireBeforePropertyChange(P_OnlineOrderDelivery, old, newValue);
        this.onlineOrderDelivery = newValue;
        firePropertyChange(P_OnlineOrderDelivery, old, this.onlineOrderDelivery);
    }
    @OAProperty(isFkeyOnly = true)
    @OAColumn(name = "OnlineOrderDeliveryId")
    public Integer getOnlineOrderDeliveryId() {
        return (Integer) getFkeyProperty(P_OnlineOrderDeliveryId);
    }
    public void setOnlineOrderDeliveryId(Integer newValue) {
        this.onlineOrderDelivery = null;
        setFkeyProperty(P_OnlineOrderDeliveryId, newValue);
    }

    @OAOne(
        displayName = "Online Order Item", 
        reverseName = OnlineOrderItem.P_OodItems, 
        allowCreateNew = false, 
        fkeys = {@OAFkey(fromProperty = P_OnlineOrderItemId, toProperty = OnlineOrderItem.P_Id)}
    )
    public OnlineOrderItem getOnlineOrderItem() {
        if (onlineOrderItem == null) {
            onlineOrderItem = (OnlineOrderItem) getObject(P_OnlineOrderItem);
        }
        return onlineOrderItem;
    }
    public void setOnlineOrderItem(OnlineOrderItem newValue) {
        OnlineOrderItem old = this.onlineOrderItem;
        fireBeforePropertyChange(P_OnlineOrderItem, old, newValue);
        this.onlineOrderItem = newValue;
        firePropertyChange(P_OnlineOrderItem, old, this.onlineOrderItem);
    }
    @OAProperty(isFkeyOnly = true)
    @OAColumn(name = "OnlineOrderItemId")
    public Integer getOnlineOrderItemId() {
        return (Integer) getFkeyProperty(P_OnlineOrderItemId);
    }
    public void setOnlineOrderItemId(Integer newValue) {
        this.onlineOrderItem = null;
        setFkeyProperty(P_OnlineOrderItemId, newValue);
    }

    @OAMany(
        displayName = "Ood Item Eaches", 
        toClass = OodItemEach.class, 
        owner = true, 
        reverseName = OodItemEach.P_OodItem, 
        cascadeSave = true, 
        cascadeDelete = true
    )
    public Hub<OodItemEach> getOodItemEaches() {
        if (hubOodItemEaches == null) {
            hubOodItemEaches = (Hub<OodItemEach>) getHub(P_OodItemEaches);
        }
        return hubOodItemEaches;
    }
    public void load(ResultSet rs, int id) throws SQLException {
        this.id = id;
        java.sql.Timestamp timestamp;
        timestamp = rs.getTimestamp(2);
        if (timestamp != null) this.created = new OADateTime(timestamp);
        this.quantity = rs.getInt(3);
        setPrimitiveNull(P_Quantity, rs.wasNull());
        int onlineOrderDeliveryFkey = rs.getInt(4);
        setFkeyProperty(P_OnlineOrderDelivery, rs.wasNull() ? null : onlineOrderDeliveryFkey);
        int onlineOrderItemFkey = rs.getInt(5);
        setFkeyProperty(P_OnlineOrderItem, rs.wasNull() ? null : onlineOrderItemFkey);

        this.changedFlag = false;
        this.newFlag = false;
    }
}
 
