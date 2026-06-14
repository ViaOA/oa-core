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
    lowerName = "itemPack",
    pluralName = "ItemPacks",
    shortName = "itp",
    displayName = "Item Pack",
    displayProperty = "name",
    noPojo = true
)
@OATable(
    indexes = {
        @OAIndex(name = "ItemPackItem", fkey = true, columns = { @OAIndexColumn(name = "ItemId") }), 
        @OAIndex(name = "ItemPackItemPackType", fkey = true, columns = { @OAIndexColumn(name = "ItemPackTypeId") })
    }
)
public class ItemPack extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(ItemPack.class.getName());

    public static final String P_Id = "id";
    public static final String P_Created = "created";
    public static final String P_Name = "name";
     
    public static final String P_Item = "item";
    public static final String P_ItemId = "itemId"; // fkey
    public static final String P_ItemPackType = "itemPackType";
    public static final String P_ItemPackTypeId = "itemPackTypeId"; // fkey
    public static final String P_PriceBookEntries = "priceBookEntries";
    public static final String P_Products = "products";
     
    protected volatile int id;
    protected volatile OADateTime created;
    protected volatile String name;
     
    // Links to other objects.
    protected volatile transient Item item;
    protected volatile transient ItemPackType itemPackType;
    protected transient Hub<PriceBookEntry> hubPriceBookEntries;
    protected transient Hub<Product> hubProducts;
     
    public ItemPack() {
        if (!isLoading()) setObjectDefaults();
    }
    @Override
    public void setObjectDefaults() {
        setCreated(new OADateTime());
    }
     
    public ItemPack(int id) {
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

    @OAProperty(lowerName = "name", maxLength = 50, displayLength = 18)
    @OAColumn(name = "Name", maxLength = 50)
    public String getName() {
        return name;
    }
    public void setName(String newValue) {
        String old = name;
        fireBeforePropertyChange(P_Name, old, newValue);
        this.name = newValue;
        firePropertyChange(P_Name, old, this.name);
    }

    @OAOne(
        reverseName = Item.P_ItemPacks, 
        required = true, 
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
        displayName = "Item Pack Type", 
        reverseName = ItemPackType.P_ItemPacks, 
        allowCreateNew = false, 
        fkeys = {@OAFkey(fromProperty = P_ItemPackTypeId, toProperty = ItemPackType.P_Id)}
    )
    public ItemPackType getItemPackType() {
        if (itemPackType == null) {
            itemPackType = (ItemPackType) getObject(P_ItemPackType);
        }
        return itemPackType;
    }
    public void setItemPackType(ItemPackType newValue) {
        ItemPackType old = this.itemPackType;
        fireBeforePropertyChange(P_ItemPackType, old, newValue);
        this.itemPackType = newValue;
        firePropertyChange(P_ItemPackType, old, this.itemPackType);
    }
    @OAProperty(isFkeyOnly = true)
    @OAColumn(name = "ItemPackTypeId")
    public Integer getItemPackTypeId() {
        return (Integer) getFkeyProperty(P_ItemPackTypeId);
    }
    public void setItemPackTypeId(Integer newValue) {
        this.itemPackType = null;
        setFkeyProperty(P_ItemPackTypeId, newValue);
    }

    @OAMany(
        displayName = "Price Book Entries", 
        toClass = PriceBookEntry.class, 
        reverseName = PriceBookEntry.P_ItemPack
    )
    public Hub<PriceBookEntry> getPriceBookEntries() {
        if (hubPriceBookEntries == null) {
            hubPriceBookEntries = (Hub<PriceBookEntry>) getHub(P_PriceBookEntries);
        }
        return hubPriceBookEntries;
    }

    @OAMany(
        toClass = Product.class, 
        reverseName = Product.P_ItemPack
    )
    public Hub<Product> getProducts() {
        if (hubProducts == null) {
            hubProducts = (Hub<Product>) getHub(P_Products);
        }
        return hubProducts;
    }
    public void load(ResultSet rs, int id) throws SQLException {
        this.id = id;
        java.sql.Timestamp timestamp;
        timestamp = rs.getTimestamp(2);
        if (timestamp != null) this.created = new OADateTime(timestamp);
        this.name = rs.getString(3);
        int itemFkey = rs.getInt(4);
        setFkeyProperty(P_Item, rs.wasNull() ? null : itemFkey);
        int itemPackTypeFkey = rs.getInt(5);
        setFkeyProperty(P_ItemPackType, rs.wasNull() ? null : itemPackTypeFkey);

        this.changedFlag = false;
        this.newFlag = false;
    }
}
 
