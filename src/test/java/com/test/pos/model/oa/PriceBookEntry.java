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
import com.viaoa.datetime.OADate;
import com.test.pos.delegate.oa.*;
import com.test.pos.model.oa.filter.*;
import com.test.pos.model.oa.propertypath.*;
 
@OAClass(
    lowerName = "priceBookEntry",
    pluralName = "PriceBookEntries",
    shortName = "pbe",
    displayName = "Price Book Entry",
    displayProperty = "salePrice",
    noPojo = true
)
@OATable(
    indexes = {
        @OAIndex(name = "PriceBookEntryItem", fkey = true, columns = { @OAIndexColumn(name = "ItemId") }), 
        @OAIndex(name = "PriceBookEntryItemOptionValue", fkey = true, columns = { @OAIndexColumn(name = "ItemOptionValueId") }), 
        @OAIndex(name = "PriceBookEntryItemPack", fkey = true, columns = { @OAIndexColumn(name = "ItemPackId") }), 
        @OAIndex(name = "PriceBookEntryProduct", fkey = true, columns = { @OAIndexColumn(name = "ProductId") })
    }
)
public class PriceBookEntry extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(PriceBookEntry.class.getName());

    public static final String P_Id = "id";
    public static final String P_Created = "created";
    public static final String P_Name = "name";
    public static final String P_SalePrice = "salePrice";
    public static final String P_FromDate = "fromDate";
    public static final String P_ToDate = "toDate";
    public static final String P_Promotion = "promotion";
    public static final String P_Priority = "priority";
     
    public static final String P_CalcForCurrentPriceBookEntry = "calcForCurrentPriceBookEntry";
    public static final String P_Item = "item";
    public static final String P_ItemId = "itemId"; // fkey
    public static final String P_ItemOptionValue = "itemOptionValue";
    public static final String P_ItemOptionValueId = "itemOptionValueId"; // fkey
    public static final String P_ItemPack = "itemPack";
    public static final String P_ItemPackId = "itemPackId"; // fkey
    public static final String P_Product = "product";
    public static final String P_ProductId = "productId"; // fkey
     
    protected volatile int id;
    protected volatile OADateTime created;
    protected volatile String name;
    protected volatile double salePrice;
    protected volatile OADate fromDate;
    protected volatile OADate toDate;
    protected volatile boolean promotion;
    protected volatile int priority;
     
    // Links to other objects.
    protected volatile transient Item item;
    protected volatile transient ItemOptionValue itemOptionValue;
    protected volatile transient ItemPack itemPack;
    protected volatile transient Product product;
     
    public PriceBookEntry() {
        if (!isLoading()) setObjectDefaults();
    }
    @Override
    public void setObjectDefaults() {
        setCreated(new OADateTime());
    }
     
    public PriceBookEntry(int id) {
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

    @OAProperty(lowerName = "name", maxLength = 30, displayLength = 18)
    @OAColumn(name = "Name", maxLength = 30)
    public String getName() {
        return name;
    }
    public void setName(String newValue) {
        String old = name;
        fireBeforePropertyChange(P_Name, old, newValue);
        this.name = newValue;
        firePropertyChange(P_Name, old, this.name);
    }

    @OAProperty(lowerName = "salePrice", displayName = "Sale Price", decimalPlaces = 2, isCurrency = true, displayLength = 9, uiColumnLength = 10)
    @OAColumn(name = "SalePrice", sqlType = java.sql.Types.NUMERIC)
    public double getSalePrice() {
        return salePrice;
    }
    public void setSalePrice(double newValue) {
        double old = salePrice;
        fireBeforePropertyChange(P_SalePrice, old, newValue);
        this.salePrice = newValue;
        firePropertyChange(P_SalePrice, old, this.salePrice);
    }

    @OAProperty(lowerName = "fromDate", displayName = "From Date", displayLength = 8, uiColumnLength = 9)
    @OAColumn(name = "FromDate", sqlType = java.sql.Types.DATE)
    public OADate getFromDate() {
        return fromDate;
    }
    public void setFromDate(OADate newValue) {
        OADate old = fromDate;
        fireBeforePropertyChange(P_FromDate, old, newValue);
        this.fromDate = newValue;
        firePropertyChange(P_FromDate, old, this.fromDate);
    }

    @OAProperty(lowerName = "toDate", displayName = "To Date", displayLength = 8)
    @OAColumn(name = "ToDate", sqlType = java.sql.Types.DATE)
    public OADate getToDate() {
        return toDate;
    }
    public void setToDate(OADate newValue) {
        OADate old = toDate;
        fireBeforePropertyChange(P_ToDate, old, newValue);
        this.toDate = newValue;
        firePropertyChange(P_ToDate, old, this.toDate);
    }

    @OAProperty(lowerName = "promotion", trackPrimitiveNull = false, displayLength = 5, uiColumnLength = 9)
    @OAColumn(name = "Promotion", sqlType = java.sql.Types.BOOLEAN)
    public boolean getPromotion() {
        return promotion;
    }
    public boolean isPromotion() {
        return getPromotion();
    }
    public void setPromotion(boolean newValue) {
        boolean old = promotion;
        fireBeforePropertyChange(P_Promotion, old, newValue);
        this.promotion = newValue;
        firePropertyChange(P_Promotion, old, this.promotion);
    }

    @OAProperty(lowerName = "priority", displayLength = 6, uiColumnLength = 8)
    @OAColumn(name = "Priority", sqlType = java.sql.Types.INTEGER)
    public int getPriority() {
        return priority;
    }
    public void setPriority(int newValue) {
        int old = priority;
        fireBeforePropertyChange(P_Priority, old, newValue);
        this.priority = newValue;
        firePropertyChange(P_Priority, old, this.priority);
    }

    @OAOne(
        displayName = "Product", 
        isCalculated = true, 
        reverseName = Product.P_CurrentPriceBookEntry, 
        allowCreateNew = false, 
        allowAddExisting = false
    )
    private Product getCalcForCurrentPriceBookEntry() {
        // oamodel has createMethod set to false, this method exists only for annotations.
        return null;
    }

    @OAOne(
        reverseName = Item.P_PriceBookEntries, 
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
        displayName = "Item Option Value", 
        reverseName = ItemOptionValue.P_PriceBookEntries, 
        allowCreateNew = false, 
        fkeys = {@OAFkey(fromProperty = P_ItemOptionValueId, toProperty = ItemOptionValue.P_Id)}
    )
    public ItemOptionValue getItemOptionValue() {
        if (itemOptionValue == null) {
            itemOptionValue = (ItemOptionValue) getObject(P_ItemOptionValue);
        }
        return itemOptionValue;
    }
    public void setItemOptionValue(ItemOptionValue newValue) {
        ItemOptionValue old = this.itemOptionValue;
        fireBeforePropertyChange(P_ItemOptionValue, old, newValue);
        this.itemOptionValue = newValue;
        firePropertyChange(P_ItemOptionValue, old, this.itemOptionValue);
    }
    @OAProperty(isFkeyOnly = true)
    @OAColumn(name = "ItemOptionValueId")
    public Integer getItemOptionValueId() {
        return (Integer) getFkeyProperty(P_ItemOptionValueId);
    }
    public void setItemOptionValueId(Integer newValue) {
        this.itemOptionValue = null;
        setFkeyProperty(P_ItemOptionValueId, newValue);
    }

    @OAOne(
        displayName = "Item Pack", 
        reverseName = ItemPack.P_PriceBookEntries, 
        allowCreateNew = false, 
        selectFromPath = P_Item + "." + Item.P_ItemPacks, 
        fkeys = {@OAFkey(fromProperty = P_ItemPackId, toProperty = ItemPack.P_Id)}
    )
    public ItemPack getItemPack() {
        if (itemPack == null) {
            itemPack = (ItemPack) getObject(P_ItemPack);
        }
        return itemPack;
    }
    public void setItemPack(ItemPack newValue) {
        ItemPack old = this.itemPack;
        fireBeforePropertyChange(P_ItemPack, old, newValue);
        this.itemPack = newValue;
        firePropertyChange(P_ItemPack, old, this.itemPack);
    }
    @OAProperty(isFkeyOnly = true)
    @OAColumn(name = "ItemPackId")
    public Integer getItemPackId() {
        return (Integer) getFkeyProperty(P_ItemPackId);
    }
    public void setItemPackId(Integer newValue) {
        this.itemPack = null;
        setFkeyProperty(P_ItemPackId, newValue);
    }

    @OAOne(
        reverseName = Product.P_PriceBookEntries, 
        allowCreateNew = false, 
        selectFromPath = P_Item + "." + Item.P_Products, 
        fkeys = {@OAFkey(fromProperty = P_ProductId, toProperty = Product.P_Id)}
    )
    public Product getProduct() {
        if (product == null) {
            product = (Product) getObject(P_Product);
        }
        return product;
    }
    public void setProduct(Product newValue) {
        Product old = this.product;
        fireBeforePropertyChange(P_Product, old, newValue);
        this.product = newValue;
        firePropertyChange(P_Product, old, this.product);
    }
    @OAProperty(isFkeyOnly = true)
    @OAColumn(name = "ProductId")
    public Integer getProductId() {
        return (Integer) getFkeyProperty(P_ProductId);
    }
    public void setProductId(Integer newValue) {
        this.product = null;
        setFkeyProperty(P_ProductId, newValue);
    }
    public void load(ResultSet rs, int id) throws SQLException {
        this.id = id;
        java.sql.Timestamp timestamp;
        timestamp = rs.getTimestamp(2);
        if (timestamp != null) this.created = new OADateTime(timestamp);
        this.name = rs.getString(3);
        this.salePrice = rs.getDouble(4);
        setPrimitiveNull(P_SalePrice, rs.wasNull());
        java.sql.Date date;
        date = rs.getDate(5);
        if (date != null) this.fromDate = new OADate(date);
        date = rs.getDate(6);
        if (date != null) this.toDate = new OADate(date);
        this.promotion = rs.getBoolean(7);
        this.priority = rs.getInt(8);
        setPrimitiveNull(P_Priority, rs.wasNull());
        int itemFkey = rs.getInt(9);
        setFkeyProperty(P_Item, rs.wasNull() ? null : itemFkey);
        int itemOptionValueFkey = rs.getInt(10);
        setFkeyProperty(P_ItemOptionValue, rs.wasNull() ? null : itemOptionValueFkey);
        int itemPackFkey = rs.getInt(11);
        setFkeyProperty(P_ItemPack, rs.wasNull() ? null : itemPackFkey);
        int productFkey = rs.getInt(12);
        setFkeyProperty(P_Product, rs.wasNull() ? null : productFkey);

        this.changedFlag = false;
        this.newFlag = false;
    }
}
 
