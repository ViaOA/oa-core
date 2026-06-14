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
import com.viaoa.datetime.OADateTime;
import com.viaoa.datasource.*;
import com.viaoa.filter.*;
import com.viaoa.find.*;

@OAClass(useDataSource=false, localOnly=true)
public class DemoNodeSearch extends OAObject {
    private static final long serialVersionUID = 1L;

    private static Logger LOG = Logger.getLogger(DemoNodeSearch.class.getName());

    public static final String P_Type = "Type";
    public static final String P_Name = "Name";
    public static final String P_Started = "Started";
    public static final String P_Started2 = "Started2";
    public static final String P_CustomQuery = "CustomQuery";
    public static final String P_MaxResults = "MaxResults";

    protected int type;
    protected String name;
    protected OADateTime started;
    protected OADateTime started2;
    protected String customQuery;
    protected int maxResults;

    @OAProperty(lowerName = "type", displayLength = 6)
    public int getType() {
        return type;
    }
    public void setType(int newValue) {
        int old = type;
        fireBeforePropertyChange(P_Type, old, newValue);
        this.type = newValue;
        firePropertyChange(P_Type, old, this.type);
    }
      
    @OAProperty(lowerName = "name", displayLength = 20)
    public String getName() {
        return name;
    }
    public void setName(String newValue) {
        String old = name;
        fireBeforePropertyChange(P_Name, old, newValue);
        this.name = newValue;
        firePropertyChange(P_Name, old, this.name);
    }
      
    @OAProperty(lowerName = "started", displayLength = 15, ignoreTimeZone = true)
    public OADateTime getStarted() {
        return started;
    }
    public void setStarted(OADateTime newValue) {
        OADateTime old = started;
        fireBeforePropertyChange(P_Started, old, newValue);
        this.started = newValue;
        firePropertyChange(P_Started, old, this.started);
        if (isLoading()) return;
        if (started != null) {
            if (started2 == null) setStarted2(this.started.plusDays(1));
            else if (started.compareTo(started2) > 0) setStarted2(this.started.plusDays(1));
        }
    } 
    public OADateTime getStarted2() {
        return started2;
    }
    public void setStarted2(OADateTime newValue) {
        OADateTime old = started2;
        fireBeforePropertyChange(P_Started2, old, newValue);
        this.started2 = newValue;
        firePropertyChange(P_Started2, old, this.started2);
        if (started != null && started2 != null) {
            if (started.compareTo(started2) > 0) setStarted(this.started2);
        }
    }

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
        setType(0);
        setNull(P_Type);
        setName(null);
        setStarted(null);
        setStarted2(null);
        setCustomQuery(null);
    }

    public boolean isDataEntered() {
        if (!isNull(P_Type)) return true;
        if (getName() != null) return true;
        if (getStarted() != null) return true;
        if (getCustomQuery() != null) return true;
        return false;
    }

    protected String extraWhere;
    protected Object[] extraWhereParams;
    protected OAFilter<DemoNode> filterExtraWhere;

    public void setExtraWhere(String s, Object ... args) {
        this.extraWhere = s;
        this.extraWhereParams = args;
        if (OAString.isNotEmpty(s) && getExtraWhereFilter() == null) {
            OAFilter<DemoNode> f = new OAQueryFilter<DemoNode>(DemoNode.class, s, args);
            setExtraWhereFilter(f);
        }
    }
    public void setExtraWhereFilter(OAFilter<DemoNode> filter) {
        this.filterExtraWhere = filter;
    }
    public OAFilter<DemoNode> getExtraWhereFilter() {
        return this.filterExtraWhere;
    }

    public OASelect<DemoNode> getSelect() {
        final String prefix = "";
        String sql = "";
        String sortOrder = null;
        Object[] args = new Object[0];
        if (!isNull(P_Type)) {
            if (sql.length() > 0) sql += " AND ";
            sql += DemoNode.P_Type + " = ?";
            args = OAArray.add(Object.class, args, this.type);
        }
        if (OAString.isNotEmpty(this.name)) {
            if (sql.length() > 0) sql += " AND ";
            String val = OAString.convertToLikeSearch(name);
            if (val.indexOf("%") >= 0) {
                sql += DemoNode.P_Name + " LIKE ?";
            }
            else {
                sql += DemoNode.P_Name + " = ?";
            }
            args = OAArray.add(Object.class, args, val);
        }
        if (started != null) {
            if (sql.length() > 0) sql += " AND ";
            if (started2 != null && !started.equals(started2)) {
                sql += DemoNode.P_Started + " >= ?";
                args = OAArray.add(Object.class, args, this.started);
                sql += " AND " + DemoNode.P_Started + " <= ?";
                args = OAArray.add(Object.class, args, this.started2);
            }
            else {
                sql += DemoNode.P_Started + " = ?";
                args = OAArray.add(Object.class, args, this.started);
            }
        }
    if (OAString.isNotEmpty(this.customQuery)) {
        if (sql.length() > 0) sql += " AND ";
        sql += "(" + getCustomQuery() + ")";
    }

        if (OAString.isNotEmpty(extraWhere)) {
            if (sql.length() > 0) sql = "(" + sql + ") AND ";
            sql += extraWhere;
            args = OAArray.add(Object.class, args, extraWhereParams);
        }

        OASelect<DemoNode> select = new OASelect<DemoNode>(DemoNode.class, sql, args, sortOrder);
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
        if (!isNull(P_Type)) {
            if (sql.length() > 0) sql += " AND ";
            sql += prefix + DemoNode.P_Type + " = ?";
            args = OAArray.add(Object.class, args, this.type);
        }
        if (OAString.isNotEmpty(this.name)) {
            if (sql.length() > 0) sql += " AND ";
            String val = OAString.convertToLikeSearch(name);
            if (val.indexOf("%") >= 0) {
                sql += prefix + DemoNode.P_Name + " LIKE ?";
            }
            else {
                sql += prefix + DemoNode.P_Name + " = ?";
            }
            args = OAArray.add(Object.class, args, val);
        }
        if (started != null) {
            if (sql.length() > 0) sql += " AND ";
            if (started2 != null && !started.equals(started2)) {
                sql += prefix + DemoNode.P_Started + " >= ?";
                args = OAArray.add(Object.class, args, this.started);
                sql += " AND " + prefix + DemoNode.P_Started + " <= ?";
                args = OAArray.add(Object.class, args, this.started2);
            }
            else {
                sql += prefix + DemoNode.P_Started + " = ?";
                args = OAArray.add(Object.class, args, this.started);
            }
        }
    if (OAString.isNotEmpty(this.customQuery)) {
        if (sql.length() > 0) sql += " AND ";
        sql += "(" + getCustomQuery() + ")";
    }
        select.add(sql, args);
    }

    private OAFilter<DemoNode> filterDataSourceFilter;
    public OAFilter<DemoNode> getDataSourceFilter() {
        if (filterDataSourceFilter != null) return filterDataSourceFilter;
        filterDataSourceFilter = new OAFilter<DemoNode>() {
            @Override
            public boolean isUsed(DemoNode demoNode) {
                return DemoNodeSearch.this.isUsedForDataSourceFilter(demoNode);
            }
        };
        return filterDataSourceFilter;
    }
    
    private OAFilter<DemoNode> filterCustomFilter;
    public OAFilter<DemoNode> getCustomFilter() {
        if (filterCustomFilter != null) return filterCustomFilter;
        filterCustomFilter = new OAFilter<DemoNode>() {
            @Override
            public boolean isUsed(DemoNode demoNode) {
                boolean b = DemoNodeSearch.this.isUsedForCustomFilter(demoNode);
                return b;
            }
        };
        return filterCustomFilter;
    }
    
    public boolean isUsedForDataSourceFilter(DemoNode searchDemoNode) {
        return true;
    }
    public boolean isUsedForCustomFilter(DemoNode searchDemoNode) {
        return true;
    }
}
