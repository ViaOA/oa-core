// Generated by OABuilder
package com.oreillyauto.remodel.model.oa;
 
import java.util.List;
import java.util.logging.*;
import java.sql.*;
import javax.xml.bind.annotation.*;
import com.viaoa.util.*;
import com.viaoa.object.*;
import com.viaoa.hub.*;
import com.viaoa.scheduler.*;
import com.viaoa.annotation.*;
import com.viaoa.util.OADateTime;
import com.oreillyauto.remodel.delegate.oa.*;
import com.oreillyauto.remodel.model.oa.filter.*;
import com.oreillyauto.remodel.model.oa.propertypath.*;
 
@OAClass(
    lowerName = "columnType",
    pluralName = "ColumnTypes",
    shortName = "clt",
    displayName = "Column Type",
    displayProperty = "display"
)
@OATable(
    indexes = {
        @OAIndex(name = "ColumnTypeDatabaseType", fkey = true, columns = { @OAIndexColumn(name = "DatabaseTypeId") }), 
        @OAIndex(name = "ColumnTypeDataType", fkey = true, columns = { @OAIndexColumn(name = "DataTypeId") }), 
        @OAIndex(name = "ColumnTypeSqlType", fkey = true, columns = { @OAIndexColumn(name = "SqlTypeId") })
    }
)
@XmlRootElement(name = "columnType")
@XmlType(factoryMethod = "jaxbCreate")
@XmlAccessorType(XmlAccessType.NONE)
public class ColumnType extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(ColumnType.class.getName());

    public static final String PROPERTY_Id = "Id";
    public static final String P_Id = "Id";
    public static final String PROPERTY_Created = "Created";
    public static final String P_Created = "Created";
    public static final String PROPERTY_IsJsonb = "IsJsonb";
    public static final String P_IsJsonb = "IsJsonb";
    public static final String PROPERTY_Seq = "Seq";
    public static final String P_Seq = "Seq";
     
    public static final String PROPERTY_Display = "Display";
    public static final String P_Display = "Display";
     
    public static final String PROPERTY_CalcDataType = "CalcDataType";
    public static final String P_CalcDataType = "CalcDataType";
    public static final String PROPERTY_Columns = "Columns";
    public static final String P_Columns = "Columns";
    public static final String PROPERTY_DatabaseType = "DatabaseType";
    public static final String P_DatabaseType = "DatabaseType";
    public static final String PROPERTY_DataType = "DataType";
    public static final String P_DataType = "DataType";
    public static final String PROPERTY_SqlType = "SqlType";
    public static final String P_SqlType = "SqlType";
     
    protected volatile int id;
    protected volatile OADateTime created;
    protected volatile boolean isJsonb;
    protected volatile int seq;
     
    // Links to other objects.
    protected volatile transient DatabaseType databaseType;
    protected volatile transient DataType dataType;
    protected volatile transient SqlType sqlType;
     
    public ColumnType() {
        if (!isLoading()) {
            setCreated(new OADateTime());
        }
    }
     
    public ColumnType(int id) {
        this();
        setId(id);
    }
     

    @XmlAttribute(name="oaSingleId")
    public Integer getJaxbGuid() {
        return super.getJaxbGuid();
    }

    @OAProperty(isUnique = true, trackPrimitiveNull = false, displayLength = 6)
    @OAId()
    @XmlTransient
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
    @XmlID
    @XmlAttribute(name="id")
    public String getJaxbId() {
        // note: jaxb spec requires id to be a string
        if (!getJaxbShouldInclude(P_Id)) return null;
        return ""+id;
    }
    public void setJaxbId(String id) {
        if (getJaxbAllowPropertyChange(P_Id, this.id, id)) {
            setId((int) OAConv.convert(int.class, id));
        }
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
    @XmlElement(name="created", nillable=true)
    public OADateTime getJaxbCreated() {
        if (!getJaxbShouldInclude(P_Created)) return null;
        return getCreated();
    }
    public void setJaxbCreated(OADateTime newValue) {
        if (getJaxbAllowPropertyChange(P_Created, this.created, newValue)) {
            setCreated(newValue);
        }
    }

    @OAProperty(displayName = "Is Jsonb", displayLength = 5, columnLength = 8)
    @OAColumn(sqlType = java.sql.Types.BOOLEAN)
    public boolean getIsJsonb() {
        return isJsonb;
    }
    public boolean isIsJsonb() {
        return getIsJsonb();
    }
    public void setIsJsonb(boolean newValue) {
        boolean old = isJsonb;
        fireBeforePropertyChange(P_IsJsonb, old, newValue);
        this.isJsonb = newValue;
        firePropertyChange(P_IsJsonb, old, this.isJsonb);
    }
    @XmlElement(name="isJsonb")
    public Boolean getJaxbIsJsonb() {
        if (!getJaxbShouldInclude(P_IsJsonb)) return null;
        return getIsJsonb();
    }
    public void setJaxbIsJsonb(Boolean newValue) {
        if (getJaxbAllowPropertyChange(P_IsJsonb, this.isJsonb, newValue)) {
            setIsJsonb(newValue);
        }
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
    @XmlElement(name="seq")
    public Integer getJaxbSeq() {
        if (!getJaxbShouldInclude(P_Seq)) return null;
        return getSeq();
    }
    public void setJaxbSeq(Integer newValue) {
        if (getJaxbAllowPropertyChange(P_Seq, this.seq, newValue)) {
            setSeq(newValue);
        }
    }

    @OACalculatedProperty(displayLength = 20, columnLength = 16, properties = {P_SqlType+"."+SqlType.P_Name, P_CalcDataType+"."+DataType.P_Name})
    public String getDisplay() {
        String display = "";
        String name = null;
        DataType dataType = this.getCalcDataType();
        if (dataType != null) {
            name = dataType.getName();
        }
        display = OAString.concat(display, name, " ");
    
        name = null;
        SqlType sqlType = this.getSqlType();
        if (sqlType != null) {
            name = sqlType.getName();
            name = "(" + OAString.notNull(name) + ")";
        }
        display = OAString.concat(display, name, " ");
    
        return display;
    }
    @XmlElement(name="display", nillable=true)
    public String getJaxbDisplay() {
        if (!getJaxbShouldInclude(P_Display)) return null;
        return getDisplay();
    }
     
    @OAOne(
        displayName = "Data Type", 
        isCalculated = true, 
        calcDependentProperties = {P_SqlType+"."+SqlType.P_DataType, P_DataType}, 
        reverseName = DataType.P_CalcColumnTypes, 
        allowCreateNew = false, 
        allowAddExisting = false
    )
    @XmlTransient
    public DataType getCalcDataType() {
        // Custom code here to get CalcDataType
        DataType dt = getDataType();
        if (dt == null) {
            SqlType st = getSqlType();
            if (st != null) {
                dt = st.getDataType();
            }
        }
        return dt;
    }
     
    @OAMany(
        toClass = Column.class, 
        reverseName = Column.P_ColumnType, 
        createMethod = false
    )
    @XmlTransient
    private Hub<Column> getColumns() {
        // oamodel has createMethod set to false, this method exists only for annotations.
        return null;
    }
     
    @OAOne(
        displayName = "Database Type", 
        reverseName = DatabaseType.P_ColumnTypes, 
        required = true, 
        allowCreateNew = false
    )
    @OAFkey(columns = {"DatabaseTypeId"})
    @XmlTransient
    public DatabaseType getDatabaseType() {
        if (databaseType == null) {
            databaseType = (DatabaseType) getObject(P_DatabaseType);
        }
        return databaseType;
    }
    public void setDatabaseType(DatabaseType newValue) {
        DatabaseType old = this.databaseType;
        fireBeforePropertyChange(P_DatabaseType, old, newValue);
        this.databaseType = newValue;
        firePropertyChange(P_DatabaseType, old, this.databaseType);
    }
    @XmlElement(name="databaseType", required=true)
    public DatabaseType getJaxbDatabaseType() {
        Object obj = super.getJaxbObject(P_DatabaseType);
        return (DatabaseType) obj;
    }
    public void setJaxbDatabaseType(DatabaseType newValue) {
        if (getJaxbAllowPropertyChange(P_DatabaseType, this.databaseType, newValue)) {
            setDatabaseType(newValue);
        }
    }
    @XmlElement(name="refDatabaseType")
    @XmlIDREF
    public DatabaseType getJaxbRefDatabaseType() {
        Object obj = super.getJaxbRefObject(P_DatabaseType);
        return (DatabaseType) obj;
    }
    public void setJaxbRefDatabaseType(DatabaseType newValue) {
        setJaxbDatabaseType(newValue);
    }
    @XmlElement(name="databaseTypeId", required=true)
    public String getJaxbDatabaseTypeId() {
        String s = super.getJaxbId(P_DatabaseType);
        return s;
    }
    public void setJaxbDatabaseTypeId(String id) {
        setJaxbId(P_DatabaseType, id);
    }
     
    @OAOne(
        displayName = "Data Type", 
        reverseName = DataType.P_ColumnTypes, 
        allowCreateNew = false
    )
    @OAFkey(columns = {"DataTypeId"})
    @XmlTransient
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
    @XmlElement(name="dataType", nillable=true)
    public DataType getJaxbDataType() {
        Object obj = super.getJaxbObject(P_DataType);
        return (DataType) obj;
    }
    public void setJaxbDataType(DataType newValue) {
        if (getJaxbAllowPropertyChange(P_DataType, this.dataType, newValue)) {
            setDataType(newValue);
        }
    }
    @XmlElement(name="refDataType")
    @XmlIDREF
    public DataType getJaxbRefDataType() {
        Object obj = super.getJaxbRefObject(P_DataType);
        return (DataType) obj;
    }
    public void setJaxbRefDataType(DataType newValue) {
        setJaxbDataType(newValue);
    }
    @XmlElement(name="dataTypeId", nillable=true)
    public String getJaxbDataTypeId() {
        String s = super.getJaxbId(P_DataType);
        return s;
    }
    public void setJaxbDataTypeId(String id) {
        setJaxbId(P_DataType, id);
    }
     
    @OAOne(
        displayName = "Sql Type", 
        reverseName = SqlType.P_ColumnTypes, 
        required = true, 
        allowCreateNew = false
    )
    @OAFkey(columns = {"SqlTypeId"})
    @XmlTransient
    public SqlType getSqlType() {
        if (sqlType == null) {
            sqlType = (SqlType) getObject(P_SqlType);
        }
        return sqlType;
    }
    public void setSqlType(SqlType newValue) {
        SqlType old = this.sqlType;
        fireBeforePropertyChange(P_SqlType, old, newValue);
        this.sqlType = newValue;
        firePropertyChange(P_SqlType, old, this.sqlType);
    }
    @XmlElement(name="sqlType", required=true)
    public SqlType getJaxbSqlType() {
        Object obj = super.getJaxbObject(P_SqlType);
        return (SqlType) obj;
    }
    public void setJaxbSqlType(SqlType newValue) {
        if (getJaxbAllowPropertyChange(P_SqlType, this.sqlType, newValue)) {
            setSqlType(newValue);
        }
    }
    @XmlElement(name="refSqlType")
    @XmlIDREF
    public SqlType getJaxbRefSqlType() {
        Object obj = super.getJaxbRefObject(P_SqlType);
        return (SqlType) obj;
    }
    public void setJaxbRefSqlType(SqlType newValue) {
        setJaxbSqlType(newValue);
    }
    @XmlElement(name="sqlTypeId", required=true)
    public String getJaxbSqlTypeId() {
        String s = super.getJaxbId(P_SqlType);
        return s;
    }
    public void setJaxbSqlTypeId(String id) {
        setJaxbId(P_SqlType, id);
    }
     
    public void load(ResultSet rs, int id) throws SQLException {
        this.id = id;
        java.sql.Timestamp timestamp;
        timestamp = rs.getTimestamp(2);
        if (timestamp != null) this.created = new OADateTime(timestamp);
        this.isJsonb = rs.getBoolean(3);
        if (rs.wasNull()) {
            OAObjectInfoDelegate.setPrimitiveNull(this, ColumnType.P_IsJsonb, true);
        }
        this.seq = (int) rs.getInt(4);
        if (rs.wasNull()) {
            OAObjectInfoDelegate.setPrimitiveNull(this, ColumnType.P_Seq, true);
        }
        int databaseTypeFkey = rs.getInt(5);
        if (!rs.wasNull() && databaseTypeFkey > 0) {
            setProperty(P_DatabaseType, new OAObjectKey(databaseTypeFkey));
        }
        int dataTypeFkey = rs.getInt(6);
        if (!rs.wasNull() && dataTypeFkey > 0) {
            setProperty(P_DataType, new OAObjectKey(dataTypeFkey));
        }
        int sqlTypeFkey = rs.getInt(7);
        if (!rs.wasNull() && sqlTypeFkey > 0) {
            setProperty(P_SqlType, new OAObjectKey(sqlTypeFkey));
        }
        if (rs.getMetaData().getColumnCount() != 7) {
            throw new SQLException("invalid number of columns for load method");
        }

        this.changedFlag = false;
        this.newFlag = false;
    }
    public static ColumnType jaxbCreate() {
        ColumnType columnType = (ColumnType) OAObject.jaxbCreateInstance(ColumnType.class);
        if (columnType == null) columnType = new ColumnType();
        return columnType;
    }
}
 
