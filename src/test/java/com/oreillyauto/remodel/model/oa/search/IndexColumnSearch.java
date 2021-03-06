// Generated by OABuilder
package com.oreillyauto.remodel.model.oa.search;

import javax.xml.bind.annotation.*;
import java.util.logging.*;

import com.oreillyauto.remodel.model.oa.*;
import com.oreillyauto.remodel.model.oa.propertypath.*;
import com.viaoa.annotation.*;
import com.viaoa.object.*;
import com.viaoa.hub.*;
import com.viaoa.util.*;
import com.viaoa.datasource.*;
import com.viaoa.filter.OAQueryFilter;

@OAClass(useDataSource=false, localOnly=true)
@XmlRootElement(name = "indexColumnSearch")
@XmlType(factoryMethod = "jaxbCreate")
@XmlAccessorType(XmlAccessType.PROPERTY)
public class IndexColumnSearch extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(IndexColumnSearch.class.getName());
    public static final String P_Column = "Column";
    public static final String P_UseColumnSearch = "UseColumnSearch";
    public static final String P_MaxResults = "MaxResults";

    protected Column column;
    protected boolean useColumnSearch;
    protected ColumnSearch searchColumn;
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
    public Column getColumn() {
        if (column == null) {
            column = (Column) getObject(P_Column);
        }
        return column;
    }
    public void setColumn(Column newValue) {
        Column old = this.column;
        this.column = newValue;
        firePropertyChange(P_Column, old, this.column);
    }
    public boolean getUseColumnSearch() {
        return useColumnSearch;
    }
    public void setUseColumnSearch(boolean newValue) {
        boolean old = this.useColumnSearch;
        this.useColumnSearch = newValue;
        firePropertyChange(P_UseColumnSearch, old, this.useColumnSearch);
    }
    public ColumnSearch getColumnSearch() {
        return this.searchColumn;
    }
    public void setColumnSearch(ColumnSearch newValue) {
        this.searchColumn = newValue;
    }

    public static IndexColumnSearch jaxbCreate() {
        IndexColumnSearch indexColumnSearch = (IndexColumnSearch) OAObject.jaxbCreateInstance(IndexColumnSearch.class);
        if (indexColumnSearch == null) indexColumnSearch = new IndexColumnSearch();
        return indexColumnSearch;
    }

    public void reset() {
        setColumn(null);
        setUseColumnSearch(false);
    }

    public boolean isDataEntered() {
        if (getColumn() != null) return true;
        if (getUseColumnSearch()) return true;
        return false;
    }

    protected String extraWhere;
    protected Object[] extraWhereParams;
    protected OAFilter<IndexColumn> filterExtraWhere;

    public void setExtraWhere(String s, Object ... args) {
        this.extraWhere = s;
        this.extraWhereParams = args;
        if (!OAString.isEmpty(s) && getExtraWhereFilter() == null) {
            OAFilter<IndexColumn> f = new OAQueryFilter<IndexColumn>(IndexColumn.class, s, args);
            setExtraWhereFilter(f);
        }
    }
    public void setExtraWhereFilter(OAFilter<IndexColumn> filter) {
        this.filterExtraWhere = filter;
    }
    public OAFilter<IndexColumn> getExtraWhereFilter() {
        return this.filterExtraWhere;
    }

    public OASelect<IndexColumn> getSelect() {
        final String prefix = "";
        String sql = "";
        String sortOrder = null;
        Object[] args = new Object[0];
        OAFinder finder = null;
        if (!useColumnSearch && getColumn() != null) {
            if (sql.length() > 0) sql += " AND ";
            sql += IndexColumnPP.column().pp + " = ?";
            args = OAArray.add(Object.class, args, getColumn());
            finder = new OAFinder<Column, IndexColumn>(getColumn(), Column.P_IndexColumns);
        }

        if (!OAString.isEmpty(extraWhere)) {
            if (sql.length() > 0) sql = "(" + sql + ") AND ";
            sql += extraWhere;
            args = OAArray.add(Object.class, args, extraWhereParams);
        }

        OASelect<IndexColumn> select = new OASelect<IndexColumn>(IndexColumn.class, sql, args, sortOrder);
        select.setDataSourceFilter(this.getDataSourceFilter());
        select.setFilter(this.getCustomFilter());
        select.setFinder(finder);
        if (getMaxResults() > 0) select.setMax(getMaxResults());
        if (useColumnSearch && getColumnSearch() != null) {
            getColumnSearch().appendSelect(IndexColumnPP.column().pp, select);
        }
        return select;
    }

    public void appendSelect(final String fromName, final OASelect select) {
        final String prefix = fromName + ".";
        String sql = "";
        Object[] args = new Object[0];
        if (!useColumnSearch && getColumn() != null) {
            if (sql.length() > 0) sql += " AND ";
            sql += prefix + IndexColumnPP.column().pp + " = ?";
            args = OAArray.add(Object.class, args, getColumn());
        }
        if (useColumnSearch && getColumnSearch() != null) {
            getColumnSearch().appendSelect(prefix + IndexColumnPP.column().pp, select);
        }
        select.add(sql, args);
    }

    private OAFilter<IndexColumn> filterDataSourceFilter;
    public OAFilter<IndexColumn> getDataSourceFilter() {
        if (filterDataSourceFilter != null) return filterDataSourceFilter;
        filterDataSourceFilter = new OAFilter<IndexColumn>() {
            @Override
            public boolean isUsed(IndexColumn indexColumn) {
                return IndexColumnSearch.this.isUsedForDataSourceFilter(indexColumn);
            }
        };
        return filterDataSourceFilter;
    }
    
    private OAFilter<IndexColumn> filterCustomFilter;
    public OAFilter<IndexColumn> getCustomFilter() {
        if (filterCustomFilter != null) return filterCustomFilter;
        filterCustomFilter = new OAFilter<IndexColumn>() {
            @Override
            public boolean isUsed(IndexColumn indexColumn) {
                boolean b = IndexColumnSearch.this.isUsedForCustomFilter(indexColumn);
                if (b && filterExtraWhere != null) b = filterExtraWhere.isUsed(indexColumn);
                return b;
            }
        };
        return filterCustomFilter;
    }
    
    public boolean isUsedForDataSourceFilter(IndexColumn searchIndexColumn) {
        if (column != null) {
            if (!OACompare.isEqual(searchIndexColumn.getColumn(), column)) return false;
        }
        if (useColumnSearch && getColumnSearch() != null) {
            Column column = (Column) searchIndexColumn.getProperty(IndexColumnPP.column().pp);
            if (column == null) return false;
            if (!getColumnSearch().isUsedForDataSourceFilter(column)) return false;
        }
        return true;
    }
    public boolean isUsedForCustomFilter(IndexColumn searchIndexColumn) {
        return true;
    }
}
