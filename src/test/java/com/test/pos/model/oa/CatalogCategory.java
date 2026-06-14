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
    lowerName = "catalogCategory",
    pluralName = "CatalogCategories",
    shortName = "ctc",
    displayName = "Catalog Category",
    displayProperty = "name",
    noPojo = true
)
@OATable(
    indexes = {
        @OAIndex(name = "CatalogCategoryCatalog", fkey = true, columns = { @OAIndexColumn(name = "CatalogId") }), 
        @OAIndex(name = "CatalogCategoryParentCatalogCategory", fkey = true, columns = { @OAIndexColumn(name = "ParentCatalogCategoryId") })
    }
)
public class CatalogCategory extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(CatalogCategory.class.getName());

    public static final String P_Id = "id";
    public static final String P_Created = "created";
    public static final String P_Name = "name";
     
    public static final String P_Catalog = "catalog";
    public static final String P_CatalogId = "catalogId"; // fkey
    public static final String P_CatalogCategories = "catalogCategories";
    public static final String P_CatalogItems = "catalogItems";
    public static final String P_CatalogItemsId = "catalogItemsId"; // fkey
    public static final String P_ParentCatalogCategory = "parentCatalogCategory";
    public static final String P_ParentCatalogCategoryId = "parentCatalogCategoryId"; // fkey
     
    protected volatile int id;
    protected volatile OADateTime created;
    protected volatile String name;
     
    // Links to other objects.
    protected volatile transient Catalog catalog;
    protected transient Hub<CatalogCategory> hubCatalogCategories;
    protected transient Hub<CatalogItem> hubCatalogItems;
    protected volatile transient CatalogCategory parentCatalogCategory;
     
    public CatalogCategory() {
        if (!isLoading()) setObjectDefaults();
    }
    @Override
    public void setObjectDefaults() {
        setCreated(new OADateTime());
    }
     
    public CatalogCategory(int id) {
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
        reverseName = Catalog.P_CatalogCategories, 
        allowCreateNew = false, 
        fkeys = {@OAFkey(fromProperty = P_CatalogId, toProperty = Catalog.P_Id)}
    )
    public Catalog getCatalog() {
        if (catalog == null) {
            catalog = (Catalog) getObject(P_Catalog);
        }
        return catalog;
    }
    public void setCatalog(Catalog newValue) {
        Catalog old = this.catalog;
        fireBeforePropertyChange(P_Catalog, old, newValue);
        this.catalog = newValue;
        firePropertyChange(P_Catalog, old, this.catalog);
    }
    @OAProperty(isFkeyOnly = true)
    @OAColumn(name = "CatalogId")
    public Integer getCatalogId() {
        return (Integer) getFkeyProperty(P_CatalogId);
    }
    public void setCatalogId(Integer newValue) {
        this.catalog = null;
        setFkeyProperty(P_CatalogId, newValue);
    }

    @OAMany(
        displayName = "Catalog Categories", 
        toClass = CatalogCategory.class, 
        recursive = true, 
        reverseName = CatalogCategory.P_ParentCatalogCategory
    )
    public Hub<CatalogCategory> getCatalogCategories() {
        if (hubCatalogCategories == null) {
            hubCatalogCategories = (Hub<CatalogCategory>) getHub(P_CatalogCategories);
        }
        return hubCatalogCategories;
    }

    @OAMany(
        displayName = "Catalog Items", 
        toClass = CatalogItem.class, 
        reverseName = CatalogItem.P_RootCatalogCategories
    )
    @OALinkTable(name = "CatalogCategoryNew_45", indexName = "New_45RootCatalogCategory", columns = {"CatalogCategoryId"})
    public Hub<CatalogItem> getCatalogItems() {
        if (hubCatalogItems == null) {
            hubCatalogItems = (Hub<CatalogItem>) getHub(P_CatalogItems);
        }
        return hubCatalogItems;
    }

    @OAOne(
        displayName = "Parent Catalog Category", 
        reverseName = CatalogCategory.P_CatalogCategories, 
        allowCreateNew = false, 
        fkeys = {@OAFkey(fromProperty = P_ParentCatalogCategoryId, toProperty = CatalogCategory.P_Id)}
    )
    public CatalogCategory getParentCatalogCategory() {
        if (parentCatalogCategory == null) {
            parentCatalogCategory = (CatalogCategory) getObject(P_ParentCatalogCategory);
        }
        return parentCatalogCategory;
    }
    public void setParentCatalogCategory(CatalogCategory newValue) {
        CatalogCategory old = this.parentCatalogCategory;
        fireBeforePropertyChange(P_ParentCatalogCategory, old, newValue);
        this.parentCatalogCategory = newValue;
        firePropertyChange(P_ParentCatalogCategory, old, this.parentCatalogCategory);
    }
    @OAProperty(isFkeyOnly = true)
    @OAColumn(name = "ParentCatalogCategoryId")
    public Integer getParentCatalogCategoryId() {
        return (Integer) getFkeyProperty(P_ParentCatalogCategoryId);
    }
    public void setParentCatalogCategoryId(Integer newValue) {
        this.parentCatalogCategory = null;
        setFkeyProperty(P_ParentCatalogCategoryId, newValue);
    }
    public void load(ResultSet rs, int id) throws SQLException {
        this.id = id;
        java.sql.Timestamp timestamp;
        timestamp = rs.getTimestamp(2);
        if (timestamp != null) this.created = new OADateTime(timestamp);
        this.name = rs.getString(3);
        int catalogFkey = rs.getInt(4);
        setFkeyProperty(P_Catalog, rs.wasNull() ? null : catalogFkey);
        int parentCatalogCategoryFkey = rs.getInt(5);
        setFkeyProperty(P_ParentCatalogCategory, rs.wasNull() ? null : parentCatalogCategoryFkey);

        this.changedFlag = false;
        this.newFlag = false;
    }
}
 
