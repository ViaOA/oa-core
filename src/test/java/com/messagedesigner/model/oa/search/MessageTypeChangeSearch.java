// Generated by OABuilder
package com.messagedesigner.model.oa.search;

import java.util.logging.*;

import com.messagedesigner.model.oa.*;
import com.messagedesigner.model.oa.propertypath.*;
import com.messagedesigner.model.oa.MessageTypeChange;
import com.messagedesigner.model.oa.search.MessageTypeChangeSearch;
import com.viaoa.annotation.*;
import com.viaoa.object.*;
import com.viaoa.hub.*;
import com.viaoa.util.*;
import com.viaoa.datasource.*;
import com.viaoa.filter.*;

@OAClass(useDataSource=false, localOnly=true)
public class MessageTypeChangeSearch extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(MessageTypeChangeSearch.class.getName());
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
    protected OAFilter<MessageTypeChange> filterExtraWhere;

    public void setExtraWhere(String s, Object ... args) {
        this.extraWhere = s;
        this.extraWhereParams = args;
        if (OAString.isNotEmpty(s) && getExtraWhereFilter() == null) {
            OAFilter<MessageTypeChange> f = new OAQueryFilter<MessageTypeChange>(MessageTypeChange.class, s, args);
            setExtraWhereFilter(f);
        }
    }
    public void setExtraWhereFilter(OAFilter<MessageTypeChange> filter) {
        this.filterExtraWhere = filter;
    }
    public OAFilter<MessageTypeChange> getExtraWhereFilter() {
        return this.filterExtraWhere;
    }

    public OASelect<MessageTypeChange> getSelect() {
        final String prefix = "";
        String sql = "";
        String sortOrder = null;
        Object[] args = new Object[0];

        if (OAString.isNotEmpty(extraWhere)) {
            if (sql.length() > 0) sql = "(" + sql + ") AND ";
            sql += extraWhere;
            args = OAArray.add(Object.class, args, extraWhereParams);
        }

        OASelect<MessageTypeChange> select = new OASelect<MessageTypeChange>(MessageTypeChange.class, sql, args, sortOrder);
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

    private OAFilter<MessageTypeChange> filterDataSourceFilter;
    public OAFilter<MessageTypeChange> getDataSourceFilter() {
        if (filterDataSourceFilter != null) return filterDataSourceFilter;
        filterDataSourceFilter = new OAFilter<MessageTypeChange>() {
            @Override
            public boolean isUsed(MessageTypeChange messageTypeChange) {
                return MessageTypeChangeSearch.this.isUsedForDataSourceFilter(messageTypeChange);
            }
        };
        return filterDataSourceFilter;
    }
    
    private OAFilter<MessageTypeChange> filterCustomFilter;
    public OAFilter<MessageTypeChange> getCustomFilter() {
        if (filterCustomFilter != null) return filterCustomFilter;
        filterCustomFilter = new OAFilter<MessageTypeChange>() {
            @Override
            public boolean isUsed(MessageTypeChange messageTypeChange) {
                boolean b = MessageTypeChangeSearch.this.isUsedForCustomFilter(messageTypeChange);
                return b;
            }
        };
        return filterCustomFilter;
    }
    
    public boolean isUsedForDataSourceFilter(MessageTypeChange searchMessageTypeChange) {
        return true;
    }
    public boolean isUsedForCustomFilter(MessageTypeChange searchMessageTypeChange) {
        return true;
    }
}
