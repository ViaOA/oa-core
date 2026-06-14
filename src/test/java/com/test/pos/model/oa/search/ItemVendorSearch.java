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
public class ItemVendorSearch extends OAObject {
    private static final long serialVersionUID = 1L;

    private static Logger LOG = Logger.getLogger(ItemVendorSearch.class.getName());

    public static final String P_Name = "Name";
    public static final String P_CustomQuery = "CustomQuery";
    public static final String P_MaxResults = "MaxResults";

    protected String name;
    protected String customQuery;
    protected int maxResults;

    @OAProperty(lowerName = "name", maxLength = 50, displayLength = 18)
    public String getName() {
        return name;
    }
    public void setName(String newValue) {
        String old = name;
        fireBeforePropertyChange(P_Name, old, newValue);
        this.name = newValue;
        firePropertyChange(P_Name, old, this.name);
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
        setName(null);
        setCustomQuery(null);
    }

    public boolean isDataEntered() {
        if (getName() != null) return true;
        if (getCustomQuery() != null) return true;
        return false;
    }

    protected String extraWhere;
    protected Object[] extraWhereParams;
    protected OAFilter<ItemVendor> filterExtraWhere;

    public void setExtraWhere(String s, Object ... args) {
        this.extraWhere = s;
        this.extraWhereParams = args;
        if (OAString.isNotEmpty(s) && getExtraWhereFilter() == null) {
            OAFilter<ItemVendor> f = new OAQueryFilter<ItemVendor>(ItemVendor.class, s, args);
            setExtraWhereFilter(f);
        }
    }
    public void setExtraWhereFilter(OAFilter<ItemVendor> filter) {
        this.filterExtraWhere = filter;
    }
    public OAFilter<ItemVendor> getExtraWhereFilter() {
        return this.filterExtraWhere;
    }

    public OASelect<ItemVendor> getSelect() {
        final String prefix = "";
        String sql = "";
        String sortOrder = null;
        Object[] args = new Object[0];
        if (OAString.isNotEmpty(this.name)) {
            if (sql.length() > 0) sql += " AND ";
            String val = OAString.convertToLikeSearch(name);
            if (val.indexOf("%") >= 0) {
                sql += ItemVendor.P_Name + " LIKE ?";
            }
            else {
                sql += ItemVendor.P_Name + " = ?";
            }
            args = OAArray.add(Object.class, args, val);
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

        OASelect<ItemVendor> select = new OASelect<ItemVendor>(ItemVendor.class, sql, args, sortOrder);
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
        if (OAString.isNotEmpty(this.name)) {
            if (sql.length() > 0) sql += " AND ";
            String val = OAString.convertToLikeSearch(name);
            if (val.indexOf("%") >= 0) {
                sql += prefix + ItemVendor.P_Name + " LIKE ?";
            }
            else {
                sql += prefix + ItemVendor.P_Name + " = ?";
            }
            args = OAArray.add(Object.class, args, val);
        }
    if (OAString.isNotEmpty(this.customQuery)) {
        if (sql.length() > 0) sql += " AND ";
        sql += "(" + getCustomQuery() + ")";
    }
        select.add(sql, args);
    }

    private OAFilter<ItemVendor> filterDataSourceFilter;
    public OAFilter<ItemVendor> getDataSourceFilter() {
        if (filterDataSourceFilter != null) return filterDataSourceFilter;
        filterDataSourceFilter = new OAFilter<ItemVendor>() {
            @Override
            public boolean isUsed(ItemVendor itemVendor) {
                return ItemVendorSearch.this.isUsedForDataSourceFilter(itemVendor);
            }
        };
        return filterDataSourceFilter;
    }
    
    private OAFilter<ItemVendor> filterCustomFilter;
    public OAFilter<ItemVendor> getCustomFilter() {
        if (filterCustomFilter != null) return filterCustomFilter;
        filterCustomFilter = new OAFilter<ItemVendor>() {
            @Override
            public boolean isUsed(ItemVendor itemVendor) {
                boolean b = ItemVendorSearch.this.isUsedForCustomFilter(itemVendor);
                return b;
            }
        };
        return filterCustomFilter;
    }
    
    public boolean isUsedForDataSourceFilter(ItemVendor searchItemVendor) {
        return true;
    }
    public boolean isUsedForCustomFilter(ItemVendor searchItemVendor) {
        return true;
    }
}
