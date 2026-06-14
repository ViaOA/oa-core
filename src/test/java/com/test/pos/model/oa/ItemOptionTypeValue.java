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
    lowerName = "itemOptionTypeValue",
    pluralName = "ItemOptionTypeValues",
    shortName = "iot",
    displayName = "Item Option Type Value",
    displayProperty = "value",
    noPojo = true
)
@OATable(
    indexes = {
        @OAIndex(name = "ItemOptionTypeValueItemOptionType", fkey = true, columns = { @OAIndexColumn(name = "ItemOptionTypeId") })
    }
)
public class ItemOptionTypeValue extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(ItemOptionTypeValue.class.getName());

    public static final String P_Id = "id";
    public static final String P_Created = "created";
    public static final String P_Value = "value";
     
    public static final String P_ItemOptionType = "itemOptionType";
    public static final String P_ItemOptionTypeId = "itemOptionTypeId"; // fkey
     
    protected volatile int id;
    protected volatile OADateTime created;
    protected volatile String value;
     
    // Links to other objects.
    protected volatile transient ItemOptionType itemOptionType;
     
    public ItemOptionTypeValue() {
        if (!isLoading()) setObjectDefaults();
    }
    @Override
    public void setObjectDefaults() {
        setCreated(new OADateTime());
    }
     
    public ItemOptionTypeValue(int id) {
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

    @OAProperty(lowerName = "value", maxLength = 50, displayLength = 12)
    @OAColumn(name = "Value", maxLength = 50)
    public String getValue() {
        return value;
    }
    public void setValue(String newValue) {
        String old = value;
        fireBeforePropertyChange(P_Value, old, newValue);
        this.value = newValue;
        firePropertyChange(P_Value, old, this.value);
    }

    @OAOne(
        displayName = "Item Option Type", 
        reverseName = ItemOptionType.P_ItemOptionTypeValues, 
        required = true, 
        allowCreateNew = false, 
        fkeys = {@OAFkey(fromProperty = P_ItemOptionTypeId, toProperty = ItemOptionType.P_Id)}
    )
    public ItemOptionType getItemOptionType() {
        if (itemOptionType == null) {
            itemOptionType = (ItemOptionType) getObject(P_ItemOptionType);
        }
        return itemOptionType;
    }
    public void setItemOptionType(ItemOptionType newValue) {
        ItemOptionType old = this.itemOptionType;
        fireBeforePropertyChange(P_ItemOptionType, old, newValue);
        this.itemOptionType = newValue;
        firePropertyChange(P_ItemOptionType, old, this.itemOptionType);
    }
    @OAProperty(isFkeyOnly = true)
    @OAColumn(name = "ItemOptionTypeId")
    public Integer getItemOptionTypeId() {
        return (Integer) getFkeyProperty(P_ItemOptionTypeId);
    }
    public void setItemOptionTypeId(Integer newValue) {
        this.itemOptionType = null;
        setFkeyProperty(P_ItemOptionTypeId, newValue);
    }
    public void load(ResultSet rs, int id) throws SQLException {
        this.id = id;
        java.sql.Timestamp timestamp;
        timestamp = rs.getTimestamp(2);
        if (timestamp != null) this.created = new OADateTime(timestamp);
        this.value = rs.getString(3);
        int itemOptionTypeFkey = rs.getInt(4);
        setFkeyProperty(P_ItemOptionType, rs.wasNull() ? null : itemOptionTypeFkey);

        this.changedFlag = false;
        this.newFlag = false;
    }
}
 
