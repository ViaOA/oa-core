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
public class CustomerSearch extends OAObject {
    private static final long serialVersionUID = 1L;

    private static Logger LOG = Logger.getLogger(CustomerSearch.class.getName());

    public static final String P_Name = "Name";
    public static final String P_Type = "Type";
    public static final String P_Type2 = "Type2";
    public static final String P_AddressesCity = "AddressesCity";
    public static final String P_AddressesState = "AddressesState";
    public static final String P_CustomQuery = "CustomQuery";
    public static final String P_MaxResults = "MaxResults";

    protected String name;
    protected int type;
    protected int type2;
    protected String addressesCity;
    protected String addressesState;
    protected String customQuery;
    protected int maxResults;

    @OAProperty(lowerName = "name", maxLength = 75, displayLength = 22, uiColumnLength = 20)
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
    public int getType() {
        return type;
    }
    public void setType(int newValue) {
        int old = type;
        fireBeforePropertyChange(P_Type, old, newValue);
        this.type = newValue;
        firePropertyChange(P_Type, old, this.type);
        if (isLoading()) return;
        if (type > type2) setType2(this.type);
    } 
    public int getType2() {
        return type2;
    }
    public void setType2(int newValue) {
        int old = type2;
        fireBeforePropertyChange(P_Type2, old, newValue);
        this.type2 = newValue;
        firePropertyChange(P_Type2, old, this.type2);
        if (isLoading()) return;
        if (type > type2) setType(this.type2);
    }
    @OAProperty(lowerName = "city", maxLength = 50, displayLength = 18)
    public String getAddressesCity() {
        return addressesCity;
    }
    public void setAddressesCity(String newValue) {
        String old = addressesCity;
        fireBeforePropertyChange(P_AddressesCity, old, newValue);
        this.addressesCity = newValue;
        firePropertyChange(P_AddressesCity, old, this.addressesCity);
    }
      
    @OAProperty(lowerName = "state", maxLength = 30, displayLength = 18, uiColumnLength = 8)
    public String getAddressesState() {
        return addressesState;
    }
    public void setAddressesState(String newValue) {
        String old = addressesState;
        fireBeforePropertyChange(P_AddressesState, old, newValue);
        this.addressesState = newValue;
        firePropertyChange(P_AddressesState, old, this.addressesState);
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
        setType(0);
        setNull(P_Type);
        setType2(0);
        setNull(P_Type2);
        setAddressesCity(null);
        setAddressesState(null);
        setCustomQuery(null);
    }

    public boolean isDataEntered() {
        if (getName() != null) return true;
        if (!isNull(P_Type)) return true;
        if (getAddressesCity() != null) return true;
        if (getAddressesState() != null) return true;
        if (getCustomQuery() != null) return true;
        return false;
    }

    protected String extraWhere;
    protected Object[] extraWhereParams;
    protected OAFilter<Customer> filterExtraWhere;

    public void setExtraWhere(String s, Object ... args) {
        this.extraWhere = s;
        this.extraWhereParams = args;
        if (OAString.isNotEmpty(s) && getExtraWhereFilter() == null) {
            OAFilter<Customer> f = new OAQueryFilter<Customer>(Customer.class, s, args);
            setExtraWhereFilter(f);
        }
    }
    public void setExtraWhereFilter(OAFilter<Customer> filter) {
        this.filterExtraWhere = filter;
    }
    public OAFilter<Customer> getExtraWhereFilter() {
        return this.filterExtraWhere;
    }

    public OASelect<Customer> getSelect() {
        final String prefix = "";
        String sql = "";
        String sortOrder = null;
        Object[] args = new Object[0];
        if (OAString.isNotEmpty(this.name)) {
            if (sql.length() > 0) sql += " AND ";
            String val = OAString.convertToLikeSearch(name);
            if (val.indexOf("%") >= 0) {
                sql += Customer.P_Name + " LIKE ?";
            }
            else {
                sql += Customer.P_Name + " = ?";
            }
            args = OAArray.add(Object.class, args, val);
        }
        if (!isNull(P_Type)) {
            if (sql.length() > 0) sql += " AND ";
            if (!isNull(P_Type2) && type != type2) {
                sql += Customer.P_Type + " >= ?";
                args = OAArray.add(Object.class, args, getType());
                sql += " AND " + Customer.P_Type + " <= ?";
                args = OAArray.add(Object.class, args, getType2());
            }
            else {
                sql += Customer.P_Type + " = ?";
                args = OAArray.add(Object.class, args, getType());
            }
        }
        if (OAString.isNotEmpty(this.addressesCity)) {
            if (sql.length() > 0) sql += " AND ";
            String val = OAString.convertToLikeSearch(addressesCity);
            if (val.indexOf("%") >= 0) {
                sql += CustomerPP.addresses().city() + " LIKE ?";
            }
            else {
                sql += CustomerPP.addresses().city() + " = ?";
            }
            args = OAArray.add(Object.class, args, val);
        }
        if (OAString.isNotEmpty(this.addressesState)) {
            if (sql.length() > 0) sql += " AND ";
            String val = OAString.convertToLikeSearch(addressesState);
            if (val.indexOf("%") >= 0) {
                sql += CustomerPP.addresses().state() + " LIKE ?";
            }
            else {
                sql += CustomerPP.addresses().state() + " = ?";
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

        OASelect<Customer> select = new OASelect<Customer>(Customer.class, sql, args, sortOrder);
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
                sql += prefix + Customer.P_Name + " LIKE ?";
            }
            else {
                sql += prefix + Customer.P_Name + " = ?";
            }
            args = OAArray.add(Object.class, args, val);
        }
        if (!isNull(P_Type)) {
            if (sql.length() > 0) sql += " AND ";
            if (!isNull(P_Type2) && type != type2) {
                sql += prefix + Customer.P_Type + " >= ?";
                args = OAArray.add(Object.class, args, getType());
                sql += " AND " + prefix + Customer.P_Type + " <= ?";
                args = OAArray.add(Object.class, args, getType2());
            }
            else {
                sql += prefix + Customer.P_Type + " = ?";
                args = OAArray.add(Object.class, args, getType());
            }
        }
        if (OAString.isNotEmpty(this.addressesCity)) {
            if (sql.length() > 0) sql += " AND ";
            String val = OAString.convertToLikeSearch(addressesCity);
            if (val.indexOf("%") >= 0) {
                sql += prefix + CustomerPP.addresses().city() + " LIKE ?";
            }
            else {
                sql += prefix + CustomerPP.addresses().city() + " = ?";
            }
            args = OAArray.add(Object.class, args, val);
        }
        if (OAString.isNotEmpty(this.addressesState)) {
            if (sql.length() > 0) sql += " AND ";
            String val = OAString.convertToLikeSearch(addressesState);
            if (val.indexOf("%") >= 0) {
                sql += prefix + CustomerPP.addresses().state() + " LIKE ?";
            }
            else {
                sql += prefix + CustomerPP.addresses().state() + " = ?";
            }
            args = OAArray.add(Object.class, args, val);
        }
    if (OAString.isNotEmpty(this.customQuery)) {
        if (sql.length() > 0) sql += " AND ";
        sql += "(" + getCustomQuery() + ")";
    }
        select.add(sql, args);
    }

    private OAFilter<Customer> filterDataSourceFilter;
    public OAFilter<Customer> getDataSourceFilter() {
        if (filterDataSourceFilter != null) return filterDataSourceFilter;
        filterDataSourceFilter = new OAFilter<Customer>() {
            @Override
            public boolean isUsed(Customer customer) {
                return CustomerSearch.this.isUsedForDataSourceFilter(customer);
            }
        };
        return filterDataSourceFilter;
    }
    
    private OAFilter<Customer> filterCustomFilter;
    public OAFilter<Customer> getCustomFilter() {
        if (filterCustomFilter != null) return filterCustomFilter;
        filterCustomFilter = new OAFilter<Customer>() {
            @Override
            public boolean isUsed(Customer customer) {
                boolean b = CustomerSearch.this.isUsedForCustomFilter(customer);
                return b;
            }
        };
        return filterCustomFilter;
    }
    
    public boolean isUsedForDataSourceFilter(Customer searchCustomer) {
        return true;
    }
    public boolean isUsedForCustomFilter(Customer searchCustomer) {
        return true;
    }
}
