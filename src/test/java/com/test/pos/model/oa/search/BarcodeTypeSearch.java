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
public class BarcodeTypeSearch extends OAObject {
    private static final long serialVersionUID = 1L;

    private static Logger LOG = Logger.getLogger(BarcodeTypeSearch.class.getName());

    public static final String P_Name = "Name";
    public static final String P_BarcodeType = "BarcodeType";
    public static final String P_CustomQuery = "CustomQuery";
    public static final String P_MaxResults = "MaxResults";

    protected String name;
    protected int barcodeType;
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
      
    @OAProperty(lowerName = "type", displayLength = 6)
    public int getBarcodeType() {
        return barcodeType;
    }
    public void setBarcodeType(int newValue) {
        int old = barcodeType;
        fireBeforePropertyChange(P_BarcodeType, old, newValue);
        this.barcodeType = newValue;
        firePropertyChange(P_BarcodeType, old, this.barcodeType);
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
        setBarcodeType(0);
        setNull(P_BarcodeType);
        setCustomQuery(null);
    }

    public boolean isDataEntered() {
        if (getName() != null) return true;
        if (!isNull(P_BarcodeType)) return true;
        if (getCustomQuery() != null) return true;
        return false;
    }

    protected String extraWhere;
    protected Object[] extraWhereParams;
    protected OAFilter<BarcodeType> filterExtraWhere;

    public void setExtraWhere(String s, Object ... args) {
        this.extraWhere = s;
        this.extraWhereParams = args;
        if (OAString.isNotEmpty(s) && getExtraWhereFilter() == null) {
            OAFilter<BarcodeType> f = new OAQueryFilter<BarcodeType>(BarcodeType.class, s, args);
            setExtraWhereFilter(f);
        }
    }
    public void setExtraWhereFilter(OAFilter<BarcodeType> filter) {
        this.filterExtraWhere = filter;
    }
    public OAFilter<BarcodeType> getExtraWhereFilter() {
        return this.filterExtraWhere;
    }

    public OASelect<BarcodeType> getSelect() {
        final String prefix = "";
        String sql = "";
        String sortOrder = null;
        Object[] args = new Object[0];
        if (OAString.isNotEmpty(this.name)) {
            if (sql.length() > 0) sql += " AND ";
            String val = OAString.convertToLikeSearch(name);
            if (val.indexOf("%") >= 0) {
                sql += BarcodeType.P_Name + " LIKE ?";
            }
            else {
                sql += BarcodeType.P_Name + " = ?";
            }
            args = OAArray.add(Object.class, args, val);
        }
        if (!isNull(P_BarcodeType)) {
            if (sql.length() > 0) sql += " AND ";
            sql += BarcodeType.P_Type + " = ?";
            args = OAArray.add(Object.class, args, this.barcodeType);
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

        OASelect<BarcodeType> select = new OASelect<BarcodeType>(BarcodeType.class, sql, args, sortOrder);
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
                sql += prefix + BarcodeType.P_Name + " LIKE ?";
            }
            else {
                sql += prefix + BarcodeType.P_Name + " = ?";
            }
            args = OAArray.add(Object.class, args, val);
        }
        if (!isNull(P_BarcodeType)) {
            if (sql.length() > 0) sql += " AND ";
            sql += prefix + BarcodeType.P_Type + " = ?";
            args = OAArray.add(Object.class, args, this.barcodeType);
        }
    if (OAString.isNotEmpty(this.customQuery)) {
        if (sql.length() > 0) sql += " AND ";
        sql += "(" + getCustomQuery() + ")";
    }
        select.add(sql, args);
    }

    private OAFilter<BarcodeType> filterDataSourceFilter;
    public OAFilter<BarcodeType> getDataSourceFilter() {
        if (filterDataSourceFilter != null) return filterDataSourceFilter;
        filterDataSourceFilter = new OAFilter<BarcodeType>() {
            @Override
            public boolean isUsed(BarcodeType barcodeType) {
                return BarcodeTypeSearch.this.isUsedForDataSourceFilter(barcodeType);
            }
        };
        return filterDataSourceFilter;
    }
    
    private OAFilter<BarcodeType> filterCustomFilter;
    public OAFilter<BarcodeType> getCustomFilter() {
        if (filterCustomFilter != null) return filterCustomFilter;
        filterCustomFilter = new OAFilter<BarcodeType>() {
            @Override
            public boolean isUsed(BarcodeType barcodeType) {
                boolean b = BarcodeTypeSearch.this.isUsedForCustomFilter(barcodeType);
                return b;
            }
        };
        return filterCustomFilter;
    }
    
    public boolean isUsedForDataSourceFilter(BarcodeType searchBarcodeType) {
        return true;
    }
    public boolean isUsedForCustomFilter(BarcodeType searchBarcodeType) {
        return true;
    }
}
