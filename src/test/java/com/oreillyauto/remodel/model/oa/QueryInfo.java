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
    lowerName = "queryInfo",
    pluralName = "QueryInfos",
    shortName = "qri",
    displayName = "Query Info",
    displayProperty = "name",
    rootTreePropertyPaths = {
        "[Project]."+Project.P_Repositories+"."+Repository.P_QueryInfos
    }
)
@OATable(
    indexes = {
        @OAIndex(name = "QueryInfoRepository", fkey = true, columns = { @OAIndexColumn(name = "RepositoryId") })
    }
)
public class QueryInfo extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(QueryInfo.class.getName());

    public static final String P_Id = "Id";
    public static final String P_Created = "Created";
    public static final String P_Name = "Name";
    public static final String P_SqlCode = "SqlCode";
    public static final String P_Seq = "Seq";
    public static final String P_AllowPaging = "AllowPaging";
     
     
    public static final String P_QuerySorts = "QuerySorts";
    public static final String P_QueryTables = "QueryTables";
    public static final String P_Repository = "Repository";
     
    protected volatile int id;
    protected volatile OADateTime created;
    protected volatile String name;
    protected volatile String sqlCode;
    protected volatile int seq;
    protected volatile boolean allowPaging;
     
    // Links to other objects.
    protected transient Hub<QuerySort> hubQuerySorts;
    protected transient Hub<QueryTable> hubQueryTables;
    protected volatile transient Repository repository;
     
    public QueryInfo() {
        if (!isLoading()) setObjectDefaults();
    }
    @Override
    public void setObjectDefaults() {
        setCreated(new OADateTime());
    }
     
    public QueryInfo(int id) {
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
    @OAProperty(maxLength = 254, displayLength = 20)
    @OAColumn(maxLength = 254)
    public String getName() {
        return name;
    }
    public void setName(String newValue) {
        String old = name;
        fireBeforePropertyChange(P_Name, old, newValue);
        this.name = newValue;
        firePropertyChange(P_Name, old, this.name);
    }
    @OAProperty(displayName = "Sql Code", displayLength = 30, columnLength = 20)
    @OAColumn(sqlType = java.sql.Types.CLOB)
    public String getSqlCode() {
        return sqlCode;
    }
    public void setSqlCode(String newValue) {
        String old = sqlCode;
        fireBeforePropertyChange(P_SqlCode, old, newValue);
        this.sqlCode = newValue;
        firePropertyChange(P_SqlCode, old, this.sqlCode);
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
    @OAProperty(displayName = "Allow Paging", displayLength = 5, columnLength = 12)
    @OAColumn(sqlType = java.sql.Types.BOOLEAN)
    public boolean getAllowPaging() {
        return allowPaging;
    }
    public boolean isAllowPaging() {
        return getAllowPaging();
    }
    public void setAllowPaging(boolean newValue) {
        boolean old = allowPaging;
        fireBeforePropertyChange(P_AllowPaging, old, newValue);
        this.allowPaging = newValue;
        firePropertyChange(P_AllowPaging, old, this.allowPaging);
    }
    @OAMany(
        displayName = "Query Sorts", 
        toClass = QuerySort.class, 
        owner = true, 
        reverseName = QuerySort.P_QueryInfo, 
        cascadeSave = true, 
        cascadeDelete = true, 
        seqProperty = QuerySort.P_Seq, 
        sortProperty = QuerySort.P_Seq
    )
    public Hub<QuerySort> getQuerySorts() {
        if (hubQuerySorts == null) {
            hubQuerySorts = (Hub<QuerySort>) getHub(P_QuerySorts);
        }
        return hubQuerySorts;
    }
    @OAMany(
        displayName = "Query Tables", 
        toClass = QueryTable.class, 
        owner = true, 
        reverseName = QueryTable.P_QueryInfo, 
        cascadeSave = true, 
        cascadeDelete = true, 
        uniqueProperty = QueryTable.P_Table
    )
    public Hub<QueryTable> getQueryTables() {
        if (hubQueryTables == null) {
            hubQueryTables = (Hub<QueryTable>) getHub(P_QueryTables);
        }
        return hubQueryTables;
    }
    @OAOne(
        reverseName = Repository.P_QueryInfos, 
        required = true, 
        allowCreateNew = false
    )
    @OAFkey(columns = {"RepositoryId"})
    public Repository getRepository() {
        if (repository == null) {
            repository = (Repository) getObject(P_Repository);
        }
        return repository;
    }
    public void setRepository(Repository newValue) {
        Repository old = this.repository;
        fireBeforePropertyChange(P_Repository, old, newValue);
        this.repository = newValue;
        firePropertyChange(P_Repository, old, this.repository);
    }
    public void load(ResultSet rs, int id) throws SQLException {
        this.id = id;
        java.sql.Timestamp timestamp;
        timestamp = rs.getTimestamp(2);
        if (timestamp != null) this.created = new OADateTime(timestamp);
        this.name = rs.getString(3);
        this.sqlCode = rs.getString(4);
        this.seq = (int) rs.getInt(5);
        if (rs.wasNull()) {
            OAObjectInfoDelegate.setPrimitiveNull(this, QueryInfo.P_Seq, true);
        }
        this.allowPaging = rs.getBoolean(6);
        if (rs.wasNull()) {
            OAObjectInfoDelegate.setPrimitiveNull(this, QueryInfo.P_AllowPaging, true);
        }
        int repositoryFkey = rs.getInt(7);
        if (!rs.wasNull() && repositoryFkey > 0) {
            setProperty(P_Repository, new OAObjectKey(repositoryFkey));
        }
        if (rs.getMetaData().getColumnCount() != 7) {
            throw new SQLException("invalid number of columns for load method");
        }

        this.changedFlag = false;
        this.newFlag = false;
    }
}
 