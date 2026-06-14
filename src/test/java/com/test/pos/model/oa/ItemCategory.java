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
    lowerName = "itemCategory",
    pluralName = "ItemCategories",
    shortName = "itc",
    displayName = "Item Category",
    isLookup = true,
    isPreSelect = true,
    displayProperty = "code",
    noPojo = true
)
@OATable(
    indexes = {
        @OAIndex(name = "ItemCategoryParentItemCategory", fkey = true, columns = { @OAIndexColumn(name = "ParentItemCategoryId") }), 
        @OAIndex(name = "ItemCategoryVertexTaxCode", fkey = true, columns = { @OAIndexColumn(name = "VertexTaxCodeId") })
    }
)
public class ItemCategory extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(ItemCategory.class.getName());

    public static final String P_Id = "id";
    public static final String P_Created = "created";
    public static final String P_Code = "code";
    public static final String P_Name = "name";
     
    public static final String P_Items = "items";
    public static final String P_ItemsId = "itemsId"; // fkey
    public static final String P_ParentItemCategory = "parentItemCategory";
    public static final String P_ParentItemCategoryId = "parentItemCategoryId"; // fkey
    public static final String P_SubItemCategories = "subItemCategories";
    public static final String P_VertexTaxCode = "vertexTaxCode";
    public static final String P_VertexTaxCodeId = "vertexTaxCodeId"; // fkey
     
    protected volatile int id;
    protected volatile OADateTime created;
    protected volatile String code;
    protected volatile String name;
     
    // Links to other objects.
    protected transient Hub<Item> hubItems;
    protected volatile transient ItemCategory parentItemCategory;
    protected transient Hub<ItemCategory> hubSubItemCategories;
    protected volatile transient VertexTaxCode vertexTaxCode;
     
    public ItemCategory() {
        if (!isLoading()) setObjectDefaults();
    }
    @Override
    public void setObjectDefaults() {
        setCreated(new OADateTime());
    }
     
    public ItemCategory(int id) {
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

    @OAMany(
        toClass = Item.class, 
        reverseName = Item.P_ItemCategories
    )
    @OALinkTable(name = "ItemCategoryItem", indexName = "ItemItemCategory", columns = {"ItemCategoryId"})
    public Hub<Item> getItems() {
        if (hubItems == null) {
            hubItems = (Hub<Item>) getHub(P_Items);
        }
        return hubItems;
    }

    @OAOne(
        displayName = "Parent Item Category", 
        reverseName = ItemCategory.P_SubItemCategories, 
        allowCreateNew = false, 
        fkeys = {@OAFkey(fromProperty = P_ParentItemCategoryId, toProperty = ItemCategory.P_Id)}
    )
    public ItemCategory getParentItemCategory() {
        if (parentItemCategory == null) {
            parentItemCategory = (ItemCategory) getObject(P_ParentItemCategory);
        }
        return parentItemCategory;
    }
    public void setParentItemCategory(ItemCategory newValue) {
        ItemCategory old = this.parentItemCategory;
        fireBeforePropertyChange(P_ParentItemCategory, old, newValue);
        this.parentItemCategory = newValue;
        firePropertyChange(P_ParentItemCategory, old, this.parentItemCategory);
    }
    @OAProperty(isFkeyOnly = true)
    @OAColumn(name = "ParentItemCategoryId")
    public Integer getParentItemCategoryId() {
        return (Integer) getFkeyProperty(P_ParentItemCategoryId);
    }
    public void setParentItemCategoryId(Integer newValue) {
        this.parentItemCategory = null;
        setFkeyProperty(P_ParentItemCategoryId, newValue);
    }

    @OAMany(
        displayName = "Sub Item Categories", 
        toClass = ItemCategory.class, 
        recursive = true, 
        reverseName = ItemCategory.P_ParentItemCategory
    )
    public Hub<ItemCategory> getSubItemCategories() {
        if (hubSubItemCategories == null) {
            hubSubItemCategories = (Hub<ItemCategory>) getHub(P_SubItemCategories);
        }
        return hubSubItemCategories;
    }

    @OAOne(
        displayName = "Vertex Tax Code", 
        reverseName = VertexTaxCode.P_RootItemCategories, 
        allowCreateNew = false, 
        fkeys = {@OAFkey(fromProperty = P_VertexTaxCodeId, toProperty = VertexTaxCode.P_Id)}
    )
    public VertexTaxCode getVertexTaxCode() {
        if (vertexTaxCode == null) {
            vertexTaxCode = (VertexTaxCode) getObject(P_VertexTaxCode);
        }
        return vertexTaxCode;
    }
    public void setVertexTaxCode(VertexTaxCode newValue) {
        VertexTaxCode old = this.vertexTaxCode;
        fireBeforePropertyChange(P_VertexTaxCode, old, newValue);
        this.vertexTaxCode = newValue;
        firePropertyChange(P_VertexTaxCode, old, this.vertexTaxCode);
    }
    @OAProperty(isFkeyOnly = true)
    @OAColumn(name = "VertexTaxCodeId")
    public Integer getVertexTaxCodeId() {
        return (Integer) getFkeyProperty(P_VertexTaxCodeId);
    }
    public void setVertexTaxCodeId(Integer newValue) {
        this.vertexTaxCode = null;
        setFkeyProperty(P_VertexTaxCodeId, newValue);
    }
    public void load(ResultSet rs, int id) throws SQLException {
        this.id = id;
        java.sql.Timestamp timestamp;
        timestamp = rs.getTimestamp(2);
        if (timestamp != null) this.created = new OADateTime(timestamp);
        this.code = rs.getString(3);
        this.name = rs.getString(4);
        int parentItemCategoryFkey = rs.getInt(5);
        setFkeyProperty(P_ParentItemCategory, rs.wasNull() ? null : parentItemCategoryFkey);
        int vertexTaxCodeFkey = rs.getInt(6);
        setFkeyProperty(P_VertexTaxCode, rs.wasNull() ? null : vertexTaxCodeFkey);

        this.changedFlag = false;
        this.newFlag = false;
    }
}
 
