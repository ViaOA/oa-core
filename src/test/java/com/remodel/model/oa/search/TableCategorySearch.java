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
public class TableCategorySearch extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(TableCategorySearch.class.getName());
    public static final String P_Name = "Name";
    public static final String P_Table = "Table";
    public static final String P_UseTableSearch = "UseTableSearch";
    public static final String P_MaxResults = "MaxResults";

    protected String name;
    protected Table table;
    protected boolean useTableSearch;
    protected TableSearch searchTable;
    protected int maxResults;

    public String getName() {
        return name;
    }
    public void setName(String newValue) {
        String old = name;
        fireBeforePropertyChange(P_Name, old, newValue);
        this.name = newValue;
        firePropertyChange(P_Name, old, this.name);
    }
      

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

    public void reset() {
        setName(null);
        setTable(null);
        setUseTableSearch(false);
    }

    public boolean isDataEntered() {
        if (getName() != null) return true;
        if (getTable() != null) return true;
        if (getUseTableSearch()) return true;
        return false;
    }

    protected String extraWhere;
    protected Object[] extraWhereParams;
    protected OAFilter<TableCategory> filterExtraWhere;

    public void setExtraWhere(String s, Object ... args) {
        this.extraWhere = s;
        this.extraWhereParams = args;
        if (OAString.isNotEmpty(s) && getExtraWhereFilter() == null) {
            OAFilter<TableCategory> f = new OAQueryFilter<TableCategory>(TableCategory.class, s, args);
            setExtraWhereFilter(f);
        }
    }
    public void setExtraWhereFilter(OAFilter<TableCategory> filter) {
        this.filterExtraWhere = filter;
    }
    public OAFilter<TableCategory> getExtraWhereFilter() {
        return this.filterExtraWhere;
    }

    public OASelect<TableCategory> getSelect() {
        final String prefix = "";
        String sql = "";
        String sortOrder = null;
        Object[] args = new Object[0];
        OAFinder finder = null;
        if (OAString.isNotEmpty(this.name)) {
            if (sql.length() > 0) sql += " AND ";
            String value = OAString.convertToLikeSearch(name);
            if (value.indexOf("%") >= 0) {
                sql += TableCategory.P_Name + " LIKE ?";
            }
            else {
                sql += TableCategory.P_Name + " = ?";
            }
            args = OAArray.add(Object.class, args, value);
        }
        if (!useTableSearch && getTable() != null) {
            if (sql.length() > 0) sql += " AND ";
            sql += TableCategoryPP.tables().pp + " = ?";
            args = OAArray.add(Object.class, args, getTable());
            finder = new OAFinder<Table, TableCategory>(getTable(), Table.P_TableCategories);
        }

        if (OAString.isNotEmpty(extraWhere)) {
            if (sql.length() > 0) sql = "(" + sql + ") AND ";
            sql += extraWhere;
            args = OAArray.add(Object.class, args, extraWhereParams);
        }

        OASelect<TableCategory> select = new OASelect<TableCategory>(TableCategory.class, sql, args, sortOrder);
        if (getExtraWhereFilter() != null && getExtraWhereFilter().updateSelect(select)) {
            select.setFilter(new OAAndFilter(this.getCustomFilter(), getExtraWhereFilter()));
        }
        else select.setFilter(this.getCustomFilter());
        select.setDataSourceFilter(this.getDataSourceFilter());
        select.setFinder(finder);
        if (getMaxResults() > 0) select.setMax(getMaxResults());
        if (useTableSearch && getTableSearch() != null) {
            getTableSearch().appendSelect(TableCategoryPP.tables().pp, select);
        }
        return select;
    }

    public void appendSelect(final String fromName, final OASelect select) {
        final String prefix = fromName + ".";
        String sql = "";
        Object[] args = new Object[0];
        if (OAString.isNotEmpty(this.name)) {
            if (sql.length() > 0) sql += " AND ";
            String value = OAString.convertToLikeSearch(name);
            if (value.indexOf("%") >= 0) {
                sql += prefix + TableCategory.P_Name + " LIKE ?";
            }
            else {
                sql += prefix + TableCategory.P_Name + " = ?";
            }
            args = OAArray.add(Object.class, args, value);
        }
        if (!useTableSearch && getTable() != null) {
            if (sql.length() > 0) sql += " AND ";
            sql += prefix + TableCategoryPP.tables().pp + " = ?";
            args = OAArray.add(Object.class, args, getTable());
        }
        if (useTableSearch && getTableSearch() != null) {
            getTableSearch().appendSelect(prefix + TableCategoryPP.tables().pp, select);
        }
        select.add(sql, args);
    }

    private OAFilter<TableCategory> filterDataSourceFilter;
    public OAFilter<TableCategory> getDataSourceFilter() {
        if (filterDataSourceFilter != null) return filterDataSourceFilter;
        filterDataSourceFilter = new OAFilter<TableCategory>() {
            @Override
            public boolean isUsed(TableCategory tableCategory) {
                return TableCategorySearch.this.isUsedForDataSourceFilter(tableCategory);
            }
        };
        return filterDataSourceFilter;
    }
    
    private OAFilter<TableCategory> filterCustomFilter;
    public OAFilter<TableCategory> getCustomFilter() {
        if (filterCustomFilter != null) return filterCustomFilter;
        filterCustomFilter = new OAFilter<TableCategory>() {
            @Override
            public boolean isUsed(TableCategory tableCategory) {
                boolean b = TableCategorySearch.this.isUsedForCustomFilter(tableCategory);
                return b;
            }
        };
        return filterCustomFilter;
    }
    
    public boolean isUsedForDataSourceFilter(TableCategory searchTableCategory) {
        return true;
    }
    public boolean isUsedForCustomFilter(TableCategory searchTableCategory) {
        return true;
    }
}