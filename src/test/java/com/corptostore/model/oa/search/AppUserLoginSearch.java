// Generated by OABuilder
package com.corptostore.model.oa.search;

import java.util.*;
import java.util.logging.*;

import com.corptostore.model.oa.*;
import com.corptostore.model.oa.propertypath.*;
import com.corptostore.model.oa.AppUserLogin;
import com.corptostore.model.oa.search.AppUserLoginSearch;
import com.viaoa.annotation.*;
import com.viaoa.object.*;
import com.viaoa.hub.*;
import com.viaoa.util.*;
import com.viaoa.datasource.*;
import com.viaoa.filter.*;

@OAClass(useDataSource=false, localOnly=true)
public class AppUserLoginSearch extends OAObject {
    private static final long serialVersionUID = 1L;

    private static Logger LOG = Logger.getLogger(AppUserLoginSearch.class.getName());

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
    protected OAFilter<AppUserLogin> filterExtraWhere;

    public void setExtraWhere(String s, Object ... args) {
        this.extraWhere = s;
        this.extraWhereParams = args;
        if (OAString.isNotEmpty(s) && getExtraWhereFilter() == null) {
            OAFilter<AppUserLogin> f = new OAQueryFilter<AppUserLogin>(AppUserLogin.class, s, args);
            setExtraWhereFilter(f);
        }
    }
    public void setExtraWhereFilter(OAFilter<AppUserLogin> filter) {
        this.filterExtraWhere = filter;
    }
    public OAFilter<AppUserLogin> getExtraWhereFilter() {
        return this.filterExtraWhere;
    }

    public OASelect<AppUserLogin> getSelect() {
        final String prefix = "";
        String sql = "";
        String sortOrder = null;
        Object[] args = new Object[0];

        if (OAString.isNotEmpty(extraWhere)) {
            if (sql.length() > 0) sql = "(" + sql + ") AND ";
            sql += extraWhere;
            args = OAArray.add(Object.class, args, extraWhereParams);
        }

        OASelect<AppUserLogin> select = new OASelect<AppUserLogin>(AppUserLogin.class, sql, args, sortOrder);
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

    private OAFilter<AppUserLogin> filterDataSourceFilter;
    public OAFilter<AppUserLogin> getDataSourceFilter() {
        if (filterDataSourceFilter != null) return filterDataSourceFilter;
        filterDataSourceFilter = new OAFilter<AppUserLogin>() {
            @Override
            public boolean isUsed(AppUserLogin appUserLogin) {
                return AppUserLoginSearch.this.isUsedForDataSourceFilter(appUserLogin);
            }
        };
        return filterDataSourceFilter;
    }
    
    private OAFilter<AppUserLogin> filterCustomFilter;
    public OAFilter<AppUserLogin> getCustomFilter() {
        if (filterCustomFilter != null) return filterCustomFilter;
        filterCustomFilter = new OAFilter<AppUserLogin>() {
            @Override
            public boolean isUsed(AppUserLogin appUserLogin) {
                boolean b = AppUserLoginSearch.this.isUsedForCustomFilter(appUserLogin);
                return b;
            }
        };
        return filterCustomFilter;
    }
    
    public boolean isUsedForDataSourceFilter(AppUserLogin searchAppUserLogin) {
        return true;
    }
    public boolean isUsedForCustomFilter(AppUserLogin searchAppUserLogin) {
        return true;
    }
}