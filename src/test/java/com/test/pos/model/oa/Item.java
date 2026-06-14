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
    lowerName = "item",
    pluralName = "Items",
    shortName = "itm",
    displayName = "Item",
    displayProperty = "code",
    noPojo = true
)
@OATable(
    indexes = {
        @OAIndex(name = "ItemName", columns = {@OAIndexColumn(name = "Name", lowerName = "NameLower")}),
        @OAIndex(name = "ItemItemLine", fkey = true, columns = { @OAIndexColumn(name = "ItemLineId") }), 
        @OAIndex(name = "ItemManufacturer", fkey = true, columns = { @OAIndexColumn(name = "ManufacturerId") })
    }
)
public class Item extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(Item.class.getName());

    public static final String P_Id = "id";
    public static final String P_Created = "created";
    public static final String P_Code = "code";
    public static final String P_Name = "name";
    public static final String P_Brand = "brand";
    public static final String P_Description = "description";
    public static final String P_UseSerialCode = "useSerialCode";
    public static final String P_SerialCodeMask = "serialCodeMask";
    public static final String P_Keywords = "keywords";
    public static final String P_HtmlDescription = "htmlDescription";
    public static final String P_Discontinued = "discontinued";
    public static final String P_DiscontinuedReason = "discontinuedReason";
    public static final String P_Stocking = "stocking";
    public static final String P_QuantityOnHand = "quantityOnHand";
    public static final String P_MinQuantityOnHand = "minQuantityOnHand";
    public static final String P_MaxQuantityOnHand = "maxQuantityOnHand";
    public static final String P_ShelfLifeInDays = "shelfLifeInDays";
    public static final String P_AgeRestricted = "ageRestricted";
    public static final String P_MinAge = "minAge";
    public static final String P_MaxAge = "maxAge";
    public static final String P_SaleByWeight = "saleByWeight";
    public static final String P_UsedForKitOnly = "usedForKitOnly";
    public static final String P_NotReturnable = "notReturnable";
     
    public static final String P_CatalogItems = "catalogItems";
    public static final String P_ItemCategories = "itemCategories";
    public static final String P_ItemCategoriesId = "itemCategoriesId"; // fkey
    public static final String P_ItemKits = "itemKits";
    public static final String P_ItemLine = "itemLine";
    public static final String P_ItemLineId = "itemLineId"; // fkey
    public static final String P_ItemOptions = "itemOptions";
    public static final String P_ItemPacks = "itemPacks";
    public static final String P_ItemVariants = "itemVariants";
    public static final String P_ItemVendors = "itemVendors";
    public static final String P_ItemVendorsId = "itemVendorsId"; // fkey
    public static final String P_Manufacturer = "manufacturer";
    public static final String P_ManufacturerId = "manufacturerId"; // fkey
    public static final String P_OnlineOrderItems = "onlineOrderItems";
    public static final String P_PriceBookEntries = "priceBookEntries";
    public static final String P_Products = "products";
    public static final String P_StsItems = "stsItems";
    public static final String P_VertexTaxCodes = "vertexTaxCodes";
    public static final String P_VertexTaxCodesId = "vertexTaxCodesId"; // fkey
     
    protected volatile int id;
    protected volatile OADateTime created;
    protected volatile String code;
    protected volatile String name;
    protected volatile String brand;
    protected volatile String description;
    protected volatile boolean useSerialCode;
    protected volatile String serialCodeMask;
    protected volatile String keywords;
    protected volatile String htmlDescription;
    protected volatile OADate discontinued;
    protected volatile String discontinuedReason;
    protected volatile boolean stocking;
    protected volatile int quantityOnHand;
    protected volatile int minQuantityOnHand;
    protected volatile int maxQuantityOnHand;
    protected volatile int shelfLifeInDays;
    protected volatile boolean ageRestricted;
    protected volatile int minAge;
    protected volatile int maxAge;
    protected volatile boolean saleByWeight;
    protected volatile boolean usedForKitOnly;
    protected volatile boolean notReturnable;
     
    // Links to other objects.
    protected transient Hub<ItemCategory> hubItemCategories;
    protected transient Hub<ItemKit> hubItemKits;
    protected volatile transient ItemLine itemLine;
    protected transient Hub<ItemOption> hubItemOptions;
    protected transient Hub<ItemPack> hubItemPacks;
    protected transient Hub<ItemVariant> hubItemVariants;
    protected transient Hub<ItemVendor> hubItemVendors;
    protected volatile transient Manufacturer manufacturer;
    protected transient Hub<OnlineOrderItem> hubOnlineOrderItems;
    protected transient Hub<PriceBookEntry> hubPriceBookEntries;
    protected transient Hub<Product> hubProducts;
    protected transient Hub<VertexTaxCode> hubVertexTaxCodes;
     
    public Item() {
        if (!isLoading()) setObjectDefaults();
    }
    @Override
    public void setObjectDefaults() {
        setCreated(new OADateTime());
    }
     
    public Item(int id) {
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

    @OAProperty(lowerName = "code", maxLength = 10, displayLength = 10)
    @OAColumn(name = "Code", maxLength = 10)
    public String getCode() {
        return code;
    }
    public void setCode(String newValue) {
        String old = code;
        fireBeforePropertyChange(P_Code, old, newValue);
        this.code = newValue;
        firePropertyChange(P_Code, old, this.code);
    }

    @OAProperty(lowerName = "name", maxLength = 75, displayLength = 22, uiColumnLength = 20)
    @OAColumn(name = "Name", maxLength = 75, lowerName = "NameLower")
    public String getName() {
        return name;
    }
    public void setName(String newValue) {
        String old = name;
        fireBeforePropertyChange(P_Name, old, newValue);
        this.name = newValue;
        firePropertyChange(P_Name, old, this.name);
    }

    @OAProperty(lowerName = "brand", maxLength = 50, displayLength = 12)
    @OAColumn(name = "Brand", maxLength = 50)
    public String getBrand() {
        return brand;
    }
    public void setBrand(String newValue) {
        String old = brand;
        fireBeforePropertyChange(P_Brand, old, newValue);
        this.brand = newValue;
        firePropertyChange(P_Brand, old, this.brand);
    }

    @OAProperty(lowerName = "description", displayLength = 30, uiColumnLength = 20)
    @OAColumn(name = "Description", sqlType = java.sql.Types.CLOB)
    public String getDescription() {
        return description;
    }
    public void setDescription(String newValue) {
        String old = description;
        fireBeforePropertyChange(P_Description, old, newValue);
        this.description = newValue;
        firePropertyChange(P_Description, old, this.description);
    }

    @OAProperty(lowerName = "useSerialCode", displayName = "Use Serial Code", displayLength = 5, uiColumnLength = 15)
    @OAColumn(name = "UseSerialCode", sqlType = java.sql.Types.BOOLEAN)
    public boolean getUseSerialCode() {
        return useSerialCode;
    }
    public boolean isUseSerialCode() {
        return getUseSerialCode();
    }
    public void setUseSerialCode(boolean newValue) {
        boolean old = useSerialCode;
        fireBeforePropertyChange(P_UseSerialCode, old, newValue);
        this.useSerialCode = newValue;
        firePropertyChange(P_UseSerialCode, old, this.useSerialCode);
    }

    @OAProperty(lowerName = "serialCodeMask", displayName = "Serial Code Mask", maxLength = 30, displayLength = 18)
    @OAColumn(name = "SerialCodeMask", maxLength = 30)
    public String getSerialCodeMask() {
        return serialCodeMask;
    }
    public void setSerialCodeMask(String newValue) {
        String old = serialCodeMask;
        fireBeforePropertyChange(P_SerialCodeMask, old, newValue);
        this.serialCodeMask = newValue;
        firePropertyChange(P_SerialCodeMask, old, this.serialCodeMask);
    }
     
    @OAObjCallback(enabledProperty = Item.P_UseSerialCode)
    public void serialCodeMaskCallback(OAObjectCallback callback) {
        if (callback == null) return;
        switch (callback.getType()) {
        }
    }

    @OAProperty(lowerName = "keywords", maxLength = 250, displayLength = 22, uiColumnLength = 20)
    @OAColumn(name = "Keywords", maxLength = 250)
    public String getKeywords() {
        return keywords;
    }
    public void setKeywords(String newValue) {
        String old = keywords;
        fireBeforePropertyChange(P_Keywords, old, newValue);
        this.keywords = newValue;
        firePropertyChange(P_Keywords, old, this.keywords);
    }

    @OAProperty(lowerName = "htmlDescription", displayName = "Html Description", displayLength = 30, uiColumnLength = 20, isHtml = true)
    @OAColumn(name = "HtmlDescription", sqlType = java.sql.Types.CLOB)
    public String getHtmlDescription() {
        return htmlDescription;
    }
    public void setHtmlDescription(String newValue) {
        String old = htmlDescription;
        fireBeforePropertyChange(P_HtmlDescription, old, newValue);
        this.htmlDescription = newValue;
        firePropertyChange(P_HtmlDescription, old, this.htmlDescription);
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
     
    @OAObjCallback(enabledProperty = Item.P_Discontinued, enabledValue = false)
    public void discontinuedReasonCallback(OAObjectCallback callback) {
        if (callback == null) return;
        switch (callback.getType()) {
        }
    }

    @OAProperty(lowerName = "stocking", displayLength = 5, uiColumnLength = 8)
    @OAColumn(name = "Stocking", sqlType = java.sql.Types.BOOLEAN)
    public boolean getStocking() {
        return stocking;
    }
    public boolean isStocking() {
        return getStocking();
    }
    public void setStocking(boolean newValue) {
        boolean old = stocking;
        fireBeforePropertyChange(P_Stocking, old, newValue);
        this.stocking = newValue;
        firePropertyChange(P_Stocking, old, this.stocking);
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
     
    @OAObjCallback(enabledProperty = Item.P_Stocking)
    public void quantityOnHandCallback(OAObjectCallback callback) {
        if (callback == null) return;
        switch (callback.getType()) {
        }
    }

    @OAProperty(lowerName = "minQuantityOnHand", displayName = "Min Quantity On Hand", displayLength = 6, uiColumnLength = 20)
    @OAColumn(name = "MinQuantityOnHand", sqlType = java.sql.Types.INTEGER)
    public int getMinQuantityOnHand() {
        return minQuantityOnHand;
    }
    public void setMinQuantityOnHand(int newValue) {
        int old = minQuantityOnHand;
        fireBeforePropertyChange(P_MinQuantityOnHand, old, newValue);
        this.minQuantityOnHand = newValue;
        firePropertyChange(P_MinQuantityOnHand, old, this.minQuantityOnHand);
    }
     
    @OAObjCallback(enabledProperty = Item.P_Stocking)
    public void minQuantityOnHandCallback(OAObjectCallback callback) {
        if (callback == null) return;
        switch (callback.getType()) {
        }
    }

    @OAProperty(lowerName = "maxQuantityOnHand", displayName = "Max Quantity On Hand", displayLength = 6, uiColumnLength = 20)
    @OAColumn(name = "MaxQuantityOnHand", sqlType = java.sql.Types.INTEGER)
    public int getMaxQuantityOnHand() {
        return maxQuantityOnHand;
    }
    public void setMaxQuantityOnHand(int newValue) {
        int old = maxQuantityOnHand;
        fireBeforePropertyChange(P_MaxQuantityOnHand, old, newValue);
        this.maxQuantityOnHand = newValue;
        firePropertyChange(P_MaxQuantityOnHand, old, this.maxQuantityOnHand);
    }
     
    @OAObjCallback(enabledProperty = Item.P_Stocking)
    public void maxQuantityOnHandCallback(OAObjectCallback callback) {
        if (callback == null) return;
        switch (callback.getType()) {
        }
    }

    @OAProperty(lowerName = "shelfLifeInDays", displayName = "Shelf Life In Days", displayLength = 6, uiColumnLength = 18)
    @OAColumn(name = "ShelfLifeInDays", sqlType = java.sql.Types.INTEGER)
    public int getShelfLifeInDays() {
        return shelfLifeInDays;
    }
    public void setShelfLifeInDays(int newValue) {
        int old = shelfLifeInDays;
        fireBeforePropertyChange(P_ShelfLifeInDays, old, newValue);
        this.shelfLifeInDays = newValue;
        firePropertyChange(P_ShelfLifeInDays, old, this.shelfLifeInDays);
    }

    @OAProperty(lowerName = "ageRestricted", displayName = "Age Restricted", displayLength = 5, uiColumnLength = 14)
    @OAColumn(name = "AgeRestricted", sqlType = java.sql.Types.BOOLEAN)
    public boolean getAgeRestricted() {
        return ageRestricted;
    }
    public boolean isAgeRestricted() {
        return getAgeRestricted();
    }
    public void setAgeRestricted(boolean newValue) {
        boolean old = ageRestricted;
        fireBeforePropertyChange(P_AgeRestricted, old, newValue);
        this.ageRestricted = newValue;
        firePropertyChange(P_AgeRestricted, old, this.ageRestricted);
    }

    @OAProperty(lowerName = "minAge", displayName = "Min Age", displayLength = 6, uiColumnLength = 7)
    @OAColumn(name = "MinAge", sqlType = java.sql.Types.INTEGER)
    public int getMinAge() {
        return minAge;
    }
    public void setMinAge(int newValue) {
        int old = minAge;
        fireBeforePropertyChange(P_MinAge, old, newValue);
        this.minAge = newValue;
        firePropertyChange(P_MinAge, old, this.minAge);
    }
     
    @OAObjCallback(enabledProperty = Item.P_AgeRestricted)
    public void minAgeCallback(OAObjectCallback callback) {
        if (callback == null) return;
        switch (callback.getType()) {
        }
    }

    @OAProperty(lowerName = "maxAge", displayName = "Max Age", displayLength = 6, uiColumnLength = 7)
    @OAColumn(name = "MaxAge", sqlType = java.sql.Types.INTEGER)
    public int getMaxAge() {
        return maxAge;
    }
    public void setMaxAge(int newValue) {
        int old = maxAge;
        fireBeforePropertyChange(P_MaxAge, old, newValue);
        this.maxAge = newValue;
        firePropertyChange(P_MaxAge, old, this.maxAge);
    }
     
    @OAObjCallback(enabledProperty = Item.P_AgeRestricted)
    public void maxAgeCallback(OAObjectCallback callback) {
        if (callback == null) return;
        switch (callback.getType()) {
        }
    }

    @OAProperty(lowerName = "saleByWeight", displayName = "Sale By Weight", displayLength = 5, uiColumnLength = 14)
    @OAColumn(name = "SaleByWeight", sqlType = java.sql.Types.BOOLEAN)
    public boolean getSaleByWeight() {
        return saleByWeight;
    }
    public boolean isSaleByWeight() {
        return getSaleByWeight();
    }
    public void setSaleByWeight(boolean newValue) {
        boolean old = saleByWeight;
        fireBeforePropertyChange(P_SaleByWeight, old, newValue);
        this.saleByWeight = newValue;
        firePropertyChange(P_SaleByWeight, old, this.saleByWeight);
    }

    @OAProperty(lowerName = "usedForKitOnly", displayName = "Used For Kit Only", displayLength = 5, uiColumnLength = 17)
    @OAColumn(name = "UsedForKitOnly", sqlType = java.sql.Types.BOOLEAN)
    public boolean getUsedForKitOnly() {
        return usedForKitOnly;
    }
    public boolean isUsedForKitOnly() {
        return getUsedForKitOnly();
    }
    public void setUsedForKitOnly(boolean newValue) {
        boolean old = usedForKitOnly;
        fireBeforePropertyChange(P_UsedForKitOnly, old, newValue);
        this.usedForKitOnly = newValue;
        firePropertyChange(P_UsedForKitOnly, old, this.usedForKitOnly);
    }

    @OAProperty(lowerName = "notReturnable", displayName = "Not Returnable", displayLength = 5, uiColumnLength = 14)
    @OAColumn(name = "NotReturnable", sqlType = java.sql.Types.BOOLEAN)
    public boolean getNotReturnable() {
        return notReturnable;
    }
    public boolean isNotReturnable() {
        return getNotReturnable();
    }
    public void setNotReturnable(boolean newValue) {
        boolean old = notReturnable;
        fireBeforePropertyChange(P_NotReturnable, old, newValue);
        this.notReturnable = newValue;
        firePropertyChange(P_NotReturnable, old, this.notReturnable);
    }

    @OAMany(
        displayName = "Catalog Items", 
        toClass = CatalogItem.class, 
        reverseName = CatalogItem.P_Item, 
        createMethod = false
    )
    private Hub<CatalogItem> getCatalogItems() {
        // oamodel has createMethod set to false, this method exists only for annotations.
        return null;
    }

    @OAMany(
        displayName = "Item Categories", 
        toClass = ItemCategory.class, 
        recursive = false, 
        reverseName = ItemCategory.P_Items
    )
    @OALinkTable(name = "ItemCategoryItem", indexName = "ItemCategoryItem", columns = {"ItemId"})
    public Hub<ItemCategory> getItemCategories() {
        if (hubItemCategories == null) {
            hubItemCategories = (Hub<ItemCategory>) getHub(P_ItemCategories);
        }
        return hubItemCategories;
    }

    @OAMany(
        displayName = "Item Kits", 
        toClass = ItemKit.class, 
        owner = true, 
        reverseName = ItemKit.P_Item, 
        cascadeSave = true, 
        cascadeDelete = true
    )
    public Hub<ItemKit> getItemKits() {
        if (hubItemKits == null) {
            hubItemKits = (Hub<ItemKit>) getHub(P_ItemKits);
        }
        return hubItemKits;
    }

    @OAOne(
        displayName = "Item Line", 
        reverseName = ItemLine.P_Items, 
        allowCreateNew = false, 
        fkeys = {@OAFkey(fromProperty = P_ItemLineId, toProperty = ItemLine.P_Id)}
    )
    public ItemLine getItemLine() {
        if (itemLine == null) {
            itemLine = (ItemLine) getObject(P_ItemLine);
        }
        return itemLine;
    }
    public void setItemLine(ItemLine newValue) {
        ItemLine old = this.itemLine;
        fireBeforePropertyChange(P_ItemLine, old, newValue);
        this.itemLine = newValue;
        firePropertyChange(P_ItemLine, old, this.itemLine);
    }
    @OAProperty(isFkeyOnly = true)
    @OAColumn(name = "ItemLineId")
    public Integer getItemLineId() {
        return (Integer) getFkeyProperty(P_ItemLineId);
    }
    public void setItemLineId(Integer newValue) {
        this.itemLine = null;
        setFkeyProperty(P_ItemLineId, newValue);
    }

    @OAMany(
        displayName = "Item Options", 
        toClass = ItemOption.class, 
        owner = true, 
        reverseName = ItemOption.P_Item, 
        cascadeSave = true, 
        cascadeDelete = true
    )
    public Hub<ItemOption> getItemOptions() {
        if (hubItemOptions == null) {
            hubItemOptions = (Hub<ItemOption>) getHub(P_ItemOptions);
        }
        return hubItemOptions;
    }

    @OAMany(
        displayName = "Item Packs", 
        toClass = ItemPack.class, 
        owner = true, 
        reverseName = ItemPack.P_Item, 
        cascadeSave = true, 
        cascadeDelete = true
    )
    public Hub<ItemPack> getItemPacks() {
        if (hubItemPacks == null) {
            hubItemPacks = (Hub<ItemPack>) getHub(P_ItemPacks);
        }
        return hubItemPacks;
    }

    @OAMany(
        displayName = "Item Variants", 
        toClass = ItemVariant.class, 
        owner = true, 
        reverseName = ItemVariant.P_Item, 
        cascadeSave = true, 
        cascadeDelete = true
    )
    public Hub<ItemVariant> getItemVariants() {
        if (hubItemVariants == null) {
            hubItemVariants = (Hub<ItemVariant>) getHub(P_ItemVariants);
        }
        return hubItemVariants;
    }

    @OAMany(
        displayName = "Item Vendors", 
        toClass = ItemVendor.class, 
        reverseName = ItemVendor.P_Items
    )
    @OALinkTable(name = "ItemVendorItem", indexName = "ItemVendorItem", columns = {"ItemId"})
    public Hub<ItemVendor> getItemVendors() {
        if (hubItemVendors == null) {
            hubItemVendors = (Hub<ItemVendor>) getHub(P_ItemVendors);
        }
        return hubItemVendors;
    }

    @OAOne(
        reverseName = Manufacturer.P_Items, 
        allowCreateNew = false, 
        fkeys = {@OAFkey(fromProperty = P_ManufacturerId, toProperty = Manufacturer.P_Id)}
    )
    public Manufacturer getManufacturer() {
        if (manufacturer == null) {
            manufacturer = (Manufacturer) getObject(P_Manufacturer);
        }
        return manufacturer;
    }
    public void setManufacturer(Manufacturer newValue) {
        Manufacturer old = this.manufacturer;
        fireBeforePropertyChange(P_Manufacturer, old, newValue);
        this.manufacturer = newValue;
        firePropertyChange(P_Manufacturer, old, this.manufacturer);
    }
    @OAProperty(isFkeyOnly = true)
    @OAColumn(name = "ManufacturerId")
    public Integer getManufacturerId() {
        return (Integer) getFkeyProperty(P_ManufacturerId);
    }
    public void setManufacturerId(Integer newValue) {
        this.manufacturer = null;
        setFkeyProperty(P_ManufacturerId, newValue);
    }

    @OAMany(
        displayName = "Online Order Items", 
        toClass = OnlineOrderItem.class, 
        reverseName = OnlineOrderItem.P_Item
    )
    public Hub<OnlineOrderItem> getOnlineOrderItems() {
        if (hubOnlineOrderItems == null) {
            hubOnlineOrderItems = (Hub<OnlineOrderItem>) getHub(P_OnlineOrderItems);
        }
        return hubOnlineOrderItems;
    }

    @OAMany(
        displayName = "Price Book Entries", 
        toClass = PriceBookEntry.class, 
        owner = true, 
        reverseName = PriceBookEntry.P_Item, 
        cascadeSave = true, 
        cascadeDelete = true
    )
    public Hub<PriceBookEntry> getPriceBookEntries() {
        if (hubPriceBookEntries == null) {
            hubPriceBookEntries = (Hub<PriceBookEntry>) getHub(P_PriceBookEntries);
        }
        return hubPriceBookEntries;
    }

    @OAMany(
        toClass = Product.class, 
        owner = true, 
        reverseName = Product.P_Item, 
        cascadeSave = true, 
        cascadeDelete = true
    )
    public Hub<Product> getProducts() {
        if (hubProducts == null) {
            hubProducts = (Hub<Product>) getHub(P_Products);
        }
        return hubProducts;
    }

    @OAMany(
        displayName = "Sts Items", 
        toClass = StsItem.class, 
        reverseName = StsItem.P_Item, 
        createMethod = false
    )
    private Hub<StsItem> getStsItems() {
        // oamodel has createMethod set to false, this method exists only for annotations.
        return null;
    }

    @OAMany(
        displayName = "Vertex Tax Codes", 
        toClass = VertexTaxCode.class, 
        reverseName = VertexTaxCode.P_Items
    )
    @OALinkTable(name = "VertexTaxCodeItem", indexName = "VertexTaxCodeItem", columns = {"ItemId"})
    public Hub<VertexTaxCode> getVertexTaxCodes() {
        if (hubVertexTaxCodes == null) {
            hubVertexTaxCodes = (Hub<VertexTaxCode>) getHub(P_VertexTaxCodes);
        }
        return hubVertexTaxCodes;
    }
    public void load(ResultSet rs, int id) throws SQLException {
        this.id = id;
        java.sql.Timestamp timestamp;
        timestamp = rs.getTimestamp(2);
        if (timestamp != null) this.created = new OADateTime(timestamp);
        this.code = rs.getString(3);
        this.name = rs.getString(4);
        this.brand = rs.getString(5);
        this.description = rs.getString(6);
        this.useSerialCode = rs.getBoolean(7);
        setPrimitiveNull(P_UseSerialCode, rs.wasNull());
        this.serialCodeMask = rs.getString(8);
        this.keywords = rs.getString(9);
        this.htmlDescription = rs.getString(10);
        java.sql.Date date;
        date = rs.getDate(11);
        if (date != null) this.discontinued = new OADate(date);
        this.discontinuedReason = rs.getString(12);
        this.stocking = rs.getBoolean(13);
        setPrimitiveNull(P_Stocking, rs.wasNull());
        this.quantityOnHand = rs.getInt(14);
        setPrimitiveNull(P_QuantityOnHand, rs.wasNull());
        this.minQuantityOnHand = rs.getInt(15);
        setPrimitiveNull(P_MinQuantityOnHand, rs.wasNull());
        this.maxQuantityOnHand = rs.getInt(16);
        setPrimitiveNull(P_MaxQuantityOnHand, rs.wasNull());
        this.shelfLifeInDays = rs.getInt(17);
        setPrimitiveNull(P_ShelfLifeInDays, rs.wasNull());
        this.ageRestricted = rs.getBoolean(18);
        setPrimitiveNull(P_AgeRestricted, rs.wasNull());
        this.minAge = rs.getInt(19);
        setPrimitiveNull(P_MinAge, rs.wasNull());
        this.maxAge = rs.getInt(20);
        setPrimitiveNull(P_MaxAge, rs.wasNull());
        this.saleByWeight = rs.getBoolean(21);
        setPrimitiveNull(P_SaleByWeight, rs.wasNull());
        this.usedForKitOnly = rs.getBoolean(22);
        setPrimitiveNull(P_UsedForKitOnly, rs.wasNull());
        this.notReturnable = rs.getBoolean(23);
        setPrimitiveNull(P_NotReturnable, rs.wasNull());
        int itemLineFkey = rs.getInt(24);
        setFkeyProperty(P_ItemLine, rs.wasNull() ? null : itemLineFkey);
        int manufacturerFkey = rs.getInt(25);
        setFkeyProperty(P_Manufacturer, rs.wasNull() ? null : manufacturerFkey);

        this.changedFlag = false;
        this.newFlag = false;
    }
}
 
