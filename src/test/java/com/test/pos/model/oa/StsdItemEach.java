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
    lowerName = "stsdItemEach",
    pluralName = "StsdItemEaches",
    shortName = "sie",
    displayName = "Stsd Item Each",
    displayProperty = "id",
    noPojo = true
)
@OATable(
    indexes = {
        @OAIndex(name = "StsdItemEachStsdItem", fkey = true, columns = { @OAIndexColumn(name = "StsdItemId") })
    }
)
public class StsdItemEach extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(StsdItemEach.class.getName());

    public static final String P_Id = "id";
    public static final String P_Created = "created";
    public static final String P_SerialCode = "serialCode";
     
    public static final String P_StsdItem = "stsdItem";
    public static final String P_StsdItemId = "stsdItemId"; // fkey
     
    protected volatile int id;
    protected volatile OADateTime created;
    protected volatile String serialCode;
     
    // Links to other objects.
    protected volatile transient StsdItem stsdItem;
     
    public StsdItemEach() {
        if (!isLoading()) setObjectDefaults();
    }
    @Override
    public void setObjectDefaults() {
        setCreated(new OADateTime());
    }
     
    public StsdItemEach(int id) {
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

    @OAProperty(lowerName = "serialCode", displayName = "Serial Code", maxLength = 35, displayLength = 18)
    @OAColumn(name = "SerialCode", maxLength = 35)
    public String getSerialCode() {
        return serialCode;
    }
    public void setSerialCode(String newValue) {
        String old = serialCode;
        fireBeforePropertyChange(P_SerialCode, old, newValue);
        this.serialCode = newValue;
        firePropertyChange(P_SerialCode, old, this.serialCode);
    }

    @OAOne(
        displayName = "Stsd Item", 
        reverseName = StsdItem.P_StsdItemEaches, 
        required = true, 
        allowCreateNew = false, 
        fkeys = {@OAFkey(fromProperty = P_StsdItemId, toProperty = StsdItem.P_Id)}
    )
    public StsdItem getStsdItem() {
        if (stsdItem == null) {
            stsdItem = (StsdItem) getObject(P_StsdItem);
        }
        return stsdItem;
    }
    public void setStsdItem(StsdItem newValue) {
        StsdItem old = this.stsdItem;
        fireBeforePropertyChange(P_StsdItem, old, newValue);
        this.stsdItem = newValue;
        firePropertyChange(P_StsdItem, old, this.stsdItem);
    }
    @OAProperty(isFkeyOnly = true)
    @OAColumn(name = "StsdItemId")
    public Integer getStsdItemId() {
        return (Integer) getFkeyProperty(P_StsdItemId);
    }
    public void setStsdItemId(Integer newValue) {
        this.stsdItem = null;
        setFkeyProperty(P_StsdItemId, newValue);
    }
    public void load(ResultSet rs, int id) throws SQLException {
        this.id = id;
        java.sql.Timestamp timestamp;
        timestamp = rs.getTimestamp(2);
        if (timestamp != null) this.created = new OADateTime(timestamp);
        this.serialCode = rs.getString(3);
        int stsdItemFkey = rs.getInt(4);
        setFkeyProperty(P_StsdItem, rs.wasNull() ? null : stsdItemFkey);

        this.changedFlag = false;
        this.newFlag = false;
    }
}
 
