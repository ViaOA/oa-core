package com.test.pos.model.oa.search;

import com.viaoa.lang.*;
import com.viaoa.select.OASelect;
import java.util.*;
import java.util.logging.*;
import com.test.pos.model.oa.*;
import com.test.pos.model.oa.propertypath.*;
import com.viaoa.annotation.*;
import com.viaoa.object.*;
import com.viaoa.hub.*;
import com.viaoa.hub.filter.*;
import com.viaoa.datasource.*;
import com.viaoa.filter.*;
import com.viaoa.find.*;

@OAClass(useDataSource=false, localOnly=true)
public class StoreDayEndSearch extends OAObject {
    private static final long serialVersionUID = 1L;

    private static Logger LOG = Logger.getLogger(StoreDayEndSearch.class.getName());

    public static final String P_CustomQuery = "CustomQuery";
    public static final String P_MaxResults = "MaxResults";

    protected String customQuery;
    protected int maxResults;


    public String getCustomQuery() {
        return customQuery;
    }
    public void setCustomQuery(String newValue) {
        fireBeforePropertyChange(P_CustomQuery, this.customQuery, newValue);
        String old = customQuery;
        this.customQuery = newValue;
        firePropertyChange(P_CustomQuery, old, this.customQuery);
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

    public void reset() {
        setCustomQuery(null);
    }

    public boolean isDataEntered() {
        if (getCustomQuery() != null) return true;
        return false;
    }

    protected String extraWhere;
    protected Object[] extraWhereParams;
    protected OAFilter<StoreDayEnd> filterExtraWhere;

    public void setExtraWhere(String s, Object ... args) {
        this.extraWhere = s;
        this.extraWhereParams = args;
        if (OAString.isNotEmpty(s) && getExtraWhereFilter() == null) {
            OAFilter<StoreDayEnd> f = new OAQueryFilter<StoreDayEnd>(StoreDayEnd.class, s, args);
            setExtraWhereFilter(f);
        }
    }
    public void setExtraWhereFilter(OAFilter<StoreDayEnd> filter) {
        this.filterExtraWhere = filter;
    }
    public OAFilter<StoreDayEnd> getExtraWhereFilter() {
        return this.filterExtraWhere;
    }

    public OASelect<StoreDayEnd> getSelect() {
        final String prefix = "";
        String sql = "";
        String sortOrder = null;
        Object[] args = new Object[0];
    if (OAString.isNotEmpty(this.customQuery)) {
        if (sql.length() > 0) sql += " AND ";
        sql += "(" + getCustomQuery() + ")";
    }

        if (OAString.isNotEmpty(extraWhere)) {
            if (sql.length() > 0) sql = "(" + sql + ") AND ";
            sql += extraWhere;
            args = OAArray.add(Object.class, args, extraWhereParams);
        }

        OASelect<StoreDayEnd> select = new OASelect<StoreDayEnd>(StoreDayEnd.class, sql, args, sortOrder);
        if (getExtraWhereFilter() != null && getExtraWhereFilter().updateSelect(select)) {
            select.setFilter(new OAAndFilter(this.getCustomFilter(), getExtraWhereFilter()));
        }
        else select.setFilter(this.getCustomFilter());
        select.setDataSourceFilter(this.getDataSourceFilter());
        if (getMaxResults() > 0) select.setMax(getMaxResults());
        return select;
    }

    public void appendSelect(final String fromName, final OASelect select) {
        final String prefix = fromName + ".";
        String sql = "";
        Object[] args = new Object[0];
    if (OAString.isNotEmpty(this.customQuery)) {
        if (sql.length() > 0) sql += " AND ";
        sql += "(" + getCustomQuery() + ")";
    }
        select.add(sql, args);
    }

    private OAFilter<StoreDayEnd> filterDataSourceFilter;
    public OAFilter<StoreDayEnd> getDataSourceFilter() {
        if (filterDataSourceFilter != null) return filterDataSourceFilter;
        filterDataSourceFilter = new OAFilter<StoreDayEnd>() {
            @Override
            public boolean isUsed(StoreDayEnd storeDayEnd) {
                return StoreDayEndSearch.this.isUsedForDataSourceFilter(storeDayEnd);
            }
        };
        return filterDataSourceFilter;
    }
    
    private OAFilter<StoreDayEnd> filterCustomFilter;
    public OAFilter<StoreDayEnd> getCustomFilter() {
        if (filterCustomFilter != null) return filterCustomFilter;
        filterCustomFilter = new OAFilter<StoreDayEnd>() {
            @Override
            public boolean isUsed(StoreDayEnd storeDayEnd) {
                boolean b = StoreDayEndSearch.this.isUsedForCustomFilter(storeDayEnd);
                return b;
            }
        };
        return filterCustomFilter;
    }
    
    public boolean isUsedForDataSourceFilter(StoreDayEnd searchStoreDayEnd) {
        return true;
    }
    public boolean isUsedForCustomFilter(StoreDayEnd searchStoreDayEnd) {
        return true;
    }
}
