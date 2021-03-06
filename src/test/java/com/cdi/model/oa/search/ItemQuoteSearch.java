// Generated by OABuilder
package com.cdi.model.oa.search;

import java.util.logging.*;
import com.cdi.model.oa.*;
import com.cdi.model.oa.propertypath.*;
import com.viaoa.annotation.*;
import com.viaoa.datasource.*;
import com.viaoa.object.*;
import com.viaoa.hub.*;
import com.viaoa.util.*;
import com.viaoa.filter.OAQueryFilter;

@OAClass(useDataSource=false, localOnly=true)
public class ItemQuoteSearch extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(ItemQuoteSearch.class.getName());
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
    protected OAFilter<ItemQuote> filterExtraWhere;

    public void setExtraWhere(String s, Object ... args) {
        this.extraWhere = s;
        this.extraWhereParams = args;
        if (!OAString.isEmpty(s) && getExtraWhereFilter() == null) {
            OAFilter<ItemQuote> f = new OAQueryFilter<ItemQuote>(ItemQuote.class, s, args);
            setExtraWhereFilter(f);
        }
    }
    public void setExtraWhereFilter(OAFilter<ItemQuote> filter) {
        this.filterExtraWhere = filter;
    }
    public OAFilter<ItemQuote> getExtraWhereFilter() {
        return this.filterExtraWhere;
    }

    public OASelect<ItemQuote> getSelect() {
        String sql = "";
        String sortOrder = null;
        Object[] args = new Object[0];

        if (!OAString.isEmpty(extraWhere)) {
            if (sql.length() > 0) sql = "(" + sql + ") AND ";
            sql += extraWhere;
            args = OAArray.add(Object.class, args, extraWhereParams);
        }

        OASelect<ItemQuote> select = new OASelect<ItemQuote>(ItemQuote.class, sql, args, sortOrder);
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

    private OAFilter<ItemQuote> filterDataSourceFilter;
    public OAFilter<ItemQuote> getDataSourceFilter() {
        if (filterDataSourceFilter != null) return filterDataSourceFilter;
        filterDataSourceFilter = new OAFilter<ItemQuote>() {
            @Override
            public boolean isUsed(ItemQuote itemQuote) {
                return ItemQuoteSearch.this.isUsedForDataSourceFilter(itemQuote);
            }
        };
        return filterDataSourceFilter;
    }
    
    private OAFilter<ItemQuote> filterCustomFilter;
    public OAFilter<ItemQuote> getCustomFilter() {
        if (filterCustomFilter != null) return filterCustomFilter;
        filterCustomFilter = new OAFilter<ItemQuote>() {
            @Override
            public boolean isUsed(ItemQuote itemQuote) {
                boolean b = ItemQuoteSearch.this.isUsedForCustomFilter(itemQuote);
                if (b && filterExtraWhere != null) b = filterExtraWhere.isUsed(itemQuote);
                return b;
            }
        };
        return filterCustomFilter;
    }
    
    public boolean isUsedForDataSourceFilter(ItemQuote searchItemQuote) {
        return true;
    }
    public boolean isUsedForCustomFilter(ItemQuote searchItemQuote) {
        return true;
    }
}
