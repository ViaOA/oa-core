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
    lowerName = "stsItem",
    pluralName = "StsItems",
    shortName = "sti",
    displayName = "Sts Item",
    displayProperty = "id",
    noPojo = true
)
@OATable(
    indexes = {
        @OAIndex(name = "StsItemItem", fkey = true, columns = { @OAIndexColumn(name = "ItemId") }), 
        @OAIndex(name = "StsItemStoreToStoreTransfer", fkey = true, columns = { @OAIndexColumn(name = "StoreToStoreTransferId") })
    }
)
public class StsItem extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(StsItem.class.getName());

    public static final String P_Id = "id";
    public static final String P_Created = "created";
    public static final String P_Quantity = "quantity";
     
    public static final String P_Item = "item";
    public static final String P_ItemId = "itemId"; // fkey
    public static final String P_StoreToStoreTransfer = "storeToStoreTransfer";
    public static final String P_StoreToStoreTransferId = "storeToStoreTransferId"; // fkey
    public static final String P_StsdItems = "stsdItems";
     
    protected volatile int id;
    protected volatile OADateTime created;
    protected volatile int quantity;
     
    // Links to other objects.
    protected volatile transient Item item;
    protected volatile transient StoreToStoreTransfer storeToStoreTransfer;
    protected transient Hub<StsdItem> hubStsdItems;
     
    public StsItem() {
        if (!isLoading()) setObjectDefaults();
    }
    @Override
    public void setObjectDefaults() {
        setCreated(new OADateTime());
    }
     
    public StsItem(int id) {
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
        reverseName = Item.P_StsItems, 
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
        displayName = "Store To Store Transfer", 
        reverseName = StoreToStoreTransfer.P_StsItems, 
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
        reverseName = StsdItem.P_StsItem
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
        this.quantity = rs.getInt(3);
        setPrimitiveNull(P_Quantity, rs.wasNull());
        int itemFkey = rs.getInt(4);
        setFkeyProperty(P_Item, rs.wasNull() ? null : itemFkey);
        int storeToStoreTransferFkey = rs.getInt(5);
        setFkeyProperty(P_StoreToStoreTransfer, rs.wasNull() ? null : storeToStoreTransferFkey);

        this.changedFlag = false;
        this.newFlag = false;
    }
}
 
