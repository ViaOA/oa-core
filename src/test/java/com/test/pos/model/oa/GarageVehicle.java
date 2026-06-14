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
    lowerName = "garageVehicle",
    pluralName = "GarageVehicles",
    shortName = "grv",
    displayName = "Garage Vehicle",
    displayProperty = "id",
    noPojo = true
)
@OATable(
    indexes = {
        @OAIndex(name = "GarageVehicleGarage", fkey = true, columns = { @OAIndexColumn(name = "GarageId") }), 
        @OAIndex(name = "GarageVehicleVehicleModel", fkey = true, columns = { @OAIndexColumn(name = "VehicleModelId") }), 
        @OAIndex(name = "GarageVehicleVehicleModelPackage", fkey = true, columns = { @OAIndexColumn(name = "VehicleModelPackageId") })
    }
)
public class GarageVehicle extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(GarageVehicle.class.getName());

    public static final String P_Id = "id";
    public static final String P_Created = "created";
     
    public static final String P_Garage = "garage";
    public static final String P_GarageId = "garageId"; // fkey
    public static final String P_VehicleModel = "vehicleModel";
    public static final String P_VehicleModelId = "vehicleModelId"; // fkey
    public static final String P_VehicleModelPackage = "vehicleModelPackage";
    public static final String P_VehicleModelPackageId = "vehicleModelPackageId"; // fkey
     
    protected volatile int id;
    protected volatile OADateTime created;
     
    // Links to other objects.
    protected volatile transient Garage garage;
    protected volatile transient VehicleModel vehicleModel;
    protected volatile transient VehicleModelPackage vehicleModelPackage;
     
    public GarageVehicle() {
        if (!isLoading()) setObjectDefaults();
    }
    @Override
    public void setObjectDefaults() {
        setCreated(new OADateTime());
    }
     
    public GarageVehicle(int id) {
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
        reverseName = Garage.P_GarageVehicles, 
        required = true, 
        allowCreateNew = false, 
        fkeys = {@OAFkey(fromProperty = P_GarageId, toProperty = Garage.P_Id)}
    )
    public Garage getGarage() {
        if (garage == null) {
            garage = (Garage) getObject(P_Garage);
        }
        return garage;
    }
    public void setGarage(Garage newValue) {
        Garage old = this.garage;
        fireBeforePropertyChange(P_Garage, old, newValue);
        this.garage = newValue;
        firePropertyChange(P_Garage, old, this.garage);
    }
    @OAProperty(isFkeyOnly = true)
    @OAColumn(name = "GarageId")
    public Integer getGarageId() {
        return (Integer) getFkeyProperty(P_GarageId);
    }
    public void setGarageId(Integer newValue) {
        this.garage = null;
        setFkeyProperty(P_GarageId, newValue);
    }

    @OAOne(
        displayName = "Vehicle Model", 
        reverseName = VehicleModel.P_GarageVehicles, 
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
        displayName = "Vehicle Model Package", 
        reverseName = VehicleModelPackage.P_GarageVehicles, 
        allowCreateNew = false, 
        fkeys = {@OAFkey(fromProperty = P_VehicleModelPackageId, toProperty = VehicleModelPackage.P_Id)}
    )
    public VehicleModelPackage getVehicleModelPackage() {
        if (vehicleModelPackage == null) {
            vehicleModelPackage = (VehicleModelPackage) getObject(P_VehicleModelPackage);
        }
        return vehicleModelPackage;
    }
    public void setVehicleModelPackage(VehicleModelPackage newValue) {
        VehicleModelPackage old = this.vehicleModelPackage;
        fireBeforePropertyChange(P_VehicleModelPackage, old, newValue);
        this.vehicleModelPackage = newValue;
        firePropertyChange(P_VehicleModelPackage, old, this.vehicleModelPackage);
    }
    @OAProperty(isFkeyOnly = true)
    @OAColumn(name = "VehicleModelPackageId")
    public Integer getVehicleModelPackageId() {
        return (Integer) getFkeyProperty(P_VehicleModelPackageId);
    }
    public void setVehicleModelPackageId(Integer newValue) {
        this.vehicleModelPackage = null;
        setFkeyProperty(P_VehicleModelPackageId, newValue);
    }
    public void load(ResultSet rs, int id) throws SQLException {
        this.id = id;
        java.sql.Timestamp timestamp;
        timestamp = rs.getTimestamp(2);
        if (timestamp != null) this.created = new OADateTime(timestamp);
        int garageFkey = rs.getInt(3);
        setFkeyProperty(P_Garage, rs.wasNull() ? null : garageFkey);
        int vehicleModelFkey = rs.getInt(4);
        setFkeyProperty(P_VehicleModel, rs.wasNull() ? null : vehicleModelFkey);
        int vehicleModelPackageFkey = rs.getInt(5);
        setFkeyProperty(P_VehicleModelPackage, rs.wasNull() ? null : vehicleModelPackageFkey);

        this.changedFlag = false;
        this.newFlag = false;
    }
}
 
