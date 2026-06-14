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
    lowerName = "storeDayEnd",
    pluralName = "StoreDayEnds",
    shortName = "sde",
    displayName = "Store Day End",
    displayProperty = "id",
    noPojo = true
)
@OATable(
    indexes = {
        @OAIndex(name = "StoreDayEndStoreSchedule", fkey = true, columns = { @OAIndexColumn(name = "StoreScheduleId") })
    }
)
public class StoreDayEnd extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(StoreDayEnd.class.getName());

    public static final String P_Id = "id";
    public static final String P_Created = "created";
    public static final String P_PettyCash = "pettyCash";
     
    public static final String P_StoreSchedule = "storeSchedule";
    public static final String P_StoreScheduleId = "storeScheduleId"; // fkey
     
    protected volatile int id;
    protected volatile OADateTime created;
    protected volatile double pettyCash;
     
    // Links to other objects.
    protected volatile transient StoreSchedule storeSchedule;
     
    public StoreDayEnd() {
        if (!isLoading()) setObjectDefaults();
    }
    @Override
    public void setObjectDefaults() {
        setCreated(new OADateTime());
    }
     
    public StoreDayEnd(int id) {
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

    @OAProperty(lowerName = "pettyCash", displayName = "Petty Cash", decimalPlaces = 2, isCurrency = true, displayLength = 9, uiColumnLength = 10)
    @OAColumn(name = "PettyCash", sqlType = java.sql.Types.NUMERIC)
    public double getPettyCash() {
        return pettyCash;
    }
    public void setPettyCash(double newValue) {
        double old = pettyCash;
        fireBeforePropertyChange(P_PettyCash, old, newValue);
        this.pettyCash = newValue;
        firePropertyChange(P_PettyCash, old, this.pettyCash);
    }

    @OAOne(
        displayName = "Store Schedule", 
        reverseName = StoreSchedule.P_StoreDayEnd, 
        required = true, 
        allowCreateNew = false, 
        allowAddExisting = false, 
        fkeys = {@OAFkey(fromProperty = P_StoreScheduleId, toProperty = StoreSchedule.P_Id)}
    )
    public StoreSchedule getStoreSchedule() {
        if (storeSchedule == null) {
            storeSchedule = (StoreSchedule) getObject(P_StoreSchedule);
        }
        return storeSchedule;
    }
    public void setStoreSchedule(StoreSchedule newValue) {
        StoreSchedule old = this.storeSchedule;
        fireBeforePropertyChange(P_StoreSchedule, old, newValue);
        this.storeSchedule = newValue;
        firePropertyChange(P_StoreSchedule, old, this.storeSchedule);
    }
    @OAProperty(isFkeyOnly = true)
    @OAColumn(name = "StoreScheduleId")
    public Integer getStoreScheduleId() {
        return (Integer) getFkeyProperty(P_StoreScheduleId);
    }
    public void setStoreScheduleId(Integer newValue) {
        this.storeSchedule = null;
        setFkeyProperty(P_StoreScheduleId, newValue);
    }
    public void load(ResultSet rs, int id) throws SQLException {
        this.id = id;
        java.sql.Timestamp timestamp;
        timestamp = rs.getTimestamp(2);
        if (timestamp != null) this.created = new OADateTime(timestamp);
        this.pettyCash = rs.getDouble(3);
        setPrimitiveNull(P_PettyCash, rs.wasNull());
        int storeScheduleFkey = rs.getInt(4);
        setFkeyProperty(P_StoreSchedule, rs.wasNull() ? null : storeScheduleFkey);

        this.changedFlag = false;
        this.newFlag = false;
    }
}
 
