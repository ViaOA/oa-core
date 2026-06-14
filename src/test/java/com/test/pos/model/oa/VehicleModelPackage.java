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
    lowerName = "vehicleModelPackage",
    pluralName = "VehicleModelPackages",
    shortName = "vmp",
    displayName = "Vehicle Model Package",
    displayProperty = "id",
    noPojo = true
)
@OATable(
    indexes = {
        @OAIndex(name = "VehicleModelPackageVehicleModel", fkey = true, columns = { @OAIndexColumn(name = "VehicleModelId") }), 
        @OAIndex(name = "VehicleModelPackageVehicleModelYear", fkey = true, columns = { @OAIndexColumn(name = "VehicleModelYearId") }), 
        @OAIndex(name = "VehicleModelPackageVinLookup", fkey = true, columns = { @OAIndexColumn(name = "VinLookupId") })
    }
)
public class VehicleModelPackage extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(VehicleModelPackage.class.getName());

    public static final String P_Id = "id";
    public static final String P_Created = "created";
     
    public static final String P_CatalogItems = "catalogItems";
    public static final String P_CatalogItemsId = "catalogItemsId"; // fkey
    public static final String P_GarageVehicles = "garageVehicles";
    public static final String P_VehicleModel = "vehicleModel";
    public static final String P_VehicleModelId = "vehicleModelId"; // fkey
    public static final String P_VehicleModelYear = "vehicleModelYear";
    public static final String P_VehicleModelYearId = "vehicleModelYearId"; // fkey
    public static final String P_VinLookup = "vinLookup";
    public static final String P_VinLookupId = "vinLookupId"; // fkey
     
    protected volatile int id;
    protected volatile OADateTime created;
     
    // Links to other objects.
    protected volatile transient VehicleModel vehicleModel;
    protected volatile transient VehicleModelYear vehicleModelYear;
    protected volatile transient VinLookup vinLookup;
     
    public VehicleModelPackage() {
        if (!isLoading()) setObjectDefaults();
    }
    @Override
    public void setObjectDefaults() {
        setCreated(new OADateTime());
    }
     
    public VehicleModelPackage(int id) {
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
        displayName = "Catalog Items", 
        toClass = CatalogItem.class, 
        reverseName = CatalogItem.P_VehicleModelPackages, 
        createMethod = false
    )
    @OALinkTable(name = "New_45VehicleModelPackage", indexName = "New_45VehicleModelPackage", columns = {"VehicleModelPackageId"})
    private Hub<CatalogItem> getCatalogItems() {
        // oamodel has createMethod set to false, this method exists only for annotations.
        return null;
    }

    @OAMany(
        displayName = "Garage Vehicles", 
        toClass = GarageVehicle.class, 
        reverseName = GarageVehicle.P_VehicleModelPackage, 
        createMethod = false
    )
    private Hub<GarageVehicle> getGarageVehicles() {
        // oamodel has createMethod set to false, this method exists only for annotations.
        return null;
    }

    @OAOne(
        displayName = "Vehicle Model", 
        reverseName = VehicleModel.P_VehicleModelPackages, 
        required = true, 
        allowCreateNew = false, 
        fkeys = {@OAFkey(fromProperty = P_VehicleModelId, toProperty = VehicleModel.P_Id)}
    )
    public VehicleModel getVehicleModel() {
        if (vehicleModel == null) {
            vehicleModel = (VehicleModel) getObject(P_VehicleModel);
        }
        return vehicleModel;
    }
    public void setVehicleModel(VehicleModel newValue) {
        VehicleModel old = this.vehicleModel;
        fireBeforePropertyChange(P_VehicleModel, old, newValue);
        this.vehicleModel = newValue;
        firePropertyChange(P_VehicleModel, old, this.vehicleModel);
    }
    @OAProperty(isFkeyOnly = true)
    @OAColumn(name = "VehicleModelId")
    public Integer getVehicleModelId() {
        return (Integer) getFkeyProperty(P_VehicleModelId);
    }
    public void setVehicleModelId(Integer newValue) {
        this.vehicleModel = null;
        setFkeyProperty(P_VehicleModelId, newValue);
    }

    @OAOne(
        displayName = "Vehicle Model Year", 
        reverseName = VehicleModelYear.P_VehicleModelPackages, 
        allowCreateNew = false, 
        fkeys = {@OAFkey(fromProperty = P_VehicleModelYearId, toProperty = VehicleModelYear.P_Id)}
    )
    public VehicleModelYear getVehicleModelYear() {
        if (vehicleModelYear == null) {
            vehicleModelYear = (VehicleModelYear) getObject(P_VehicleModelYear);
        }
        return vehicleModelYear;
    }
    public void setVehicleModelYear(VehicleModelYear newValue) {
        VehicleModelYear old = this.vehicleModelYear;
        fireBeforePropertyChange(P_VehicleModelYear, old, newValue);
        this.vehicleModelYear = newValue;
        firePropertyChange(P_VehicleModelYear, old, this.vehicleModelYear);
    }
    @OAProperty(isFkeyOnly = true)
    @OAColumn(name = "VehicleModelYearId")
    public Integer getVehicleModelYearId() {
        return (Integer) getFkeyProperty(P_VehicleModelYearId);
    }
    public void setVehicleModelYearId(Integer newValue) {
        this.vehicleModelYear = null;
        setFkeyProperty(P_VehicleModelYearId, newValue);
    }

    @OAOne(
        displayName = "Vin Lookup", 
        reverseName = VinLookup.P_VehicleModelPackages, 
        allowCreateNew = false, 
        fkeys = {@OAFkey(fromProperty = P_VinLookupId, toProperty = VinLookup.P_Id)}
    )
    public VinLookup getVinLookup() {
        if (vinLookup == null) {
            vinLookup = (VinLookup) getObject(P_VinLookup);
        }
        return vinLookup;
    }
    public void setVinLookup(VinLookup newValue) {
        VinLookup old = this.vinLookup;
        fireBeforePropertyChange(P_VinLookup, old, newValue);
        this.vinLookup = newValue;
        firePropertyChange(P_VinLookup, old, this.vinLookup);
    }
    @OAProperty(isFkeyOnly = true)
    @OAColumn(name = "VinLookupId")
    public Integer getVinLookupId() {
        return (Integer) getFkeyProperty(P_VinLookupId);
    }
    public void setVinLookupId(Integer newValue) {
        this.vinLookup = null;
        setFkeyProperty(P_VinLookupId, newValue);
    }
    public void load(ResultSet rs, int id) throws SQLException {
        this.id = id;
        java.sql.Timestamp timestamp;
        timestamp = rs.getTimestamp(2);
        if (timestamp != null) this.created = new OADateTime(timestamp);
        int vehicleModelFkey = rs.getInt(3);
        setFkeyProperty(P_VehicleModel, rs.wasNull() ? null : vehicleModelFkey);
        int vehicleModelYearFkey = rs.getInt(4);
        setFkeyProperty(P_VehicleModelYear, rs.wasNull() ? null : vehicleModelYearFkey);
        int vinLookupFkey = rs.getInt(5);
        setFkeyProperty(P_VinLookup, rs.wasNull() ? null : vinLookupFkey);

        this.changedFlag = false;
        this.newFlag = false;
    }
}
 
