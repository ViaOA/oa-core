// Generated by OABuilder
package com.oreillyauto.remodel.model.oa;
 
import java.util.*;
import java.util.logging.*;
import java.sql.*;
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
    lowerName = "index",
    pluralName = "Indexes",
    shortName = "ind",
    displayName = "Index",
    displayProperty = "name",
    rootTreePropertyPaths = {
        "[Database]."+Database.P_Tables+"."+Table.P_Indexes
    }
)
@OATable(
    indexes = {
        @OAIndex(name = "IndexTable", fkey = true, columns = { @OAIndexColumn(name = "TableId") })
    }
)
public class Index extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(Index.class.getName());

    public static final String P_Id = "Id";
    public static final String P_Created = "Created";
    public static final String P_Name = "Name";
    public static final String P_newName = "newName";
     
     
    public static final String P_IndexColumns = "IndexColumns";
    public static final String P_Table = "Table";
     
    protected volatile int id;
    protected volatile OADateTime created;
    protected volatile String name;
    protected volatile String newName;
     
    // Links to other objects.
    protected transient Hub<IndexColumn> hubIndexColumns;
    protected volatile transient Table table;
     
    public Index() {
        if (!isLoading()) setObjectDefaults();
    }
    @Override
    public void setObjectDefaults() {
        setCreated(new OADateTime());
    }
     
    public Index(int id) {
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
    @OAProperty(maxLength = 55, displayLength = 20)
    @OAColumn(maxLength = 55)
    public String getName() {
        return name;
    }
    public void setName(String newValue) {
        String old = name;
        fireBeforePropertyChange(P_Name, old, newValue);
        this.name = newValue;
        firePropertyChange(P_Name, old, this.name);
    }
    @OAProperty(displayName = "New Name", maxLength = 55, displayLength = 20)
    @OAColumn(maxLength = 55)
    public String getnewName() {
        return newName;
    }
    public void setnewName(String newValue) {
        String old = newName;
        fireBeforePropertyChange(P_newName, old, newValue);
        this.newName = newValue;
        firePropertyChange(P_newName, old, this.newName);
    }
    @OAMany(
        displayName = "Index Columns", 
        toClass = IndexColumn.class, 
        owner = true, 
        reverseName = IndexColumn.P_Index, 
        cascadeSave = true, 
        cascadeDelete = true, 
        seqProperty = IndexColumn.P_Seq, 
        sortProperty = IndexColumn.P_Seq
    )
    public Hub<IndexColumn> getIndexColumns() {
        if (hubIndexColumns == null) {
            hubIndexColumns = (Hub<IndexColumn>) getHub(P_IndexColumns);
        }
        return hubIndexColumns;
    }
    @OAOne(
        reverseName = Table.P_Indexes, 
        required = true, 
        allowCreateNew = false
    )
    @OAFkey(columns = {"TableId"})
    public Table getTable() {
        if (table == null) {
            table = (Table) getObject(P_Table);
        }
        return table;
    }
    public void setTable(Table newValue) {
        Table old = this.table;
        fireBeforePropertyChange(P_Table, old, newValue);
        this.table = newValue;
        firePropertyChange(P_Table, old, this.table);
    }
    public void load(ResultSet rs, int id) throws SQLException {
        this.id = id;
        java.sql.Timestamp timestamp;
        timestamp = rs.getTimestamp(2);
        if (timestamp != null) this.created = new OADateTime(timestamp);
        this.name = rs.getString(3);
        this.newName = rs.getString(4);
        int tableFkey = rs.getInt(5);
        if (!rs.wasNull() && tableFkey > 0) {
            setProperty(P_Table, new OAObjectKey(tableFkey));
        }
        if (rs.getMetaData().getColumnCount() != 5) {
            throw new SQLException("invalid number of columns for load method");
        }

        this.changedFlag = false;
        this.newFlag = false;
    }
}
 