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
@XmlRootElement(name = "queryColumnSearch")
@XmlType(factoryMethod = "jaxbCreate")
@XmlAccessorType(XmlAccessType.PROPERTY)
public class QueryColumnSearch extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(QueryColumnSearch.class.getName());
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

    public static QueryColumnSearch jaxbCreate() {
        QueryColumnSearch queryColumnSearch = (QueryColumnSearch) OAObject.jaxbCreateInstance(QueryColumnSearch.class);
        if (queryColumnSearch == null) queryColumnSearch = new QueryColumnSearch();
        return queryColumnSearch;
    }

    public void reset() {
    }

    public boolean isDataEntered() {
        return false;
    }

    protected String extraWhere;
    protected Object[] extraWhereParams;
    protected OAFilter<QueryColumn> filterExtraWhere;

    public void setExtraWhere(String s, Object ... args) {
        this.extraWhere = s;
        this.extraWhereParams = args;
        if (!OAString.isEmpty(s) && getExtraWhereFilter() == null) {
            OAFilter<QueryColumn> f = new OAQueryFilter<QueryColumn>(QueryColumn.class, s, args);
            setExtraWhereFilter(f);
        }
    }
    public void setExtraWhereFilter(OAFilter<QueryColumn> filter) {
        this.filterExtraWhere = filter;
    }
    public OAFilter<QueryColumn> getExtraWhereFilter() {
        return this.filterExtraWhere;
    }

    public OASelect<QueryColumn> getSelect() {
        final String prefix = "";
        String sql = "";
        String sortOrder = null;
        Object[] args = new Object[0];

        if (!OAString.isEmpty(extraWhere)) {
            if (sql.length() > 0) sql = "(" + sql + ") AND ";
            sql += extraWhere;
            args = OAArray.add(Object.class, args, extraWhereParams);
        }

        OASelect<QueryColumn> select = new OASelect<QueryColumn>(QueryColumn.class, sql, args, sortOrder);
        select.setDataSourceFilter(this.getDataSourceFilter());
        select.setFilter(this.getCustomFilter());
        if (getMaxResults() > 0) select.setMax(getMaxResults());
        return select;
    }

    public void appendSelect(final String fromName, final OASelect select) {
        final String prefix = fromName + ".";
        String sql = "";
        Object[] args = new Object[0];
        select.add(sql, args);
    }

    private OAFilter<QueryColumn> filterDataSourceFilter;
    public OAFilter<QueryColumn> getDataSourceFilter() {
        if (filterDataSourceFilter != null) return filterDataSourceFilter;
        filterDataSourceFilter = new OAFilter<QueryColumn>() {
            @Override
            public boolean isUsed(QueryColumn queryColumn) {
                return QueryColumnSearch.this.isUsedForDataSourceFilter(queryColumn);
            }
        };
        return filterDataSourceFilter;
    }
    
    private OAFilter<QueryColumn> filterCustomFilter;
    public OAFilter<QueryColumn> getCustomFilter() {
        if (filterCustomFilter != null) return filterCustomFilter;
        filterCustomFilter = new OAFilter<QueryColumn>() {
            @Override
            public boolean isUsed(QueryColumn queryColumn) {
                boolean b = QueryColumnSearch.this.isUsedForCustomFilter(queryColumn);
                if (b && filterExtraWhere != null) b = filterExtraWhere.isUsed(queryColumn);
                return b;
            }
        };
        return filterCustomFilter;
    }
    
    public boolean isUsedForDataSourceFilter(QueryColumn searchQueryColumn) {
        return true;
    }
    public boolean isUsedForCustomFilter(QueryColumn searchQueryColumn) {
        return true;
    }
}