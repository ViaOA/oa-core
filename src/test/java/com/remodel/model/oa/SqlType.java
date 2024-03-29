// Generated by OABuilder
package com.remodel.model.oa;
 
import java.util.*;
import java.util.logging.*;
import java.sql.*;
import com.viaoa.util.*;
import com.viaoa.object.*;
import com.viaoa.hub.*;
import com.viaoa.scheduler.*;
import com.remodel.delegate.oa.*;
import com.remodel.model.oa.filter.*;
import com.remodel.model.oa.propertypath.*;
import com.viaoa.annotation.*;
import com.viaoa.util.OADateTime;
 
@OAClass(
    lowerName = "sqlType",
    pluralName = "SqlTypes",
    shortName = "sqt",
    displayName = "Sql Type",
    isLookup = true,
    isPreSelect = true,
    isProcessed = true,
    displayProperty = "display",
    sortProperty = "seq"
)
@OATable(
    indexes = {
        @OAIndex(name = "SqlTypeDataType", fkey = true, columns = { @OAIndexColumn(name = "DataTypeId") })
    }
)
public class SqlType extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(SqlType.class.getName());

    public static final String P_Id = "Id";
    public static final String P_Created = "Created";
    public static final String P_SqlType = "SqlType";
    public static final String P_Name = "Name";
    public static final String P_Seq = "Seq";
     
    public static final String P_Display = "Display";
     
    public static final String P_ColumnTypes = "ColumnTypes";
    public static final String P_DataType = "DataType";
     
    protected volatile int id;
    protected volatile OADateTime created;
    protected volatile int sqlType;
    protected volatile String name;
    protected volatile int seq;
     
    // Links to other objects.
    protected volatile transient DataType dataType;
     
    public SqlType() {
        if (!isLoading()) setObjectDefaults();
    }
    @Override
    public void setObjectDefaults() {
        setCreated(new OADateTime());
    }
     
    public SqlType(int id) {
        this();
        setId(id);
    }
     

    @OAProperty(isUnique = true, trackPrimitiveNull = false, displayLength = 6)
    @OAId()
    @OAColumn(sqlType = java.sql.Types.INTEGER)
    public int getId() {
        return id;
    }
    public void setId(int newValue) {
        int old = id;
        fireBeforePropertyChange(P_Id, old, newValue);
        this.id = newValue;
        firePropertyChange(P_Id, old, this.id);
    }
    @OAProperty(defaultValue = "new OADateTime()", displayLength = 15, isProcessed = true)
    @OAColumn(sqlType = java.sql.Types.TIMESTAMP)
    public OADateTime getCreated() {
        return created;
    }
    public void setCreated(OADateTime newValue) {
        OADateTime old = created;
        fireBeforePropertyChange(P_Created, old, newValue);
        this.created = newValue;
        firePropertyChange(P_Created, old, this.created);
    }
    @OAProperty(displayName = "Sql Type", displayLength = 6, columnLength = 8, isProcessed = true)
    @OAColumn(sqlType = java.sql.Types.INTEGER)
    public int getSqlType() {
        return sqlType;
    }
    public void setSqlType(int newValue) {
        int old = sqlType;
        fireBeforePropertyChange(P_SqlType, old, newValue);
        this.sqlType = newValue;
        firePropertyChange(P_SqlType, old, this.sqlType);
    }
    @OAProperty(maxLength = 25, displayLength = 20)
    @OAColumn(maxLength = 25)
    public String getName() {
        return name;
    }
    public void setName(String newValue) {
        String old = name;
        fireBeforePropertyChange(P_Name, old, newValue);
        this.name = newValue;
        firePropertyChange(P_Name, old, this.name);
    }
    @OAProperty(displayLength = 6, isAutoSeq = true)
    @OAColumn(sqlType = java.sql.Types.INTEGER)
    public int getSeq() {
        return seq;
    }
    public void setSeq(int newValue) {
        int old = seq;
        fireBeforePropertyChange(P_Seq, old, newValue);
        this.seq = newValue;
        firePropertyChange(P_Seq, old, this.seq);
    }
    @OACalculatedProperty(displayLength = 20, properties = {P_SqlType, P_Name})
    public String getDisplay() {
        String display = "";
        String name = this.getName();
        display = OAString.concat(display, name, " ");
    
        int sqlType = this.getSqlType();
        display = OAString.concat(display,  "("+sqlType+")", " ");
        return display;
    }
    @OAMany(
        displayName = "Column Types", 
        toClass = ColumnType.class, 
        reverseName = ColumnType.P_SqlType, 
        createMethod = false
    )
    private Hub<ColumnType> getColumnTypes() {
        // oamodel has createMethod set to false, this method exists only for annotations.
        return null;
    }
    @OAOne(
        displayName = "Data Type", 
        reverseName = DataType.P_SqlTypes, 
        allowCreateNew = false
    )
    @OAFkey(columns = {"DataTypeId"})
    public DataType getDataType() {
        if (dataType == null) {
            dataType = (DataType) getObject(P_DataType);
        }
        return dataType;
    }
    public void setDataType(DataType newValue) {
        DataType old = this.dataType;
        fireBeforePropertyChange(P_DataType, old, newValue);
        this.dataType = newValue;
        firePropertyChange(P_DataType, old, this.dataType);
    }
    public void load(ResultSet rs, int id) throws SQLException {
        this.id = id;
        java.sql.Timestamp timestamp;
        timestamp = rs.getTimestamp(2);
        if (timestamp != null) this.created = new OADateTime(timestamp);
        this.sqlType = (int) rs.getInt(3);
        if (rs.wasNull()) {
            OAObjectInfoDelegate.setPrimitiveNull(this, SqlType.P_SqlType, true);
        }
        this.name = rs.getString(4);
        this.seq = (int) rs.getInt(5);
        if (rs.wasNull()) {
            OAObjectInfoDelegate.setPrimitiveNull(this, SqlType.P_Seq, true);
        }
        int dataTypeFkey = rs.getInt(6);
        if (!rs.wasNull() && dataTypeFkey > 0) {
            setProperty(P_DataType, new OAObjectKey(dataTypeFkey));
        }
        if (rs.getMetaData().getColumnCount() != 6) {
            throw new SQLException("invalid number of columns for load method");
        }

        this.changedFlag = false;
        this.newFlag = false;
    }
}
 
