// Generated by OABuilder
package com.oreillyauto.remodel.model.oa.search;

import java.util.logging.*;
import com.oreillyauto.remodel.model.oa.*;
import com.oreillyauto.remodel.model.oa.propertypath.*;
import com.viaoa.annotation.*;
import com.viaoa.object.*;
import com.viaoa.hub.*;
import com.viaoa.util.*;
import com.viaoa.datasource.*;
import com.viaoa.filter.*;

@OAClass(useDataSource=false, localOnly=true)
public class QuerySortSearch extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(QuerySortSearch.class.getName());
    public static final String P_MaxResults = "MaxResults";

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

    public void reset() {
    }

    public boolean isDataEntered() {
        return false;
    }

    protected String extraWhere;
    protected Object[] extraWhereParams;
    protected OAFilter<QuerySort> filterExtraWhere;

    public void setExtraWhere(String s, Object ... args) {
        this.extraWhere = s;
        this.extraWhereParams = args;
        if (OAString.isNotEmpty(s) && getExtraWhereFilter() == null) {
            OAFilter<QuerySort> f = new OAQueryFilter<QuerySort>(QuerySort.class, s, args);
            setExtraWhereFilter(f);
        }
    }
    public void setExtraWhereFilter(OAFilter<QuerySort> filter) {
        this.filterExtraWhere = filter;
    }
    public OAFilter<QuerySort> getExtraWhereFilter() {
        return this.filterExtraWhere;
    }

    public OASelect<QuerySort> getSelect() {
        final String prefix = "";
        String sql = "";
        String sortOrder = null;
        Object[] args = new Object[0];

        if (OAString.isNotEmpty(extraWhere)) {
            if (sql.length() > 0) sql = "(" + sql + ") AND ";
            sql += extraWhere;
            args = OAArray.add(Object.class, args, extraWhereParams);
        }

        OASelect<QuerySort> select = new OASelect<QuerySort>(QuerySort.class, sql, args, sortOrder);
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
        select.add(sql, args);
    }

    private OAFilter<QuerySort> filterDataSourceFilter;
    public OAFilter<QuerySort> getDataSourceFilter() {
        if (filterDataSourceFilter != null) return filterDataSourceFilter;
        filterDataSourceFilter = new OAFilter<QuerySort>() {
            @Override
            public boolean isUsed(QuerySort querySort) {
                return QuerySortSearch.this.isUsedForDataSourceFilter(querySort);
            }
        };
        return filterDataSourceFilter;
    }
    
    private OAFilter<QuerySort> filterCustomFilter;
    public OAFilter<QuerySort> getCustomFilter() {
        if (filterCustomFilter != null) return filterCustomFilter;
        filterCustomFilter = new OAFilter<QuerySort>() {
            @Override
            public boolean isUsed(QuerySort querySort) {
                boolean b = QuerySortSearch.this.isUsedForCustomFilter(querySort);
                return b;
            }
        };
        return filterCustomFilter;
    }
    
    public boolean isUsedForDataSourceFilter(QuerySort searchQuerySort) {
        return true;
    }
    public boolean isUsedForCustomFilter(QuerySort searchQuerySort) {
        return true;
    }
}