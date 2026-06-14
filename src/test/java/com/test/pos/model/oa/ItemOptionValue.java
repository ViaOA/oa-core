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
    lowerName = "itemOptionValue",
    pluralName = "ItemOptionValues",
    shortName = "iov",
    displayName = "Item Option Value",
    displayProperty = "value",
    noPojo = true
)
@OATable(
    indexes = {
        @OAIndex(name = "ItemOptionValueItemOption", fkey = true, columns = { @OAIndexColumn(name = "ItemOptionId") })
    }
)
public class ItemOptionValue extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(ItemOptionValue.class.getName());

    public static final String P_Id = "id";
    public static final String P_Created = "created";
    public static final String P_Value = "value";
     
    public static final String P_ItemOption = "itemOption";
    public static final String P_ItemOptionId = "itemOptionId"; // fkey
    public static final String P_ItemVariants = "itemVariants";
    public static final String P_ItemVariantsId = "itemVariantsId"; // fkey
    public static final String P_PriceBookEntries = "priceBookEntries";
     
    protected volatile int id;
    protected volatile OADateTime created;
    protected volatile String value;
     
    // Links to other objects.
    protected volatile transient ItemOption itemOption;
    protected transient Hub<ItemVariant> hubItemVariants;
    protected transient Hub<PriceBookEntry> hubPriceBookEntries;
     
    public ItemOptionValue() {
        if (!isLoading()) setObjectDefaults();
    }
    @Override
    public void setObjectDefaults() {
        setCreated(new OADateTime());
    }
     
    public ItemOptionValue(int id) {
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

    @OAProperty(lowerName = "value", maxLength = 50, displayLength = 18)
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
        displayName = "Item Option", 
        reverseName = ItemOption.P_ItemOptionValues, 
        required = true, 
        allowCreateNew = false, 
        fkeys = {@OAFkey(fromProperty = P_ItemOptionId, toProperty = ItemOption.P_Id)}
    )
    public ItemOption getItemOption() {
        if (itemOption == null) {
            itemOption = (ItemOption) getObject(P_ItemOption);
        }
        return itemOption;
    }
    public void setItemOption(ItemOption newValue) {
        ItemOption old = this.itemOption;
        fireBeforePropertyChange(P_ItemOption, old, newValue);
        this.itemOption = newValue;
        firePropertyChange(P_ItemOption, old, this.itemOption);
    }
    @OAProperty(isFkeyOnly = true)
    @OAColumn(name = "ItemOptionId")
    public Integer getItemOptionId() {
        return (Integer) getFkeyProperty(P_ItemOptionId);
    }
    public void setItemOptionId(Integer newValue) {
        this.itemOption = null;
        setFkeyProperty(P_ItemOptionId, newValue);
    }

    @OAMany(
        displayName = "Item Variants", 
        toClass = ItemVariant.class, 
        reverseName = ItemVariant.P_ItemOptionValues
    )
    @OALinkTable(name = "ItemOptionValueItemVariant", indexName = "ItemVariantItemOptionValue", columns = {"ItemOptionValueId"})
    public Hub<ItemVariant> getItemVariants() {
        if (hubItemVariants == null) {
            hubItemVariants = (Hub<ItemVariant>) getHub(P_ItemVariants);
        }
        return hubItemVariants;
    }

    @OAMany(
        displayName = "Price Book Entries", 
        toClass = PriceBookEntry.class, 
        reverseName = PriceBookEntry.P_ItemOptionValue
    )
    public Hub<PriceBookEntry> getPriceBookEntries() {
        if (hubPriceBookEntries == null) {
            hubPriceBookEntries = (Hub<PriceBookEntry>) getHub(P_PriceBookEntries);
        }
        return hubPriceBookEntries;
    }
    public void load(ResultSet rs, int id) throws SQLException {
        this.id = id;
        java.sql.Timestamp timestamp;
        timestamp = rs.getTimestamp(2);
        if (timestamp != null) this.created = new OADateTime(timestamp);
        this.value = rs.getString(3);
        int itemOptionFkey = rs.getInt(4);
        setFkeyProperty(P_ItemOption, rs.wasNull() ? null : itemOptionFkey);

        this.changedFlag = false;
        this.newFlag = false;
    }
}
 
