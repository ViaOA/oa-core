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
    lowerName = "catalog",
    pluralName = "Catalogs",
    shortName = "ctl",
    displayName = "Catalog",
    displayProperty = "id",
    noPojo = true
)
@OATable(
)
public class Catalog extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(Catalog.class.getName());

    public static final String P_Id = "id";
    public static final String P_Created = "created";
     
    public static final String P_CatalogCategories = "catalogCategories";
     
    protected volatile int id;
    protected volatile OADateTime created;
     
    // Links to other objects.
    protected transient Hub<CatalogCategory> hubCatalogCategories;
     
    public Catalog() {
        if (!isLoading()) setObjectDefaults();
    }
    @Override
    public void setObjectDefaults() {
        setCreated(new OADateTime());
    }
     
    public Catalog(int id) {
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

    @OAMany(
        displayName = "Catalog Categories", 
        toClass = CatalogCategory.class, 
        recursive = false, 
        reverseName = CatalogCategory.P_Catalog
    )
    public Hub<CatalogCategory> getCatalogCategories() {
        if (hubCatalogCategories == null) {
            hubCatalogCategories = (Hub<CatalogCategory>) getHub(P_CatalogCategories);
        }
        return hubCatalogCategories;
    }
    public void load(ResultSet rs, int id) throws SQLException {
        this.id = id;
        java.sql.Timestamp timestamp;
        timestamp = rs.getTimestamp(2);
        if (timestamp != null) this.created = new OADateTime(timestamp);

        this.changedFlag = false;
        this.newFlag = false;
    }
}
 
