// Generated by OABuilder
package com.remodel.model.oa.search;

import java.util.logging.*;

import com.remodel.model.oa.*;
import com.remodel.model.oa.propertypath.*;
import com.viaoa.annotation.*;
import com.viaoa.object.*;
import com.viaoa.hub.*;
import com.viaoa.util.*;
import com.viaoa.datasource.*;
import com.viaoa.filter.*;

@OAClass(useDataSource=false, localOnly=true)
public class QueryTableSearch extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(QueryTableSearch.class.getName());
    public static final String P_Table = "Table";
    public static final String P_UseTableSearch = "UseTableSearch";
    public static final String P_JoinTable = "JoinTable";
    public static final String P_UseJoinTableSearch = "UseJoinTableSearch";
    public static final String P_MaxResults = "MaxResults";

    protected Table table;
    protected boolean useTableSearch;
    protected TableSearch searchTable;
    protected QueryTable joinTable;
    protected boolean useJoinTableSearch;
    protected QueryTableSearch searchJoinTable;
    protected int maxResults;

    public int getMaxResults() {
        return maxResults;
    }
    public void setMaxResults(int newValue) {
        fireBeforePropertyChange(P_MaxResults, this.maxResults, newValue);
        int old = maxResults;
        this.maxResults = newValue;
        firePropertyChange(P_MaxResults, old, this.maxResults);
    }

    @OAOne
    public Table getTable() {
        if (table == null) {
            table = (Table) getObject(P_Table);
        }
        return table;
    }
    public void setTable(Table newValue) {
        Table old = this.table;
        this.table = newValue;
        firePropertyChange(P_Table, old, this.table);
    }
    public boolean getUseTableSearch() {
        return useTableSearch;
    }
    public void setUseTableSearch(boolean newValue) {
        boolean old = this.useTableSearch;
        this.useTableSearch = newValue;
        firePropertyChange(P_UseTableSearch, old, this.useTableSearch);
    }
    public TableSearch getTableSearch() {
        return this.searchTable;
    }
    public void setTableSearch(TableSearch newValue) {
        this.searchTable = newValue;
    }

    @OAOne
    public QueryTable getJoinTable() {
        if (joinTable == null) {
            joinTable = (QueryTable) getObject(P_JoinTable);
        }
        return joinTable;
    }
    public void setJoinTable(QueryTable newValue) {
        QueryTable old = this.joinTable;
        this.joinTable = newValue;
        firePropertyChange(P_JoinTable, old, this.joinTable);
    }
    public boolean getUseJoinTableSearch() {
        return useJoinTableSearch;
    }
    public void setUseJoinTableSearch(boolean newValue) {
        boolean old = this.useJoinTableSearch;
        this.useJoinTableSearch = newValue;
        firePropertyChange(P_UseJoinTableSearch, old, this.useJoinTableSearch);
    }
    public QueryTableSearch getJoinTableSearch() {
        return this.searchJoinTable;
    }
    public void setJoinTableSearch(QueryTableSearch newValue) {
        this.searchJoinTable = newValue;
    }

    public void reset() {
        setTable(null);
        setUseTableSearch(false);
        setJoinTable(null);
        setUseJoinTableSearch(false);
    }

    public boolean isDataEntered() {
        if (getTable() != null) return true;
        if (getUseTableSearch()) return true;
        if (getJoinTable() != null) return true;
        if (getUseJoinTableSearch()) return true;
        return false;
    }

    protected String extraWhere;
    protected Object[] extraWhereParams;
    protected OAFilter<QueryTable> filterExtraWhere;

    public void setExtraWhere(String s, Object ... args) {
        this.extraWhere = s;
        this.extraWhereParams = args;
        if (OAString.isNotEmpty(s) && getExtraWhereFilter() == null) {
            OAFilter<QueryTable> f = new OAQueryFilter<QueryTable>(QueryTable.class, s, args);
            setExtraWhereFilter(f);
        }
    }
    public void setExtraWhereFilter(OAFilter<QueryTable> filter) {
        this.filterExtraWhere = filter;
    }
    public OAFilter<QueryTable> getExtraWhereFilter() {
        return this.filterExtraWhere;
    }

    public OASelect<QueryTable> getSelect() {
        final String prefix = "";
        String sql = "";
        String sortOrder = null;
        Object[] args = new Object[0];
        OAFinder finder = null;
        if (!useTableSearch && getTable() != null) {
            if (sql.length() > 0) sql += " AND ";
            sql += QueryTablePP.table().pp + " = ?";
            args = OAArray.add(Object.class, args, getTable());
            finder = new OAFinder<Table, QueryTable>(getTable(), Table.P_QueryTables);
        }
        if (!useJoinTableSearch && getJoinTable() != null) {
            if (sql.length() > 0) sql += " AND ";
            sql += QueryTablePP.joinTable().pp + " = ?";
            args = OAArray.add(Object.class, args, getJoinTable());
            finder = new OAFinder<QueryTable, QueryTable>(getJoinTable(), QueryTable.P_JoinedTables);
        }

        if (OAString.isNotEmpty(extraWhere)) {
            if (sql.length() > 0) sql = "(" + sql + ") AND ";
            sql += extraWhere;
            args = OAArray.add(Object.class, args, extraWhereParams);
        }

        OASelect<QueryTable> select = new OASelect<QueryTable>(QueryTable.class, sql, args, sortOrder);
        if (getExtraWhereFilter() != null && getExtraWhereFilter().updateSelect(select)) {
            select.setFilter(new OAAndFilter(this.getCustomFilter(), getExtraWhereFilter()));
        }
        else select.setFilter(this.getCustomFilter());
        select.setDataSourceFilter(this.getDataSourceFilter());
        select.setFinder(finder);
        if (getMaxResults() > 0) select.setMax(getMaxResults());
        if (useTableSearch && getTableSearch() != null) {
            getTableSearch().appendSelect(QueryTablePP.table().pp, select);
        }
        if (useJoinTableSearch && getJoinTableSearch() != null) {
            getJoinTableSearch().appendSelect(QueryTablePP.joinTable().pp, select);
        }
        return select;
    }

    public void appendSelect(final String fromName, final OASelect select) {
        final String prefix = fromName + ".";
        String sql = "";
        Object[] args = new Object[0];
        if (!useTableSearch && getTable() != null) {
            if (sql.length() > 0) sql += " AND ";
            sql += prefix + QueryTablePP.table().pp + " = ?";
            args = OAArray.add(Object.class, args, getTable());
        }
        if (useTableSearch && getTableSearch() != null) {
            getTableSearch().appendSelect(prefix + QueryTablePP.table().pp, select);
        }
        if (!useJoinTableSearch && getJoinTable() != null) {
            if (sql.length() > 0) sql += " AND ";
            sql += prefix + QueryTablePP.joinTable().pp + " = ?";
            args = OAArray.add(Object.class, args, getJoinTable());
        }
        if (useJoinTableSearch && getJoinTableSearch() != null) {
            getJoinTableSearch().appendSelect(prefix + QueryTablePP.joinTable().pp, select);
        }
        select.add(sql, args);
    }

    private OAFilter<QueryTable> filterDataSourceFilter;
    public OAFilter<QueryTable> getDataSourceFilter() {
        if (filterDataSourceFilter != null) return filterDataSourceFilter;
        filterDataSourceFilter = new OAFilter<QueryTable>() {
            @Override
            public boolean isUsed(QueryTable queryTable) {
                return QueryTableSearch.this.isUsedForDataSourceFilter(queryTable);
            }
        };
        return filterDataSourceFilter;
    }
    
    private OAFilter<QueryTable> filterCustomFilter;
    public OAFilter<QueryTable> getCustomFilter() {
        if (filterCustomFilter != null) return filterCustomFilter;
        filterCustomFilter = new OAFilter<QueryTable>() {
            @Override
            public boolean isUsed(QueryTable queryTable) {
                boolean b = QueryTableSearch.this.isUsedForCustomFilter(queryTable);
                return b;
            }
        };
        return filterCustomFilter;
    }
    
    public boolean isUsedForDataSourceFilter(QueryTable searchQueryTable) {
        return true;
    }
    public boolean isUsedForCustomFilter(QueryTable searchQueryTable) {
        return true;
    }
}