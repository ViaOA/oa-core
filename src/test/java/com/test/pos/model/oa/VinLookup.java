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
    lowerName = "vinLookup",
    pluralName = "VinLookups",
    shortName = "vnl",
    displayName = "VIN Lookup",
    displayProperty = "id",
    singleton = true,
    pojoSingleton = true,
    noPojo = true
)
@OATable(
    indexes = {
        @OAIndex(name = "VinLookupVehicleModelYear", fkey = true, columns = { @OAIndexColumn(name = "VehicleModelYearId") })
    }
)
public class VinLookup extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(VinLookup.class.getName());

    public static final String P_Id = "id";
    public static final String P_Created = "created";
    public static final String P_Vin = "vin";
     
    public static final String P_VehicleModelPackages = "vehicleModelPackages";
    public static final String P_VehicleModelYear = "vehicleModelYear";
    public static final String P_VehicleModelYearId = "vehicleModelYearId"; // fkey
     
    protected volatile int id;
    protected volatile OADateTime created;
    protected volatile String vin;
     
    // Links to other objects.
    protected volatile transient VehicleModelYear vehicleModelYear;
     
    public VinLookup() {
        if (!isLoading()) setObjectDefaults();
    }
    @Override
    public void setObjectDefaults() {
        setCreated(new OADateTime());
    }
     
    public VinLookup(int id) {
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

    @OAProperty(lowerName = "vin", maxLength = 75, displayLength = 22, uiColumnLength = 20)
    @OAColumn(name = "Vin", maxLength = 75)
    public String getVin() {
        return vin;
    }
    public void setVin(String newValue) {
        String old = vin;
        fireBeforePropertyChange(P_Vin, old, newValue);
        this.vin = newValue;
        firePropertyChange(P_Vin, old, this.vin);
    }

    @OAMany(
        displayName = "Vehicle Model Packages", 
        toClass = VehicleModelPackage.class, 
        reverseName = VehicleModelPackage.P_VinLookup, 
        createMethod = false
    )
    private Hub<VehicleModelPackage> getVehicleModelPackages() {
        // oamodel has createMethod set to false, this method exists only for annotations.
        return null;
    }

    @OAOne(
        displayName = "Vehicle Model Year", 
        reverseName = VehicleModelYear.P_VinLookups, 
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
    public void load(ResultSet rs, int id) throws SQLException {
        this.id = id;
        java.sql.Timestamp timestamp;
        timestamp = rs.getTimestamp(2);
        if (timestamp != null) this.created = new OADateTime(timestamp);
        this.vin = rs.getString(3);
        int vehicleModelYearFkey = rs.getInt(4);
        setFkeyProperty(P_VehicleModelYear, rs.wasNull() ? null : vehicleModelYearFkey);

        this.changedFlag = false;
        this.newFlag = false;
    }
}
 
