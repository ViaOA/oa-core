// Generated by OABuilder
package com.remodel.model.oa;
 
import java.util.*;
import java.util.logging.*;
import java.sql.*;
import com.viaoa.object.*;
import com.viaoa.hub.*;
import com.viaoa.scheduler.*;
import com.viaoa.util.*;
import com.remodel.delegate.oa.*;
import com.remodel.model.oa.filter.*;
import com.remodel.model.oa.propertypath.*;
import com.viaoa.annotation.*;
import com.viaoa.util.OADateTime;
 
@OAClass(
    lowerName = "table",
    pluralName = "Tables",
    shortName = "tbl",
    displayName = "Table",
    displayProperty = "name",
    sortProperty = "name",
    rootTreePropertyPaths = {
        "[Database]."+Database.P_Tables
    }
)
@OATable(
    indexes = {
        @OAIndex(name = "TableDatabase", fkey = true, columns = { @OAIndexColumn(name = "DatabaseId") })
    }
)
public class Table extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(Table.class.getName());

    public static final String P_Id = "Id";
    public static final String P_Created = "Created";
    public static final String P_Name = "Name";
    public static final String P_NewName = "NewName";
    public static final String P_Description = "Description";
    public static final String P_AbbrevName = "AbbrevName";
    public static final String P_UseType = "UseType";
    public static final String P_UseTypeAsString = "UseTypeString";
    public static final String P_Notes = "Notes";
     
     
    public static final String P_Columns = "Columns";
    public static final String P_Database = "Database";
    public static final String P_ForeignTables = "ForeignTables";
    public static final String P_Indexes = "Indexes";
    public static final String P_QueryTables = "QueryTables";
    public static final String P_Repositories = "Repositories";
    public static final String P_TableCategories = "TableCategories";
    public static final String P_TableForeignTables = "TableForeignTables";
     
    protected volatile int id;
    protected volatile OADateTime created;
    protected volatile String name;
    protected volatile String newName;
    protected volatile String description;
    protected volatile String abbrevName;
    protected volatile int useType;
    public static enum UseType {
        Unknown("Unknown"),
        NotUsed("Not Used"),
        WillUse("Will Use"),
        Use("Use");

        private String display;
        UseType(String display) {
            this.display = display;
        }

        public String getDisplay() {
            return display;
        }
    }
    public static final int USETYPE_Unknown = 0;
    public static final int USETYPE_NotUsed = 1;
    public static final int USETYPE_WillUse = 2;
    public static final int USETYPE_Use = 3;
    public static final Hub<String> hubUseType;
    static {
        hubUseType = new Hub<String>(String.class);
        hubUseType.addElement("Unknown");
        hubUseType.addElement("Not Used");
        hubUseType.addElement("Will Use");
        hubUseType.addElement("Use");
    }
    protected volatile String notes;
     
    // Links to other objects.
    protected transient Hub<Column> hubColumns;
    protected volatile transient Database database;
    protected transient Hub<ForeignTable> hubForeignTables;
    protected transient Hub<Index> hubIndexes;
    protected transient Hub<QueryTable> hubQueryTables;
    protected transient Hub<Repository> hubRepositories;
    protected transient Hub<TableCategory> hubTableCategories;
    protected transient Hub<ForeignTable> hubTableForeignTables;
     
    public Table() {
        if (!isLoading()) setObjectDefaults();
    }
    @Override
    public void setObjectDefaults() {
        setCreated(new OADateTime());
    }
     
    public Table(int id) {
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
    @OAProperty(maxLength = 50, displayLength = 20)
    @OAColumn(maxLength = 50)
    public String getName() {
        return name;
    }
    public void setName(String newValue) {
        String old = name;
        fireBeforePropertyChange(P_Name, old, newValue);
        this.name = newValue;
        firePropertyChange(P_Name, old, this.name);
    }
    @OAProperty(displayName = "New Name", maxLength = 50, displayLength = 20)
    @OAColumn(maxLength = 50)
    public String getNewName() {
        return newName;
    }
    public void setNewName(String newValue) {
        String old = newName;
        fireBeforePropertyChange(P_NewName, old, newValue);
        this.newName = newValue;
        firePropertyChange(P_NewName, old, this.newName);
    }
    @OAProperty(maxLength = 150, displayLength = 20)
    @OAColumn(maxLength = 150)
    public String getDescription() {
        return description;
    }
    public void setDescription(String newValue) {
        String old = description;
        fireBeforePropertyChange(P_Description, old, newValue);
        this.description = newValue;
        firePropertyChange(P_Description, old, this.description);
    }
    @OAProperty(displayName = "Abbrev Name", maxLength = 15, displayLength = 12)
    @OAColumn(maxLength = 15)
    public String getAbbrevName() {
        return abbrevName;
    }
    public void setAbbrevName(String newValue) {
        String old = abbrevName;
        fireBeforePropertyChange(P_AbbrevName, old, newValue);
        this.abbrevName = newValue;
        firePropertyChange(P_AbbrevName, old, this.abbrevName);
    }
    @OAProperty(displayName = "Use Type", trackPrimitiveNull = false, displayLength = 6, columnLength = 8, isNameValue = true)
    @OAColumn(sqlType = java.sql.Types.INTEGER)
    public int getUseType() {
        return useType;
    }
    public void setUseType(int newValue) {
        int old = useType;
        fireBeforePropertyChange(P_UseType, old, newValue);
        this.useType = newValue;
        firePropertyChange(P_UseType, old, this.useType);
        firePropertyChange(P_UseType + "String");
        firePropertyChange(P_UseType + "Enum");
    }

    public String getUseTypeString() {
        UseType useType = getUseTypeEnum();
        if (useType == null) return null;
        return useType.name();
    }
    public void setUseTypeString(String val) {
        int x = -1;
        if (OAString.isNotEmpty(val)) {
            UseType useType = UseType.valueOf(val);
            if (useType != null) x = useType.ordinal();
        }
        if (x < 0) setNull(P_UseType);
        else setUseType(x);
    }


    public UseType getUseTypeEnum() {
        if (isNull(P_UseType)) return null;
        final int val = getUseType();
        if (val < 0 || val >= UseType.values().length) return null;
        return UseType.values()[val];
    }

    public void setUseTypeEnum(UseType val) {
        if (val == null) {
            setNull(P_UseType);
        }
        else {
            setUseType(val.ordinal());
        }
    }
    @OAProperty(displayLength = 30, columnLength = 20, isHtml = true)
    @OAColumn(sqlType = java.sql.Types.CLOB)
    public String getNotes() {
        return notes;
    }
    public void setNotes(String newValue) {
        String old = notes;
        fireBeforePropertyChange(P_Notes, old, newValue);
        this.notes = newValue;
        firePropertyChange(P_Notes, old, this.notes);
    }
    @OAMany(
        toClass = Column.class, 
        owner = true, 
        reverseName = Column.P_Table, 
        cascadeSave = true, 
        cascadeDelete = true, 
        seqProperty = Column.P_Seq, 
        sortProperty = Column.P_Seq
    )
    public Hub<Column> getColumns() {
        if (hubColumns == null) {
            hubColumns = (Hub<Column>) getHub(P_Columns);
        }
        return hubColumns;
    }
    @OAOne(
        reverseName = Database.P_Tables, 
        required = true, 
        allowCreateNew = false
    )
    @OAFkey(columns = {"DatabaseId"})
    public Database getDatabase() {
        if (database == null) {
            database = (Database) getObject(P_Database);
        }
        return database;
    }
    public void setDatabase(Database newValue) {
        Database old = this.database;
        fireBeforePropertyChange(P_Database, old, newValue);
        this.database = newValue;
        firePropertyChange(P_Database, old, this.database);
    }
    @OAMany(
        displayName = "Foreign Tables", 
        toClass = ForeignTable.class, 
        owner = true, 
        reverseName = ForeignTable.P_Table, 
        cascadeSave = true, 
        cascadeDelete = true
    )
    public Hub<ForeignTable> getForeignTables() {
        if (hubForeignTables == null) {
            hubForeignTables = (Hub<ForeignTable>) getHub(P_ForeignTables);
        }
        return hubForeignTables;
    }
    @OAMany(
        toClass = Index.class, 
        owner = true, 
        reverseName = Index.P_Table, 
        cascadeSave = true, 
        cascadeDelete = true
    )
    public Hub<Index> getIndexes() {
        if (hubIndexes == null) {
            hubIndexes = (Hub<Index>) getHub(P_Indexes);
        }
        return hubIndexes;
    }
    @OAMany(
        displayName = "Query Tables", 
        toClass = QueryTable.class, 
        reverseName = QueryTable.P_Table
    )
    public Hub<QueryTable> getQueryTables() {
        if (hubQueryTables == null) {
            hubQueryTables = (Hub<QueryTable>) getHub(P_QueryTables);
        }
        return hubQueryTables;
    }
    @OAMany(
        toClass = Repository.class, 
        reverseName = Repository.P_MainTable
    )
    public Hub<Repository> getRepositories() {
        if (hubRepositories == null) {
            hubRepositories = (Hub<Repository>) getHub(P_Repositories);
        }
        return hubRepositories;
    }
    @OAMany(
        displayName = "Table Categories", 
        toClass = TableCategory.class, 
        recursive = false, 
        reverseName = TableCategory.P_Tables
    )
    @OALinkTable(name = "TableCategoryTable", indexName = "TableCategoryTable", columns = {"TableId"})
    public Hub<TableCategory> getTableCategories() {
        if (hubTableCategories == null) {
            hubTableCategories = (Hub<TableCategory>) getHub(P_TableCategories);
        }
        return hubTableCategories;
    }
    @OAMany(
        displayName = "Foreign Tables", 
        toClass = ForeignTable.class, 
        reverseName = ForeignTable.P_ToTable
    )
    public Hub<ForeignTable> getTableForeignTables() {
        if (hubTableForeignTables == null) {
            hubTableForeignTables = (Hub<ForeignTable>) getHub(P_TableForeignTables);
        }
        return hubTableForeignTables;
    }
    public void load(ResultSet rs, int id) throws SQLException {
        this.id = id;
        java.sql.Timestamp timestamp;
        timestamp = rs.getTimestamp(2);
        if (timestamp != null) this.created = new OADateTime(timestamp);
        this.name = rs.getString(3);
        this.newName = rs.getString(4);
        this.description = rs.getString(5);
        this.abbrevName = rs.getString(6);
        this.useType = (int) rs.getInt(7);
        if (rs.wasNull()) {
            OAObjectInfoDelegate.setPrimitiveNull(this, Table.P_UseType, true);
        }
        this.notes = rs.getString(8);
        int databaseFkey = rs.getInt(9);
        if (!rs.wasNull() && databaseFkey > 0) {
            setProperty(P_Database, new OAObjectKey(databaseFkey));
        }
        if (rs.getMetaData().getColumnCount() != 9) {
            throw new SQLException("invalid number of columns for load method");
        }

        this.changedFlag = false;
        this.newFlag = false;
    }
}
 
