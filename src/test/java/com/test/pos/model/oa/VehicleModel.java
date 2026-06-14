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
    lowerName = "vehicleModel",
    pluralName = "VehicleModels",
    shortName = "vhm",
    displayName = "Vehicle Model",
    displayProperty = "id",
    noPojo = true
)
@OATable(
    indexes = {
        @OAIndex(name = "VehicleModelVehicleMake", fkey = true, columns = { @OAIndexColumn(name = "VehicleMakeId") })
    }
)
public class VehicleModel extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(VehicleModel.class.getName());

    public static final String P_Id = "id";
    public static final String P_Created = "created";
     
    public static final String P_CatalogItems = "catalogItems";
    public static final String P_CatalogItemsId = "catalogItemsId"; // fkey
    public static final String P_GarageVehicles = "garageVehicles";
    public static final String P_VehicleMake = "vehicleMake";
    public static final String P_VehicleMakeId = "vehicleMakeId"; // fkey
    public static final String P_VehicleModelPackages = "vehicleModelPackages";
    public static final String P_VehicleModelYears = "vehicleModelYears";
     
    protected volatile int id;
    protected volatile OADateTime created;
     
    // Links to other objects.
    protected volatile transient VehicleMake vehicleMake;
    protected transient Hub<VehicleModelPackage> hubVehicleModelPackages;
    protected transient Hub<VehicleModelYear> hubVehicleModelYears;
     
    public VehicleModel() {
        if (!isLoading()) setObjectDefaults();
    }
    @Override
    public void setObjectDefaults() {
        setCreated(new OADateTime());
    }
     
    public VehicleModel(int id) {
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
        reverseName = CatalogItem.P_VehicleModels, 
        createMethod = false
    )
    @OALinkTable(name = "New_45VehicleModel", indexName = "New_45VehicleModel", columns = {"VehicleModelId"})
    private Hub<CatalogItem> getCatalogItems() {
        // oamodel has createMethod set to false, this method exists only for annotations.
        return null;
    }

    @OAMany(
        displayName = "Garage Vehicles", 
        toClass = GarageVehicle.class, 
        reverseName = GarageVehicle.P_VehicleModel, 
        createMethod = false
    )
    private Hub<GarageVehicle> getGarageVehicles() {
        // oamodel has createMethod set to false, this method exists only for annotations.
        return null;
    }

    @OAOne(
        displayName = "Vehicle Make", 
        reverseName = VehicleMake.P_VehicleModels, 
        required = true, 
        allowCreateNew = false, 
        fkeys = {@OAFkey(fromProperty = P_VehicleMakeId, toProperty = VehicleMake.P_Id)}
    )
    public VehicleMake getVehicleMake() {
        if (vehicleMake == null) {
            vehicleMake = (VehicleMake) getObject(P_VehicleMake);
        }
        return vehicleMake;
    }
    public void setVehicleMake(VehicleMake newValue) {
        VehicleMake old = this.vehicleMake;
        fireBeforePropertyChange(P_VehicleMake, old, newValue);
        this.vehicleMake = newValue;
        firePropertyChange(P_VehicleMake, old, this.vehicleMake);
    }
    @OAProperty(isFkeyOnly = true)
    @OAColumn(name = "VehicleMakeId")
    public Integer getVehicleMakeId() {
        return (Integer) getFkeyProperty(P_VehicleMakeId);
    }
    public void setVehicleMakeId(Integer newValue) {
        this.vehicleMake = null;
        setFkeyProperty(P_VehicleMakeId, newValue);
    }

    @OAMany(
        displayName = "Vehicle Model Packages", 
        toClass = VehicleModelPackage.class, 
        owner = true, 
        reverseName = VehicleModelPackage.P_VehicleModel, 
        cascadeSave = true, 
        cascadeDelete = true
    )
    public Hub<VehicleModelPackage> getVehicleModelPackages() {
        if (hubVehicleModelPackages == null) {
            hubVehicleModelPackages = (Hub<VehicleModelPackage>) getHub(P_VehicleModelPackages);
        }
        return hubVehicleModelPackages;
    }

    @OAMany(
        displayName = "Vehicle Model Years", 
        toClass = VehicleModelYear.class, 
        reverseName = VehicleModelYear.P_VehicleModel
    )
    public Hub<VehicleModelYear> getVehicleModelYears() {
        if (hubVehicleModelYears == null) {
            hubVehicleModelYears = (Hub<VehicleModelYear>) getHub(P_VehicleModelYears);
        }
        return hubVehicleModelYears;
    }
    public void load(ResultSet rs, int id) throws SQLException {
        this.id = id;
        java.sql.Timestamp timestamp;
        timestamp = rs.getTimestamp(2);
        if (timestamp != null) this.created = new OADateTime(timestamp);
        int vehicleMakeFkey = rs.getInt(3);
        setFkeyProperty(P_VehicleMake, rs.wasNull() ? null : vehicleMakeFkey);

        this.changedFlag = false;
        this.newFlag = false;
    }
}
 
