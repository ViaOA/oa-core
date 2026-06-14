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
import com.viaoa.datetime.OATime;
import com.test.pos.delegate.oa.*;
import com.test.pos.model.oa.filter.*;
import com.test.pos.model.oa.propertypath.*;
 
@OAClass(
    lowerName = "storeHoursOpen",
    pluralName = "StoreHoursOpens",
    shortName = "sho",
    displayName = "Store Hours Open",
    displayProperty = "dayOfWeek",
    sortProperty = "dayOfWeek",
    noPojo = true
)
@OATable(
    indexes = {
        @OAIndex(name = "StoreHoursOpenStore", fkey = true, columns = { @OAIndexColumn(name = "StoreId") })
    }
)
public class StoreHoursOpen extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(StoreHoursOpen.class.getName());

    public static final String P_Id = "id";
    public static final String P_Created = "created";
    public static final String P_DayOfWeek = "dayOfWeek";
    public static final String P_DayOfWeekString = "dayOfWeekString";
    public static final String P_DayOfWeekEnum = "dayOfWeekEnum";
    public static final String P_DayOfWeekDisplay = "dayOfWeekDisplay";
    public static final String P_OpenTime = "openTime";
    public static final String P_CloseTime = "closeTime";
     
    public static final String P_Store = "store";
    public static final String P_StoreId = "storeId"; // fkey
     
    protected volatile int id;
    protected volatile OADateTime created;
    protected volatile int dayOfWeek;

    public static enum DayOfWeek {
        Sunday("Sunday"),
        Monday("Monday"),
        Tuesday("Tuesday"),
        Wednesday("Wednesday"),
        Thursday("Thursday"),
        Friday("Friday"),
        Saturday("Saturday");

        private String display;
        DayOfWeek(String display) {
            this.display = display;
        }

        public String getDisplay() {
            return display;
        }
    }
    public static final int DAYOFWEEK_Sunday = 0;
    public static final int DAYOFWEEK_Monday = 1;
    public static final int DAYOFWEEK_Tuesday = 2;
    public static final int DAYOFWEEK_Wednesday = 3;
    public static final int DAYOFWEEK_Thursday = 4;
    public static final int DAYOFWEEK_Friday = 5;
    public static final int DAYOFWEEK_Saturday = 6;

    protected volatile OATime openTime;
    protected volatile OATime closeTime;
     
    // Links to other objects.
    protected volatile transient Store store;
     
    public StoreHoursOpen() {
        if (!isLoading()) setObjectDefaults();
    }
    @Override
    public void setObjectDefaults() {
        setCreated(new OADateTime());
    }
     
    public StoreHoursOpen(int id) {
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

    @OAProperty(lowerName = "dayOfWeek", displayName = "Day Of Week", displayLength = 6, uiColumnLength = 11, isNameValue = true)
    @OAColumn(name = "DayOfWeek", sqlType = java.sql.Types.INTEGER)
    public int getDayOfWeek() {
        return dayOfWeek;
    }
    public void setDayOfWeek(int newValue) {
        int old = dayOfWeek;
        fireBeforePropertyChange(P_DayOfWeek, old, newValue);
        this.dayOfWeek = newValue;
        firePropertyChange(P_DayOfWeek, old, this.dayOfWeek);
    }

    @OAProperty(enumPropertyName = P_DayOfWeek)
    public String getDayOfWeekString() {
        DayOfWeek dayOfWeek = getDayOfWeekEnum();
        if (dayOfWeek == null) return null;
        return dayOfWeek.name();
    }
    public void setDayOfWeekString(String val) {
        int x = -1;
        if (OAString.isNotEmpty(val)) {
            DayOfWeek dayOfWeek = DayOfWeek.valueOf(val);
            if (dayOfWeek != null) x = dayOfWeek.ordinal();
        }
        if (x < 0) setNull(P_DayOfWeek);
        else setDayOfWeek(x);
    }
    @OAProperty(enumPropertyName = P_DayOfWeek)
    public DayOfWeek getDayOfWeekEnum() {
        if (isNull(P_DayOfWeek)) return null;
        final int val = getDayOfWeek();
        if (val < 0 || val >= DayOfWeek.values().length) return null;
        return DayOfWeek.values()[val];
    }
    public void setDayOfWeekEnum(DayOfWeek val) {
        if (val == null) {
            setNull(P_DayOfWeek);
        }
        else {
            setDayOfWeek(val.ordinal());
        }
    }
    @OACalculatedProperty(enumPropertyName = P_DayOfWeek, displayName = "Day Of Week", displayLength = 6, columnLength = 11, properties = {P_DayOfWeek} )
    public String getDayOfWeekDisplay() {
        DayOfWeek dayOfWeek = getDayOfWeekEnum();
        if (dayOfWeek == null) return null;
        return dayOfWeek.getDisplay();
    }

    @OAProperty(lowerName = "openTime", displayName = "Open Time", displayLength = 8, uiColumnLength = 9)
    @OAColumn(name = "OpenTime", sqlType = java.sql.Types.TIME)
    public OATime getOpenTime() {
        return openTime;
    }
    public void setOpenTime(OATime newValue) {
        OATime old = openTime;
        fireBeforePropertyChange(P_OpenTime, old, newValue);
        this.openTime = newValue;
        firePropertyChange(P_OpenTime, old, this.openTime);
    }

    @OAProperty(lowerName = "closeTime", displayName = "Close Time", displayLength = 8, uiColumnLength = 10)
    @OAColumn(name = "CloseTime", sqlType = java.sql.Types.TIME)
    public OATime getCloseTime() {
        return closeTime;
    }
    public void setCloseTime(OATime newValue) {
        OATime old = closeTime;
        fireBeforePropertyChange(P_CloseTime, old, newValue);
        this.closeTime = newValue;
        firePropertyChange(P_CloseTime, old, this.closeTime);
    }

    @OAOne(
        reverseName = Store.P_StoreHoursOpens, 
        required = true, 
        allowCreateNew = false, 
        fkeys = {@OAFkey(fromProperty = P_StoreId, toProperty = Store.P_Id)}
    )
    public Store getStore() {
        if (store == null) {
            store = (Store) getObject(P_Store);
        }
        return store;
    }
    public void setStore(Store newValue) {
        Store old = this.store;
        fireBeforePropertyChange(P_Store, old, newValue);
        this.store = newValue;
        firePropertyChange(P_Store, old, this.store);
    }
    @OAProperty(isFkeyOnly = true)
    @OAColumn(name = "StoreId")
    public Integer getStoreId() {
        return (Integer) getFkeyProperty(P_StoreId);
    }
    public void setStoreId(Integer newValue) {
        this.store = null;
        setFkeyProperty(P_StoreId, newValue);
    }
    public void load(ResultSet rs, int id) throws SQLException {
        this.id = id;
        java.sql.Timestamp timestamp;
        timestamp = rs.getTimestamp(2);
        if (timestamp != null) this.created = new OADateTime(timestamp);
        this.dayOfWeek = rs.getInt(3);
        setPrimitiveNull(P_DayOfWeek, rs.wasNull());
        java.sql.Time time;
        time = rs.getTime(4);
        if (time != null) this.openTime = new OATime(time);
        time = rs.getTime(5);
        if (time != null) this.closeTime = new OATime(time);
        int storeFkey = rs.getInt(6);
        setFkeyProperty(P_Store, rs.wasNull() ? null : storeFkey);

        this.changedFlag = false;
        this.newFlag = false;
    }
}
 
