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
import com.viaoa.util.OADate;
import com.viaoa.util.OADateTime;
import com.viaoa.filter.OAQueryFilter;

@OAClass(useDataSource=false, localOnly=true)
public class WebPageSearch extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(WebPageSearch.class.getName());
    public static final String P_Created = "Created";
    public static final String P_URL = "URL";
    public static final String P_LastChanged = "LastChanged";
    public static final String P_Title = "Title";
    public static final String P_MaxResults = "MaxResults";

    protected OADate created;
    protected String url;
    protected OADateTime lastChanged;
    protected String title;
    protected int maxResults;

    public OADate getCreated() {
        return created;
    }
    public void setCreated(OADate newValue) {
        OADate old = created;
        fireBeforePropertyChange(P_Created, old, newValue);
        this.created = newValue;
        firePropertyChange(P_Created, old, this.created);
    }
      

    public String getURL() {
        return url;
    }
    public void setURL(String newValue) {
        String old = url;
        fireBeforePropertyChange(P_URL, old, newValue);
        this.url = newValue;
        firePropertyChange(P_URL, old, this.url);
    }
      

    public OADateTime getLastChanged() {
        return lastChanged;
    }
    public void setLastChanged(OADateTime newValue) {
        OADateTime old = lastChanged;
        fireBeforePropertyChange(P_LastChanged, old, newValue);
        this.lastChanged = newValue;
        firePropertyChange(P_LastChanged, old, this.lastChanged);
    }
      

    public String getTitle() {
        return title;
    }
    public void setTitle(String newValue) {
        String old = title;
        fireBeforePropertyChange(P_Title, old, newValue);
        this.title = newValue;
        firePropertyChange(P_Title, old, this.title);
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
        setCreated(null);
        setURL(null);
        setLastChanged(null);
        setTitle(null);
    }

    public boolean isDataEntered() {
        if (getCreated() != null) return true;
        if (getURL() != null) return true;
        if (getLastChanged() != null) return true;
        if (getTitle() != null) return true;
        return false;
    }

    protected String extraWhere;
    protected Object[] extraWhereParams;
    protected OAFilter<WebPage> filterExtraWhere;

    public void setExtraWhere(String s, Object ... args) {
        this.extraWhere = s;
        this.extraWhereParams = args;
        if (!OAString.isEmpty(s) && getExtraWhereFilter() == null) {
            OAFilter<WebPage> f = new OAQueryFilter<WebPage>(WebPage.class, s, args);
            setExtraWhereFilter(f);
        }
    }
    public void setExtraWhereFilter(OAFilter<WebPage> filter) {
        this.filterExtraWhere = filter;
    }
    public OAFilter<WebPage> getExtraWhereFilter() {
        return this.filterExtraWhere;
    }

    public OASelect<WebPage> getSelect() {
        String sql = "";
        String sortOrder = null;
        Object[] args = new Object[0];
        if (created != null) {
            if (sql.length() > 0) sql += " AND ";
            sql += WebPage.P_Created + " = ?";
            args = OAArray.add(Object.class, args, this.created);
        }
        if (!OAString.isEmpty(this.url)) {
            if (sql.length() > 0) sql += " AND ";
            String value = this.url.replace("*", "%");
            if (!value.endsWith("%")) value += "%";
            if (value.indexOf("%") >= 0) {
                sql += WebPage.P_URL + " LIKE ?";
            }
            else {
                sql += WebPage.P_URL + " = ?";
            }
            args = OAArray.add(Object.class, args, value);
        }
        if (lastChanged != null) {
            if (sql.length() > 0) sql += " AND ";
            sql += WebPage.P_LastChanged + " = ?";
            args = OAArray.add(Object.class, args, this.lastChanged);
        }
        if (!OAString.isEmpty(this.title)) {
            if (sql.length() > 0) sql += " AND ";
            String value = this.title.replace("*", "%");
            if (!value.endsWith("%")) value += "%";
            if (value.indexOf("%") >= 0) {
                sql += WebPage.P_Title + " LIKE ?";
            }
            else {
                sql += WebPage.P_Title + " = ?";
            }
            args = OAArray.add(Object.class, args, value);
        }

        if (!OAString.isEmpty(extraWhere)) {
            if (sql.length() > 0) sql = "(" + sql + ") AND ";
            sql += extraWhere;
            args = OAArray.add(Object.class, args, extraWhereParams);
        }

        OASelect<WebPage> select = new OASelect<WebPage>(WebPage.class, sql, args, sortOrder);
        select.setDataSourceFilter(this.getDataSourceFilter());
        select.setFilter(this.getCustomFilter());
        if (getMaxResults() > 0) select.setMax(getMaxResults());
        return select;
    }

    public void appendSelect(final String fromName, final OASelect select) {
        final String prefix = fromName + ".";
        String sql = "";
        Object[] args = new Object[0];
        if (created != null) {
            if (sql.length() > 0) sql += " AND ";
            sql += WebPage.P_Created + " = ?";
            args = OAArray.add(Object.class, args, this.created);
        }
        if (!OAString.isEmpty(this.url)) {
            if (sql.length() > 0) sql += " AND ";
            String value = this.url.replace("*", "%");
            if (!value.endsWith("%")) value += "%";
            if (value.indexOf("%") >= 0) {
                sql += WebPage.P_URL + " LIKE ?";
            }
            else {
                sql += WebPage.P_URL + " = ?";
            }
            args = OAArray.add(Object.class, args, value);
        }
        if (lastChanged != null) {
            if (sql.length() > 0) sql += " AND ";
            sql += WebPage.P_LastChanged + " = ?";
            args = OAArray.add(Object.class, args, this.lastChanged);
        }
        if (!OAString.isEmpty(this.title)) {
            if (sql.length() > 0) sql += " AND ";
            String value = this.title.replace("*", "%");
            if (!value.endsWith("%")) value += "%";
            if (value.indexOf("%") >= 0) {
                sql += WebPage.P_Title + " LIKE ?";
            }
            else {
                sql += WebPage.P_Title + " = ?";
            }
            args = OAArray.add(Object.class, args, value);
        }
        select.add(sql, args);
    }

    private OAFilter<WebPage> filterDataSourceFilter;
    public OAFilter<WebPage> getDataSourceFilter() {
        if (filterDataSourceFilter != null) return filterDataSourceFilter;
        filterDataSourceFilter = new OAFilter<WebPage>() {
            @Override
            public boolean isUsed(WebPage webPage) {
                return WebPageSearch.this.isUsedForDataSourceFilter(webPage);
            }
        };
        return filterDataSourceFilter;
    }
    
    private OAFilter<WebPage> filterCustomFilter;
    public OAFilter<WebPage> getCustomFilter() {
        if (filterCustomFilter != null) return filterCustomFilter;
        filterCustomFilter = new OAFilter<WebPage>() {
            @Override
            public boolean isUsed(WebPage webPage) {
                boolean b = WebPageSearch.this.isUsedForCustomFilter(webPage);
                if (b && filterExtraWhere != null) b = filterExtraWhere.isUsed(webPage);
                return b;
            }
        };
        return filterCustomFilter;
    }
    
    public boolean isUsedForDataSourceFilter(WebPage searchWebPage) {
        if (created != null) {
            if (!OACompare.isEqual(searchWebPage.getCreated(), created)) return false;
        }
        if (url != null) {
            String s = getURL();
            if (s != null && s.indexOf('*') < 0 && s.indexOf('%') < 0) s += '*';
            if (!OACompare.isLike(searchWebPage.getURL(), s)) return false;
        }
        if (lastChanged != null) {
            if (!OACompare.isEqual(searchWebPage.getLastChanged(), lastChanged)) return false;
        }
        if (title != null) {
            String s = getTitle();
            if (s != null && s.indexOf('*') < 0 && s.indexOf('%') < 0) s += '*';
            if (!OACompare.isLike(searchWebPage.getTitle(), s)) return false;
        }
        return true;
    }
    public boolean isUsedForCustomFilter(WebPage searchWebPage) {
        return true;
    }
}
