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
public class StoreSearch extends OAObject {
    private static final long serialVersionUID = 1L;

    private static Logger LOG = Logger.getLogger(StoreSearch.class.getName());

    public static final String P_StoreNumber = "StoreNumber";
    public static final String P_StoreNumber2 = "StoreNumber2";
    public static final String P_AddressState = "AddressState";
    public static final String P_AddressCity = "AddressCity";
    public static final String P_CustomQuery = "CustomQuery";
    public static final String P_MaxResults = "MaxResults";

    protected int storeNumber;
    protected int storeNumber2;
    protected String addressState;
    protected String addressCity;
    protected String customQuery;
    protected int maxResults;

    @OAProperty(lowerName = "storeNumber", displayName = "Store Number", displayLength = 6, uiColumnLength = 12)
    public int getStoreNumber() {
        return storeNumber;
    }
    public void setStoreNumber(int newValue) {
        int old = storeNumber;
        fireBeforePropertyChange(P_StoreNumber, old, newValue);
        this.storeNumber = newValue;
        firePropertyChange(P_StoreNumber, old, this.storeNumber);
        if (isLoading()) return;
        if (storeNumber > storeNumber2) setStoreNumber2(this.storeNumber);
    } 
    public int getStoreNumber2() {
        return storeNumber2;
    }
    public void setStoreNumber2(int newValue) {
        int old = storeNumber2;
        fireBeforePropertyChange(P_StoreNumber2, old, newValue);
        this.storeNumber2 = newValue;
        firePropertyChange(P_StoreNumber2, old, this.storeNumber2);
        if (isLoading()) return;
        if (storeNumber > storeNumber2) setStoreNumber(this.storeNumber2);
    }
    @OAProperty(lowerName = "state", maxLength = 30, displayLength = 18, uiColumnLength = 8)
    public String getAddressState() {
        return addressState;
    }
    public void setAddressState(String newValue) {
        String old = addressState;
        fireBeforePropertyChange(P_AddressState, old, newValue);
        this.addressState = newValue;
        firePropertyChange(P_AddressState, old, this.addressState);
    }
      
    @OAProperty(lowerName = "city", maxLength = 50, displayLength = 18)
    public String getAddressCity() {
        return addressCity;
    }
    public void setAddressCity(String newValue) {
        String old = addressCity;
        fireBeforePropertyChange(P_AddressCity, old, newValue);
        this.addressCity = newValue;
        firePropertyChange(P_AddressCity, old, this.addressCity);
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
        setStoreNumber(0);
        setNull(P_StoreNumber);
        setStoreNumber2(0);
        setNull(P_StoreNumber2);
        setAddressState(null);
        setAddressCity(null);
        setCustomQuery(null);
    }

    public boolean isDataEntered() {
        if (!isNull(P_StoreNumber)) return true;
        if (getAddressState() != null) return true;
        if (getAddressCity() != null) return true;
        if (getCustomQuery() != null) return true;
        return false;
    }

    protected String extraWhere;
    protected Object[] extraWhereParams;
    protected OAFilter<Store> filterExtraWhere;

    public void setExtraWhere(String s, Object ... args) {
        this.extraWhere = s;
        this.extraWhereParams = args;
        if (OAString.isNotEmpty(s) && getExtraWhereFilter() == null) {
            OAFilter<Store> f = new OAQueryFilter<Store>(Store.class, s, args);
            setExtraWhereFilter(f);
        }
    }
    public void setExtraWhereFilter(OAFilter<Store> filter) {
        this.filterExtraWhere = filter;
    }
    public OAFilter<Store> getExtraWhereFilter() {
        return this.filterExtraWhere;
    }

    public OASelect<Store> getSelect() {
        final String prefix = "";
        String sql = "";
        String sortOrder = null;
        Object[] args = new Object[0];
        if (!isNull(P_StoreNumber)) {
            if (sql.length() > 0) sql += " AND ";
            if (!isNull(P_StoreNumber2) && storeNumber != storeNumber2) {
                sql += Store.P_StoreNumber + " >= ?";
                args = OAArray.add(Object.class, args, getStoreNumber());
                sql += " AND " + Store.P_StoreNumber + " <= ?";
                args = OAArray.add(Object.class, args, getStoreNumber2());
            }
            else {
                sql += Store.P_StoreNumber + " = ?";
                args = OAArray.add(Object.class, args, getStoreNumber());
            }
        }
        if (OAString.isNotEmpty(this.addressState)) {
            if (sql.length() > 0) sql += " AND ";
            String val = OAString.convertToLikeSearch(addressState);
            if (val.indexOf("%") >= 0) {
                sql += StorePP.address().state() + " LIKE ?";
            }
            else {
                sql += StorePP.address().state() + " = ?";
            }
            args = OAArray.add(Object.class, args, val);
        }
        if (OAString.isNotEmpty(this.addressCity)) {
            if (sql.length() > 0) sql += " AND ";
            String val = OAString.convertToLikeSearch(addressCity);
            if (val.indexOf("%") >= 0) {
                sql += StorePP.address().city() + " LIKE ?";
            }
            else {
                sql += StorePP.address().city() + " = ?";
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

        OASelect<Store> select = new OASelect<Store>(Store.class, sql, args, sortOrder);
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
        if (!isNull(P_StoreNumber)) {
            if (sql.length() > 0) sql += " AND ";
            if (!isNull(P_StoreNumber2) && storeNumber != storeNumber2) {
                sql += prefix + Store.P_StoreNumber + " >= ?";
                args = OAArray.add(Object.class, args, getStoreNumber());
                sql += " AND " + prefix + Store.P_StoreNumber + " <= ?";
                args = OAArray.add(Object.class, args, getStoreNumber2());
            }
            else {
                sql += prefix + Store.P_StoreNumber + " = ?";
                args = OAArray.add(Object.class, args, getStoreNumber());
            }
        }
        if (OAString.isNotEmpty(this.addressState)) {
            if (sql.length() > 0) sql += " AND ";
            String val = OAString.convertToLikeSearch(addressState);
            if (val.indexOf("%") >= 0) {
                sql += prefix + StorePP.address().state() + " LIKE ?";
            }
            else {
                sql += prefix + StorePP.address().state() + " = ?";
            }
            args = OAArray.add(Object.class, args, val);
        }
        if (OAString.isNotEmpty(this.addressCity)) {
            if (sql.length() > 0) sql += " AND ";
            String val = OAString.convertToLikeSearch(addressCity);
            if (val.indexOf("%") >= 0) {
                sql += prefix + StorePP.address().city() + " LIKE ?";
            }
            else {
                sql += prefix + StorePP.address().city() + " = ?";
            }
            args = OAArray.add(Object.class, args, val);
        }
    if (OAString.isNotEmpty(this.customQuery)) {
        if (sql.length() > 0) sql += " AND ";
        sql += "(" + getCustomQuery() + ")";
    }
        select.add(sql, args);
    }

    private OAFilter<Store> filterDataSourceFilter;
    public OAFilter<Store> getDataSourceFilter() {
        if (filterDataSourceFilter != null) return filterDataSourceFilter;
        filterDataSourceFilter = new OAFilter<Store>() {
            @Override
            public boolean isUsed(Store store) {
                return StoreSearch.this.isUsedForDataSourceFilter(store);
            }
        };
        return filterDataSourceFilter;
    }
    
    private OAFilter<Store> filterCustomFilter;
    public OAFilter<Store> getCustomFilter() {
        if (filterCustomFilter != null) return filterCustomFilter;
        filterCustomFilter = new OAFilter<Store>() {
            @Override
            public boolean isUsed(Store store) {
                boolean b = StoreSearch.this.isUsedForCustomFilter(store);
                return b;
            }
        };
        return filterCustomFilter;
    }
    
    public boolean isUsedForDataSourceFilter(Store searchStore) {
        return true;
    }
    public boolean isUsedForCustomFilter(Store searchStore) {
        return true;
    }
}
