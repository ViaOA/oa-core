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
import com.viaoa.datetime.OADate;
import com.test.pos.delegate.oa.*;
import com.test.pos.model.oa.filter.*;
import com.test.pos.model.oa.propertypath.*;
 
@OAClass(
    lowerName = "storeClosedDate",
    pluralName = "StoreClosedDates",
    shortName = "scd",
    displayName = "Store Closed Date",
    displayProperty = "date",
    sortProperty = "date",
    noPojo = true
)
@OATable(
    indexes = {
        @OAIndex(name = "StoreClosedDateStore", fkey = true, columns = { @OAIndexColumn(name = "StoreId") })
    }
)
public class StoreClosedDate extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(StoreClosedDate.class.getName());

    public static final String P_Id = "id";
    public static final String P_Created = "created";
    public static final String P_Date = "date";
    public static final String P_Reason = "reason";
     
    public static final String P_Store = "store";
    public static final String P_StoreId = "storeId"; // fkey
     
    protected volatile int id;
    protected volatile OADateTime created;
    protected volatile OADate date;
    protected volatile String reason;
     
    // Links to other objects.
    protected volatile transient Store store;
     
    public StoreClosedDate() {
        if (!isLoading()) setObjectDefaults();
    }
    @Override
    public void setObjectDefaults() {
        setCreated(new OADateTime());
    }
     
    public StoreClosedDate(int id) {
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

    @OAProperty(lowerName = "date", displayLength = 8)
    @OAColumn(name = "DateValue", sqlType = java.sql.Types.DATE)
    public OADate getDate() {
        return date;
    }
    public void setDate(OADate newValue) {
        OADate old = date;
        fireBeforePropertyChange(P_Date, old, newValue);
        this.date = newValue;
        firePropertyChange(P_Date, old, this.date);
    }

    @OAProperty(lowerName = "reason", maxLength = 25, displayLength = 15)
    @OAColumn(name = "Reason", maxLength = 25)
    public String getReason() {
        return reason;
    }
    public void setReason(String newValue) {
        String old = reason;
        fireBeforePropertyChange(P_Reason, old, newValue);
        this.reason = newValue;
        firePropertyChange(P_Reason, old, this.reason);
    }

    @OAOne(
        reverseName = Store.P_StoreClosedDates, 
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
        java.sql.Date date;
        date = rs.getDate(3);
        if (date != null) this.date = new OADate(date);
        this.reason = rs.getString(4);
        int storeFkey = rs.getInt(5);
        setFkeyProperty(P_Store, rs.wasNull() ? null : storeFkey);

        this.changedFlag = false;
        this.newFlag = false;
    }
}
 
