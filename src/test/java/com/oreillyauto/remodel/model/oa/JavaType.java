// Generated by OABuilder
package com.oreillyauto.remodel.model.oa;
 
import java.util.List;
import java.util.logging.*;
import java.sql.*;
import javax.xml.bind.annotation.*;
import com.viaoa.object.*;
import com.viaoa.hub.*;
import com.viaoa.scheduler.*;
import com.viaoa.util.*;
import com.viaoa.annotation.*;
import com.viaoa.util.OADateTime;
import com.oreillyauto.remodel.delegate.oa.*;
import com.oreillyauto.remodel.model.oa.filter.*;
import com.oreillyauto.remodel.model.oa.propertypath.*;
 
@OAClass(
    lowerName = "javaType",
    pluralName = "JavaTypes",
    shortName = "jvt",
    displayName = "Java Type",
    isLookup = true,
    isPreSelect = true,
    displayProperty = "name",
    sortProperty = "seq"
)
@OATable(
    indexes = {
        @OAIndex(name = "JavaTypeDataType", fkey = true, columns = { @OAIndexColumn(name = "DataTypeId") })
    }
)
@XmlRootElement(name = "javaType")
@XmlType(factoryMethod = "jaxbCreate")
@XmlAccessorType(XmlAccessType.NONE)
public class JavaType extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(JavaType.class.getName());

    public static final String PROPERTY_Id = "Id";
    public static final String P_Id = "Id";
    public static final String PROPERTY_Created = "Created";
    public static final String P_Created = "Created";
    public static final String PROPERTY_Name = "Name";
    public static final String P_Name = "Name";
    public static final String PROPERTY_Seq = "Seq";
    public static final String P_Seq = "Seq";
     
     
    public static final String PROPERTY_DataType = "DataType";
    public static final String P_DataType = "DataType";
    public static final String PROPERTY_JsonColumns = "JsonColumns";
    public static final String P_JsonColumns = "JsonColumns";
     
    protected volatile int id;
    protected volatile OADateTime created;
    protected volatile String name;
    protected volatile int seq;
     
    // Links to other objects.
    protected volatile transient DataType dataType;
    protected transient Hub<JsonColumn> hubJsonColumns;
     
    public JavaType() {
        if (!isLoading()) {
            setCreated(new OADateTime());
        }
    }
     
    public JavaType(int id) {
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
    @XmlElement(name="name", nillable=true)
    public String getJaxbName() {
        if (!getJaxbShouldInclude(P_Name)) return null;
        return getName();
    }
    public void setJaxbName(String newValue) {
        if (getJaxbAllowPropertyChange(P_Name, this.name, newValue)) {
            setName(newValue);
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

    @OAOne(
        displayName = "Data Type", 
        reverseName = DataType.P_JavaTypes, 
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
     
    @OAMany(
        displayName = "Json Columns", 
        toClass = JsonColumn.class, 
        reverseName = JsonColumn.P_JavaType
    )
    @XmlTransient
    public Hub<JsonColumn> getJsonColumns() {
        if (hubJsonColumns == null) {
            hubJsonColumns = (Hub<JsonColumn>) getHub(P_JsonColumns);
        }
        return hubJsonColumns;
    }
    @XmlElementWrapper(name="jsonColumns")
    @XmlElement(name="jsonColumn", type=JsonColumn.class)
    protected List<JsonColumn> getJaxbJsonColumns() {
        return getJaxbHub(P_JsonColumns);
    }
    @XmlElementWrapper(name="refJsonColumns")
    @XmlElement(name="jsonColumn", type=JsonColumn.class)
    @XmlIDREF
    protected List<JsonColumn> getJaxbRefJsonColumns() {
        return getJaxbRefHub(P_JsonColumns); 
    }
    protected void setJaxbRefJsonColumns(List<JsonColumn> lst) {
        // no-op, since jaxb sends lst=hubJsonColumns 
    }
     
    public void load(ResultSet rs, int id) throws SQLException {
        this.id = id;
        java.sql.Timestamp timestamp;
        timestamp = rs.getTimestamp(2);
        if (timestamp != null) this.created = new OADateTime(timestamp);
        this.name = rs.getString(3);
        this.seq = (int) rs.getInt(4);
        if (rs.wasNull()) {
            OAObjectInfoDelegate.setPrimitiveNull(this, JavaType.P_Seq, true);
        }
        int dataTypeFkey = rs.getInt(5);
        if (!rs.wasNull() && dataTypeFkey > 0) {
            setProperty(P_DataType, new OAObjectKey(dataTypeFkey));
        }
        if (rs.getMetaData().getColumnCount() != 5) {
            throw new SQLException("invalid number of columns for load method");
        }

        this.changedFlag = false;
        this.newFlag = false;
    }
    public static JavaType jaxbCreate() {
        JavaType javaType = (JavaType) OAObject.jaxbCreateInstance(JavaType.class);
        if (javaType == null) javaType = new JavaType();
        return javaType;
    }
}
 
