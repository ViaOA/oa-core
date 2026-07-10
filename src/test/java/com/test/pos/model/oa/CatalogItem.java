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
    lowerName = "catalogItem",
    pluralName = "CatalogItems",
    shortName = "cti",
    displayName = "Catalog Item",
    displayProperty = "name",
    noPojo = true
)
@OATable(
    name = "New_45",
    indexes = {
        @OAIndex(name = "New_45Item", fkey = true, columns = { @OAIndexColumn(name = "ItemId") })
    }
)
public class CatalogItem extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(CatalogItem.class.getName());

    public static final String P_Id = "id";
    public static final String P_Created = "created";
    public static final String P_Name = "name";
     
    public static final String P_Item = "item";
    public static final String P_ItemId = "itemId"; // fkey
    public static final String P_RootCatalogCategories = "rootCatalogCategories";
    public static final String P_RootCatalogCategoriesId = "rootCatalogCategoriesId"; // fkey
    public static final String P_VehicleModelPackages = "vehicleModelPackages";
    public static final String P_VehicleModelPackagesId = "vehicleModelPackagesId"; // fkey
    public static final String P_VehicleModels = "vehicleModels";
    public static final String P_VehicleModelsId = "vehicleModelsId"; // fkey
     
    protected volatile int id;
    protected volatile OADateTime created;
    protected volatile String name;
     
    // Links to other objects.
    protected volatile transient Item item;
    protected transient Hub<CatalogCategory> hubRootCatalogCategories;
    protected transient Hub<VehicleModelPackage> hubVehicleModelPackages;
    protected transient Hub<VehicleModel> hubVehicleModels;
     
    public CatalogItem() {
        if (!isLoading()) setObjectDefaults();
    }
    @Override
    public void setObjectDefaults() {
        setCreated(new OADateTime());
    }
     
    public CatalogItem(int id) {
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
        reverseName = Item.P_CatalogItems, 
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

    @OAMany(
        displayName = "Root Catalog Categories", 
        toClass = CatalogCategory.class, 
        recursive = false, 
        reverseName = CatalogCategory.P_CatalogItems
    )
    @OALinkTable(name = "CatalogCategoryNew_45", indexName = "CatalogCategoryCatalogItem", columns = {"New_45Id"})
    public Hub<CatalogCategory> getRootCatalogCategories() {
        if (hubRootCatalogCategories == null) {
            hubRootCatalogCategories = (Hub<CatalogCategory>) getHub(P_RootCatalogCategories);
        }
        return hubRootCatalogCategories;
    }

    @OAMany(
        displayName = "Vehicle Model Packages", 
        toClass = VehicleModelPackage.class, 
        reverseName = VehicleModelPackage.P_CatalogItems, 
        selectFromPath = P_VehicleModels + "." + VehicleModel.P_VehicleModelPackages
    )
    @OALinkTable(name = "New_45VehicleModelPackage", indexName = "ModelPackageCatalogItem", columns = {"New_45Id"})
    public Hub<VehicleModelPackage> getVehicleModelPackages() {
        if (hubVehicleModelPackages == null) {
            hubVehicleModelPackages = (Hub<VehicleModelPackage>) getHub(P_VehicleModelPackages);
        }
        return hubVehicleModelPackages;
    }

    @OAMany(
        displayName = "Vehicle Models", 
        toClass = VehicleModel.class, 
        reverseName = VehicleModel.P_CatalogItems
    )
    @OALinkTable(name = "New_45VehicleModel", indexName = "ModelCatalogItem", columns = {"New_45Id"})
    public Hub<VehicleModel> getVehicleModels() {
        if (hubVehicleModels == null) {
            hubVehicleModels = (Hub<VehicleModel>) getHub(P_VehicleModels);
        }
        return hubVehicleModels;
    }
    public void load(ResultSet rs, int id) throws SQLException {
        this.id = id;
        java.sql.Timestamp timestamp;
        timestamp = rs.getTimestamp(2);
        if (timestamp != null) this.created = new OADateTime(timestamp);
        this.name = rs.getString(3);
        int itemFkey = rs.getInt(4);
        setFkeyProperty(P_Item, rs.wasNull() ? null : itemFkey);

        this.changedFlag = false;
        this.newFlag = false;
    }
}
 
