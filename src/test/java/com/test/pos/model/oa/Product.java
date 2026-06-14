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
    lowerName = "product",
    pluralName = "Products",
    shortName = "prd",
    displayName = "Product",
    displayProperty = "sku",
    noPojo = true
)
@OATable(
    indexes = {
        @OAIndex(name = "ProductItem", fkey = true, columns = { @OAIndexColumn(name = "ItemId") }), 
        @OAIndex(name = "ProductItemPack", fkey = true, columns = { @OAIndexColumn(name = "ItemPackId") }), 
        @OAIndex(name = "ProductItemVariant", fkey = true, columns = { @OAIndexColumn(name = "ItemVariantId") })
    }
)
public class Product extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(Product.class.getName());

    public static final String P_Id = "id";
    public static final String P_Created = "created";
    public static final String P_Sku = "sku";
    public static final String P_QuantityOnHand = "quantityOnHand";
    public static final String P_Weight = "weight";
    public static final String P_SealedPackage = "sealedPackage";
    public static final String P_Discontinued = "discontinued";
    public static final String P_DiscontinuedReason = "discontinuedReason";
     
    public static final String P_CurrentPriceBookEntry = "currentPriceBookEntry";
    public static final String P_Item = "item";
    public static final String P_ItemId = "itemId"; // fkey
    public static final String P_ItemPack = "itemPack";
    public static final String P_ItemPackId = "itemPackId"; // fkey
    public static final String P_ItemVariant = "itemVariant";
    public static final String P_ItemVariantId = "itemVariantId"; // fkey
    public static final String P_LineItems = "lineItems";
    public static final String P_PriceBookEntries = "priceBookEntries";
    public static final String P_ProductSerialCodes = "productSerialCodes";
    public static final String P_ProductUpcs = "productUpcs";
     
    protected volatile int id;
    protected volatile OADateTime created;
    protected volatile String sku;
    protected volatile int quantityOnHand;
    protected volatile String weight;
    protected volatile boolean sealedPackage;
    protected volatile OADate discontinued;
    protected volatile String discontinuedReason;
     
    // Links to other objects.
    protected volatile transient Item item;
    protected volatile transient ItemPack itemPack;
    protected volatile transient ItemVariant itemVariant;
    protected transient Hub<PriceBookEntry> hubPriceBookEntries;
    protected transient Hub<ProductSerialCode> hubProductSerialCodes;
    protected transient Hub<ProductUpc> hubProductUpcs;
     
    public Product() {
        if (!isLoading()) setObjectDefaults();
    }
    @Override
    public void setObjectDefaults() {
        setCreated(new OADateTime());
    }
     
    public Product(int id) {
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

    @OAProperty(lowerName = "sku", maxLength = 25, displayLength = 15)
    @OAColumn(name = "Sku", maxLength = 25, lowerName = "SkuLower")
    public String getSku() {
        return sku;
    }
    public void setSku(String newValue) {
        String old = sku;
        fireBeforePropertyChange(P_Sku, old, newValue);
        this.sku = newValue;
        firePropertyChange(P_Sku, old, this.sku);
    }

    @OAProperty(lowerName = "quantityOnHand", displayName = "Quantity On Hand", displayLength = 6, uiColumnLength = 16)
    @OAColumn(name = "QuantityOnHand", sqlType = java.sql.Types.INTEGER)
    public int getQuantityOnHand() {
        return quantityOnHand;
    }
    public void setQuantityOnHand(int newValue) {
        int old = quantityOnHand;
        fireBeforePropertyChange(P_QuantityOnHand, old, newValue);
        this.quantityOnHand = newValue;
        firePropertyChange(P_QuantityOnHand, old, this.quantityOnHand);
    }
     
    @OAObjCallback(enabledProperty = Product.P_Item+"."+Item.P_Stocking)
    public void quantityOnHandCallback(OAObjectCallback callback) {
        if (callback == null) return;
        switch (callback.getType()) {
        }
    }

    @OAProperty(lowerName = "weight", displayLength = 20)
    @OAColumn(name = "Weight", maxLength = 0)
    public String getWeight() {
        return weight;
    }
    public void setWeight(String newValue) {
        String old = weight;
        fireBeforePropertyChange(P_Weight, old, newValue);
        this.weight = newValue;
        firePropertyChange(P_Weight, old, this.weight);
    }

    @OAProperty(lowerName = "sealedPackage", displayName = "Sealed Package", trackPrimitiveNull = false, displayLength = 5, uiColumnLength = 14)
    @OAColumn(name = "SealedPackage", sqlType = java.sql.Types.BOOLEAN)
    public boolean getSealedPackage() {
        return sealedPackage;
    }
    public boolean isSealedPackage() {
        return getSealedPackage();
    }
    public void setSealedPackage(boolean newValue) {
        boolean old = sealedPackage;
        fireBeforePropertyChange(P_SealedPackage, old, newValue);
        this.sealedPackage = newValue;
        firePropertyChange(P_SealedPackage, old, this.sealedPackage);
    }

    @OAProperty(lowerName = "discontinued", displayLength = 8, uiColumnLength = 12)
    @OAColumn(name = "Discontinued", sqlType = java.sql.Types.DATE)
    public OADate getDiscontinued() {
        return discontinued;
    }
    public void setDiscontinued(OADate newValue) {
        OADate old = discontinued;
        fireBeforePropertyChange(P_Discontinued, old, newValue);
        this.discontinued = newValue;
        firePropertyChange(P_Discontinued, old, this.discontinued);
    }

    @OAProperty(lowerName = "discontinuedReason", displayName = "Discontinued Reason", maxLength = 120, displayLength = 22, uiColumnLength = 20)
    @OAColumn(name = "DiscontinuedReason", maxLength = 120)
    public String getDiscontinuedReason() {
        return discontinuedReason;
    }
    public void setDiscontinuedReason(String newValue) {
        String old = discontinuedReason;
        fireBeforePropertyChange(P_DiscontinuedReason, old, newValue);
        this.discontinuedReason = newValue;
        firePropertyChange(P_DiscontinuedReason, old, this.discontinuedReason);
    }
     
    @OAObjCallback(enabledProperty = Item.P_Discontinued)
    public void discontinuedReasonCallback(OAObjectCallback callback) {
        if (callback == null) return;
        switch (callback.getType()) {
        }
    }

    @OAOne(
        displayName = "Current Price Book Entry", 
        isCalculated = true, 
        calcDependentProperties = {P_PriceBookEntries+"."+PriceBookEntry.P_SalePrice, P_PriceBookEntries+"."+PriceBookEntry.P_FromDate, P_PriceBookEntries+"."+PriceBookEntry.P_ToDate}, 
        reverseName = PriceBookEntry.P_CalcForCurrentPriceBookEntry, 
        allowCreateNew = false, 
        allowAddExisting = false
    )
    public PriceBookEntry getCurrentPriceBookEntry() {
        return ProductDelegate.getCurrentPriceBookEntry(this);
    }

    @OAOne(
        reverseName = Item.P_Products, 
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
        displayName = "Item Pack", 
        reverseName = ItemPack.P_Products, 
        allowCreateNew = false, 
        selectFromPropertyPath = P_Item + "." + Item.P_ItemPacks, 
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
        displayName = "Item Variant", 
        reverseName = ItemVariant.P_Products, 
        allowCreateNew = false, 
        selectFromPropertyPath = P_Item + "." + Item.P_ItemVariants, 
        fkeys = {@OAFkey(fromProperty = P_ItemVariantId, toProperty = ItemVariant.P_Id)}
    )
    public ItemVariant getItemVariant() {
        if (itemVariant == null) {
            itemVariant = (ItemVariant) getObject(P_ItemVariant);
        }
        return itemVariant;
    }
    public void setItemVariant(ItemVariant newValue) {
        ItemVariant old = this.itemVariant;
        fireBeforePropertyChange(P_ItemVariant, old, newValue);
        this.itemVariant = newValue;
        firePropertyChange(P_ItemVariant, old, this.itemVariant);
    }
    @OAProperty(isFkeyOnly = true)
    @OAColumn(name = "ItemVariantId")
    public Integer getItemVariantId() {
        return (Integer) getFkeyProperty(P_ItemVariantId);
    }
    public void setItemVariantId(Integer newValue) {
        this.itemVariant = null;
        setFkeyProperty(P_ItemVariantId, newValue);
    }

    @OAMany(
        displayName = "Line Items", 
        toClass = LineItem.class, 
        reverseName = LineItem.P_Product, 
        createMethod = false
    )
    private Hub<LineItem> getLineItems() {
        // oamodel has createMethod set to false, this method exists only for annotations.
        return null;
    }

    @OAMany(
        displayName = "Price Book Entries", 
        toClass = PriceBookEntry.class, 
        reverseName = PriceBookEntry.P_Product
    )
    public Hub<PriceBookEntry> getPriceBookEntries() {
        if (hubPriceBookEntries == null) {
            hubPriceBookEntries = (Hub<PriceBookEntry>) getHub(P_PriceBookEntries);
        }
        return hubPriceBookEntries;
    }

    @OAMany(
        displayName = "Product Serial Codes", 
        toClass = ProductSerialCode.class, 
        owner = true, 
        reverseName = ProductSerialCode.P_Product, 
        cascadeSave = true, 
        cascadeDelete = true
    )
    public Hub<ProductSerialCode> getProductSerialCodes() {
        if (hubProductSerialCodes == null) {
            hubProductSerialCodes = (Hub<ProductSerialCode>) getHub(P_ProductSerialCodes);
        }
        return hubProductSerialCodes;
    }
    @OAObjCallback(enabledProperty = Product.P_ItemVariant+"."+ItemVariant.P_Item+"."+Item.P_UseSerialCode)
    public void productSerialCodesCallback(OAObjectCallback cb) {
        if (cb == null) return;
        switch (cb.getType()) {
        }
    }

    @OAMany(
        displayName = "Product Upcs", 
        toClass = ProductUpc.class, 
        owner = true, 
        reverseName = ProductUpc.P_Product, 
        cascadeSave = true, 
        cascadeDelete = true
    )
    public Hub<ProductUpc> getProductUpcs() {
        if (hubProductUpcs == null) {
            hubProductUpcs = (Hub<ProductUpc>) getHub(P_ProductUpcs);
        }
        return hubProductUpcs;
    }
    public void load(ResultSet rs, int id) throws SQLException {
        this.id = id;
        java.sql.Timestamp timestamp;
        timestamp = rs.getTimestamp(2);
        if (timestamp != null) this.created = new OADateTime(timestamp);
        this.sku = rs.getString(3);
        this.quantityOnHand = rs.getInt(4);
        setPrimitiveNull(P_QuantityOnHand, rs.wasNull());
        this.weight = rs.getString(5);
        this.sealedPackage = rs.getBoolean(6);
        java.sql.Date date;
        date = rs.getDate(7);
        if (date != null) this.discontinued = new OADate(date);
        this.discontinuedReason = rs.getString(8);
        int itemFkey = rs.getInt(9);
        setFkeyProperty(P_Item, rs.wasNull() ? null : itemFkey);
        int itemPackFkey = rs.getInt(10);
        setFkeyProperty(P_ItemPack, rs.wasNull() ? null : itemPackFkey);
        int itemVariantFkey = rs.getInt(11);
        setFkeyProperty(P_ItemVariant, rs.wasNull() ? null : itemVariantFkey);

        this.changedFlag = false;
        this.newFlag = false;
    }
}
 
