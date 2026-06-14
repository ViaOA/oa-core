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
    lowerName = "onlineOrderItem",
    pluralName = "OnlineOrderItems",
    shortName = "ooi",
    displayName = "Online Order Item",
    displayProperty = "id",
    noPojo = true
)
@OATable(
    indexes = {
        @OAIndex(name = "OnlineOrderItemItem", fkey = true, columns = { @OAIndexColumn(name = "ItemId") }), 
        @OAIndex(name = "OnlineOrderItemOnlineOrder", fkey = true, columns = { @OAIndexColumn(name = "OnlineOrderId") })
    }
)
public class OnlineOrderItem extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(OnlineOrderItem.class.getName());

    public static final String P_Id = "id";
    public static final String P_Created = "created";
    public static final String P_Quantity = "quantity";
     
    public static final String P_Item = "item";
    public static final String P_ItemId = "itemId"; // fkey
    public static final String P_OnlineOrder = "onlineOrder";
    public static final String P_OnlineOrderId = "onlineOrderId"; // fkey
    public static final String P_OodItems = "oodItems";
     
    protected volatile int id;
    protected volatile OADateTime created;
    protected volatile int quantity;
     
    // Links to other objects.
    protected volatile transient Item item;
    protected volatile transient OnlineOrder onlineOrder;
    protected transient Hub<OodItem> hubOodItems;
     
    public OnlineOrderItem() {
        if (!isLoading()) setObjectDefaults();
    }
    @Override
    public void setObjectDefaults() {
        setCreated(new OADateTime());
    }
     
    public OnlineOrderItem(int id) {
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
        reverseName = Item.P_OnlineOrderItems, 
        allowCreateNew = false, 
        fkeys = {@OAFkey(fromProperty = P_ItemId, toProperty = Item.P_Id)}
    )
    public Item getItem() {
        if (item == null) {
            item = (Item) getObject(P_Item);
        }
        return item;
    }
    public void setItem(Item newValue) {
        Item old = this.item;
        fireBeforePropertyChange(P_Item, old, newValue);
        this.item = newValue;
        firePropertyChange(P_Item, old, this.item);
    }
    @OAProperty(isFkeyOnly = true)
    @OAColumn(name = "ItemId")
    public Integer getItemId() {
        return (Integer) getFkeyProperty(P_ItemId);
    }
    public void setItemId(Integer newValue) {
        this.item = null;
        setFkeyProperty(P_ItemId, newValue);
    }

    @OAOne(
        displayName = "Online Order", 
        reverseName = OnlineOrder.P_OnlineOrderItems, 
        required = true, 
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
        reverseName = OodItem.P_OnlineOrderItem
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
        this.quantity = rs.getInt(3);
        setPrimitiveNull(P_Quantity, rs.wasNull());
        int itemFkey = rs.getInt(4);
        setFkeyProperty(P_Item, rs.wasNull() ? null : itemFkey);
        int onlineOrderFkey = rs.getInt(5);
        setFkeyProperty(P_OnlineOrder, rs.wasNull() ? null : onlineOrderFkey);

        this.changedFlag = false;
        this.newFlag = false;
    }
}
 
