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
    lowerName = "querySort",
    pluralName = "QuerySorts",
    shortName = "qrs",
    displayName = "Query Sort",
    displayProperty = "column"
)
@OATable(
    indexes = {
        @OAIndex(name = "QuerySortColumn", fkey = true, columns = { @OAIndexColumn(name = "ColumnId") }), 
        @OAIndex(name = "QuerySortQueryInfo", fkey = true, columns = { @OAIndexColumn(name = "QueryInfoId") })
    }
)
public class QuerySort extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(QuerySort.class.getName());

    public static final String P_Id = "Id";
    public static final String P_Created = "Created";
    public static final String P_Seq = "Seq";
     
     
    public static final String P_Column = "Column";
    public static final String P_QueryInfo = "QueryInfo";
     
    protected volatile int id;
    protected volatile OADateTime created;
    protected volatile int seq;
     
    // Links to other objects.
    protected volatile transient Column column;
    protected volatile transient QueryInfo queryInfo;
     
    public QuerySort() {
        if (!isLoading()) setObjectDefaults();
    }
    @Override
    public void setObjectDefaults() {
        setCreated(new OADateTime());
    }
     
    public QuerySort(int id) {
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
    @OAOne(
        reverseName = Column.P_QuerySorts, 
        required = true, 
        allowCreateNew = false
    )
    @OAFkey(columns = {"ColumnId"})
    public Column getColumn() {
        if (column == null) {
            column = (Column) getObject(P_Column);
        }
        return column;
    }
    public void setColumn(Column newValue) {
        Column old = this.column;
        fireBeforePropertyChange(P_Column, old, newValue);
        this.column = newValue;
        firePropertyChange(P_Column, old, this.column);
    }
    @OAOne(
        displayName = "Query Info", 
        reverseName = QueryInfo.P_QuerySorts, 
        required = true, 
        allowCreateNew = false
    )
    @OAFkey(columns = {"QueryInfoId"})
    public QueryInfo getQueryInfo() {
        if (queryInfo == null) {
            queryInfo = (QueryInfo) getObject(P_QueryInfo);
        }
        return queryInfo;
    }
    public void setQueryInfo(QueryInfo newValue) {
        QueryInfo old = this.queryInfo;
        fireBeforePropertyChange(P_QueryInfo, old, newValue);
        this.queryInfo = newValue;
        firePropertyChange(P_QueryInfo, old, this.queryInfo);
    }
    public void load(ResultSet rs, int id) throws SQLException {
        this.id = id;
        java.sql.Timestamp timestamp;
        timestamp = rs.getTimestamp(2);
        if (timestamp != null) this.created = new OADateTime(timestamp);
        this.seq = (int) rs.getInt(3);
        if (rs.wasNull()) {
            OAObjectInfoDelegate.setPrimitiveNull(this, QuerySort.P_Seq, true);
        }
        int columnFkey = rs.getInt(4);
        if (!rs.wasNull() && columnFkey > 0) {
            setProperty(P_Column, new OAObjectKey(columnFkey));
        }
        int queryInfoFkey = rs.getInt(5);
        if (!rs.wasNull() && queryInfoFkey > 0) {
            setProperty(P_QueryInfo, new OAObjectKey(queryInfoFkey));
        }
        if (rs.getMetaData().getColumnCount() != 5) {
            throw new SQLException("invalid number of columns for load method");
        }

        this.changedFlag = false;
        this.newFlag = false;
    }
}
 