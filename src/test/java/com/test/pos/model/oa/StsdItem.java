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
    lowerName = "stsdItem",
    pluralName = "StsdItems",
    shortName = "sti",
    displayName = "Stsd Item",
    displayProperty = "id",
    noPojo = true
)
@OATable(
    indexes = {
        @OAIndex(name = "StsdItemStsDelivery", fkey = true, columns = { @OAIndexColumn(name = "StsDeliveryId") }), 
        @OAIndex(name = "StsdItemStsItem", fkey = true, columns = { @OAIndexColumn(name = "StsItemId") })
    }
)
public class StsdItem extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(StsdItem.class.getName());

    public static final String P_Id = "id";
    public static final String P_Created = "created";
    public static final String P_Quantity = "quantity";
    public static final String P_Received = "received";
     
    public static final String P_StsDelivery = "stsDelivery";
    public static final String P_StsDeliveryId = "stsDeliveryId"; // fkey
    public static final String P_StsdItemEaches = "stsdItemEaches";
    public static final String P_StsItem = "stsItem";
    public static final String P_StsItemId = "stsItemId"; // fkey
     
    protected volatile int id;
    protected volatile OADateTime created;
    protected volatile int quantity;
    protected volatile OADateTime received;
     
    // Links to other objects.
    protected volatile transient StsDelivery stsDelivery;
    protected transient Hub<StsdItemEach> hubStsdItemEaches;
    protected volatile transient StsItem stsItem;
     
    public StsdItem() {
        if (!isLoading()) setObjectDefaults();
    }
    @Override
    public void setObjectDefaults() {
        setCreated(new OADateTime());
    }
     
    public StsdItem(int id) {
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

    @OAProperty(lowerName = "received", displayLength = 15, ignoreTimeZone = true)
    @OAColumn(name = "Received", sqlType = java.sql.Types.TIMESTAMP)
    public OADateTime getReceived() {
        return received;
    }
    public void setReceived(OADateTime newValue) {
        OADateTime old = received;
        fireBeforePropertyChange(P_Received, old, newValue);
        this.received = newValue;
        firePropertyChange(P_Received, old, this.received);
    }

    @OAOne(
        displayName = "Sts Delivery", 
        reverseName = StsDelivery.P_StsdItems, 
        required = true, 
        allowCreateNew = false, 
        fkeys = {@OAFkey(fromProperty = P_StsDeliveryId, toProperty = StsDelivery.P_Id)}
    )
    public StsDelivery getStsDelivery() {
        if (stsDelivery == null) {
            stsDelivery = (StsDelivery) getObject(P_StsDelivery);
        }
        return stsDelivery;
    }
    public void setStsDelivery(StsDelivery newValue) {
        StsDelivery old = this.stsDelivery;
        fireBeforePropertyChange(P_StsDelivery, old, newValue);
        this.stsDelivery = newValue;
        firePropertyChange(P_StsDelivery, old, this.stsDelivery);
    }
    @OAProperty(isFkeyOnly = true)
    @OAColumn(name = "StsDeliveryId")
    public Integer getStsDeliveryId() {
        return (Integer) getFkeyProperty(P_StsDeliveryId);
    }
    public void setStsDeliveryId(Integer newValue) {
        this.stsDelivery = null;
        setFkeyProperty(P_StsDeliveryId, newValue);
    }

    @OAMany(
        displayName = "Stsd Item Eaches", 
        toClass = StsdItemEach.class, 
        owner = true, 
        reverseName = StsdItemEach.P_StsdItem, 
        cascadeSave = true, 
        cascadeDelete = true
    )
    public Hub<StsdItemEach> getStsdItemEaches() {
        if (hubStsdItemEaches == null) {
            hubStsdItemEaches = (Hub<StsdItemEach>) getHub(P_StsdItemEaches);
        }
        return hubStsdItemEaches;
    }

    @OAOne(
        displayName = "Sts Item", 
        reverseName = StsItem.P_StsdItems, 
        allowCreateNew = false, 
        fkeys = {@OAFkey(fromProperty = P_StsItemId, toProperty = StsItem.P_Id)}
    )
    public StsItem getStsItem() {
        if (stsItem == null) {
            stsItem = (StsItem) getObject(P_StsItem);
        }
        return stsItem;
    }
    public void setStsItem(StsItem newValue) {
        StsItem old = this.stsItem;
        fireBeforePropertyChange(P_StsItem, old, newValue);
        this.stsItem = newValue;
        firePropertyChange(P_StsItem, old, this.stsItem);
    }
    @OAProperty(isFkeyOnly = true)
    @OAColumn(name = "StsItemId")
    public Integer getStsItemId() {
        return (Integer) getFkeyProperty(P_StsItemId);
    }
    public void setStsItemId(Integer newValue) {
        this.stsItem = null;
        setFkeyProperty(P_StsItemId, newValue);
    }
    public void load(ResultSet rs, int id) throws SQLException {
        this.id = id;
        java.sql.Timestamp timestamp;
        timestamp = rs.getTimestamp(2);
        if (timestamp != null) this.created = new OADateTime(timestamp);
        this.quantity = rs.getInt(3);
        setPrimitiveNull(P_Quantity, rs.wasNull());
        timestamp = rs.getTimestamp(4);
        if (timestamp != null) this.received = new OADateTime(timestamp);
        int stsDeliveryFkey = rs.getInt(5);
        setFkeyProperty(P_StsDelivery, rs.wasNull() ? null : stsDeliveryFkey);
        int stsItemFkey = rs.getInt(6);
        setFkeyProperty(P_StsItem, rs.wasNull() ? null : stsItemFkey);

        this.changedFlag = false;
        this.newFlag = false;
    }
}
 
