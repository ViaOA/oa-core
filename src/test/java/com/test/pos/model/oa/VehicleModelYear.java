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
    lowerName = "vehicleModelYear",
    pluralName = "VehicleModelYears",
    shortName = "vmy",
    displayName = "Vehicle Model Year",
    displayProperty = "id",
    noPojo = true
)
@OATable(
    indexes = {
        @OAIndex(name = "VehicleModelYearVehicleModel", fkey = true, columns = { @OAIndexColumn(name = "VehicleModelId") })
    }
)
public class VehicleModelYear extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(VehicleModelYear.class.getName());

    public static final String P_Id = "id";
    public static final String P_Created = "created";
     
    public static final String P_VehicleModel = "vehicleModel";
    public static final String P_VehicleModelId = "vehicleModelId"; // fkey
    public static final String P_VehicleModelPackages = "vehicleModelPackages";
    public static final String P_VinLookups = "vinLookups";
     
    protected volatile int id;
    protected volatile OADateTime created;
     
    // Links to other objects.
    protected volatile transient VehicleModel vehicleModel;
    protected transient Hub<VehicleModelPackage> hubVehicleModelPackages;
     
    public VehicleModelYear() {
        if (!isLoading()) setObjectDefaults();
    }
    @Override
    public void setObjectDefaults() {
        setCreated(new OADateTime());
    }
     
    public VehicleModelYear(int id) {
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

    @OAOne(
        displayName = "Vehicle Model", 
        reverseName = VehicleModel.P_VehicleModelYears, 
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

    @OAMany(
        displayName = "Vehicle Model Packages", 
        toClass = VehicleModelPackage.class, 
        reverseName = VehicleModelPackage.P_VehicleModelYear
    )
    public Hub<VehicleModelPackage> getVehicleModelPackages() {
        if (hubVehicleModelPackages == null) {
            hubVehicleModelPackages = (Hub<VehicleModelPackage>) getHub(P_VehicleModelPackages);
        }
        return hubVehicleModelPackages;
    }

    @OAMany(
        displayName = "Vin Lookups", 
        toClass = VinLookup.class, 
        reverseName = VinLookup.P_VehicleModelYear, 
        createMethod = false
    )
    private Hub<VinLookup> getVinLookups() {
        // oamodel has createMethod set to false, this method exists only for annotations.
        return null;
    }
    public void load(ResultSet rs, int id) throws SQLException {
        this.id = id;
        java.sql.Timestamp timestamp;
        timestamp = rs.getTimestamp(2);
        if (timestamp != null) this.created = new OADateTime(timestamp);
        int vehicleModelFkey = rs.getInt(3);
        setFkeyProperty(P_VehicleModel, rs.wasNull() ? null : vehicleModelFkey);

        this.changedFlag = false;
        this.newFlag = false;
    }
}
 
