// Generated by OABuilder
package com.corptostore.model.oa.search;

import java.util.*;
import java.util.logging.*;

import com.corptostore.model.oa.*;
import com.corptostore.model.oa.propertypath.*;
import com.corptostore.model.oa.DashboardLine;
import com.corptostore.model.oa.search.DashboardLineSearch;
import com.viaoa.annotation.*;
import com.viaoa.object.*;
import com.viaoa.hub.*;
import com.viaoa.util.*;
import com.viaoa.datasource.*;
import com.viaoa.filter.*;

@OAClass(useDataSource=false, localOnly=true)
public class DashboardLineSearch extends OAObject {
    private static final long serialVersionUID = 1L;

    private static Logger LOG = Logger.getLogger(DashboardLineSearch.class.getName());

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
    protected OAFilter<DashboardLine> filterExtraWhere;

    public void setExtraWhere(String s, Object ... args) {
        this.extraWhere = s;
        this.extraWhereParams = args;
        if (OAString.isNotEmpty(s) && getExtraWhereFilter() == null) {
            OAFilter<DashboardLine> f = new OAQueryFilter<DashboardLine>(DashboardLine.class, s, args);
            setExtraWhereFilter(f);
        }
    }
    public void setExtraWhereFilter(OAFilter<DashboardLine> filter) {
        this.filterExtraWhere = filter;
    }
    public OAFilter<DashboardLine> getExtraWhereFilter() {
        return this.filterExtraWhere;
    }

    public OASelect<DashboardLine> getSelect() {
        final String prefix = "";
        String sql = "";
        String sortOrder = null;
        Object[] args = new Object[0];

        if (OAString.isNotEmpty(extraWhere)) {
            if (sql.length() > 0) sql = "(" + sql + ") AND ";
            sql += extraWhere;
            args = OAArray.add(Object.class, args, extraWhereParams);
        }

        OASelect<DashboardLine> select = new OASelect<DashboardLine>(DashboardLine.class, sql, args, sortOrder);
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

    private OAFilter<DashboardLine> filterDataSourceFilter;
    public OAFilter<DashboardLine> getDataSourceFilter() {
        if (filterDataSourceFilter != null) return filterDataSourceFilter;
        filterDataSourceFilter = new OAFilter<DashboardLine>() {
            @Override
            public boolean isUsed(DashboardLine dashboardLine) {
                return DashboardLineSearch.this.isUsedForDataSourceFilter(dashboardLine);
            }
        };
        return filterDataSourceFilter;
    }
    
    private OAFilter<DashboardLine> filterCustomFilter;
    public OAFilter<DashboardLine> getCustomFilter() {
        if (filterCustomFilter != null) return filterCustomFilter;
        filterCustomFilter = new OAFilter<DashboardLine>() {
            @Override
            public boolean isUsed(DashboardLine dashboardLine) {
                boolean b = DashboardLineSearch.this.isUsedForCustomFilter(dashboardLine);
                return b;
            }
        };
        return filterCustomFilter;
    }
    
    public boolean isUsedForDataSourceFilter(DashboardLine searchDashboardLine) {
        return true;
    }
    public boolean isUsedForCustomFilter(DashboardLine searchDashboardLine) {
        return true;
    }
}