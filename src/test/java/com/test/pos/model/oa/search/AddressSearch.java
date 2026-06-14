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
public class AddressSearch extends OAObject {
    private static final long serialVersionUID = 1L;

    private static Logger LOG = Logger.getLogger(AddressSearch.class.getName());

    public static final String P_Name = "Name";
    public static final String P_Address1 = "Address1";
    public static final String P_Address2 = "Address2";
    public static final String P_City = "City";
    public static final String P_State = "State";
    public static final String P_Zip = "Zip";
    public static final String P_Type = "Type";
    public static final String P_Type2 = "Type2";
    public static final String P_CustomQuery = "CustomQuery";
    public static final String P_MaxResults = "MaxResults";

    protected String name;
    protected String address1;
    protected String address2;
    protected String city;
    protected String state;
    protected String zip;
    protected int type;
    protected int type2;
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
      
    @OAProperty(lowerName = "address1", maxLength = 50, displayLength = 18)
    public String getAddress1() {
        return address1;
    }
    public void setAddress1(String newValue) {
        String old = address1;
        fireBeforePropertyChange(P_Address1, old, newValue);
        this.address1 = newValue;
        firePropertyChange(P_Address1, old, this.address1);
    }
      
    @OAProperty(lowerName = "address2", maxLength = 50, displayLength = 18)
    public String getAddress2() {
        return address2;
    }
    public void setAddress2(String newValue) {
        String old = address2;
        fireBeforePropertyChange(P_Address2, old, newValue);
        this.address2 = newValue;
        firePropertyChange(P_Address2, old, this.address2);
    }
      
    @OAProperty(lowerName = "city", maxLength = 50, displayLength = 18)
    public String getCity() {
        return city;
    }
    public void setCity(String newValue) {
        String old = city;
        fireBeforePropertyChange(P_City, old, newValue);
        this.city = newValue;
        firePropertyChange(P_City, old, this.city);
    }
      
    @OAProperty(lowerName = "state", maxLength = 30, displayLength = 18, uiColumnLength = 8)
    public String getState() {
        return state;
    }
    public void setState(String newValue) {
        String old = state;
        fireBeforePropertyChange(P_State, old, newValue);
        this.state = newValue;
        firePropertyChange(P_State, old, this.state);
    }
      
    @OAProperty(lowerName = "zip", maxLength = 20, displayLength = 5)
    public String getZip() {
        return zip;
    }
    public void setZip(String newValue) {
        String old = zip;
        fireBeforePropertyChange(P_Zip, old, newValue);
        this.zip = newValue;
        firePropertyChange(P_Zip, old, this.zip);
    }
      
    @OAProperty(lowerName = "type", displayLength = 14, uiColumnLength = 6)
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
        setAddress1(null);
        setAddress2(null);
        setCity(null);
        setState(null);
        setZip(null);
        setType(0);
        setNull(P_Type);
        setType2(0);
        setNull(P_Type2);
        setCustomQuery(null);
    }

    public boolean isDataEntered() {
        if (getName() != null) return true;
        if (getAddress1() != null) return true;
        if (getAddress2() != null) return true;
        if (getCity() != null) return true;
        if (getState() != null) return true;
        if (getZip() != null) return true;
        if (!isNull(P_Type)) return true;
        if (getCustomQuery() != null) return true;
        return false;
    }

    protected String extraWhere;
    protected Object[] extraWhereParams;
    protected OAFilter<Address> filterExtraWhere;

    public void setExtraWhere(String s, Object ... args) {
        this.extraWhere = s;
        this.extraWhereParams = args;
        if (OAString.isNotEmpty(s) && getExtraWhereFilter() == null) {
            OAFilter<Address> f = new OAQueryFilter<Address>(Address.class, s, args);
            setExtraWhereFilter(f);
        }
    }
    public void setExtraWhereFilter(OAFilter<Address> filter) {
        this.filterExtraWhere = filter;
    }
    public OAFilter<Address> getExtraWhereFilter() {
        return this.filterExtraWhere;
    }

    public OASelect<Address> getSelect() {
        final String prefix = "";
        String sql = "";
        String sortOrder = null;
        Object[] args = new Object[0];
        if (OAString.isNotEmpty(this.name)) {
            if (sql.length() > 0) sql += " AND ";
            String val = OAString.convertToLikeSearch(name);
            if (val.indexOf("%") >= 0) {
                sql += Address.P_Name + " LIKE ?";
            }
            else {
                sql += Address.P_Name + " = ?";
            }
            args = OAArray.add(Object.class, args, val);
        }
        if (OAString.isNotEmpty(this.address1)) {
            if (sql.length() > 0) sql += " AND ";
            String val = OAString.convertToLikeSearch(address1);
            if (val.indexOf("%") >= 0) {
                sql += Address.P_Address1 + " LIKE ?";
            }
            else {
                sql += Address.P_Address1 + " = ?";
            }
            args = OAArray.add(Object.class, args, val);
        }
        if (OAString.isNotEmpty(this.address2)) {
            if (sql.length() > 0) sql += " AND ";
            String val = OAString.convertToLikeSearch(address2);
            if (val.indexOf("%") >= 0) {
                sql += Address.P_Address2 + " LIKE ?";
            }
            else {
                sql += Address.P_Address2 + " = ?";
            }
            args = OAArray.add(Object.class, args, val);
        }
        if (OAString.isNotEmpty(this.city)) {
            if (sql.length() > 0) sql += " AND ";
            String val = OAString.convertToLikeSearch(city);
            if (val.indexOf("%") >= 0) {
                sql += Address.P_City + " LIKE ?";
            }
            else {
                sql += Address.P_City + " = ?";
            }
            args = OAArray.add(Object.class, args, val);
        }
        if (OAString.isNotEmpty(this.state)) {
            if (sql.length() > 0) sql += " AND ";
            String val = OAString.convertToLikeSearch(state);
            if (val.indexOf("%") >= 0) {
                sql += Address.P_State + " LIKE ?";
            }
            else {
                sql += Address.P_State + " = ?";
            }
            args = OAArray.add(Object.class, args, val);
        }
        if (OAString.isNotEmpty(this.zip)) {
            if (sql.length() > 0) sql += " AND ";
            String val = OAString.convertToLikeSearch(zip);
            if (val.indexOf("%") >= 0) {
                sql += Address.P_Zip + " LIKE ?";
            }
            else {
                sql += Address.P_Zip + " = ?";
            }
            args = OAArray.add(Object.class, args, val);
        }
        if (!isNull(P_Type)) {
            if (sql.length() > 0) sql += " AND ";
            if (!isNull(P_Type2) && type != type2) {
                sql += Address.P_Type + " >= ?";
                args = OAArray.add(Object.class, args, getType());
                sql += " AND " + Address.P_Type + " <= ?";
                args = OAArray.add(Object.class, args, getType2());
            }
            else {
                sql += Address.P_Type + " = ?";
                args = OAArray.add(Object.class, args, getType());
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

        OASelect<Address> select = new OASelect<Address>(Address.class, sql, args, sortOrder);
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
                sql += prefix + Address.P_Name + " LIKE ?";
            }
            else {
                sql += prefix + Address.P_Name + " = ?";
            }
            args = OAArray.add(Object.class, args, val);
        }
        if (OAString.isNotEmpty(this.address1)) {
            if (sql.length() > 0) sql += " AND ";
            String val = OAString.convertToLikeSearch(address1);
            if (val.indexOf("%") >= 0) {
                sql += prefix + Address.P_Address1 + " LIKE ?";
            }
            else {
                sql += prefix + Address.P_Address1 + " = ?";
            }
            args = OAArray.add(Object.class, args, val);
        }
        if (OAString.isNotEmpty(this.address2)) {
            if (sql.length() > 0) sql += " AND ";
            String val = OAString.convertToLikeSearch(address2);
            if (val.indexOf("%") >= 0) {
                sql += prefix + Address.P_Address2 + " LIKE ?";
            }
            else {
                sql += prefix + Address.P_Address2 + " = ?";
            }
            args = OAArray.add(Object.class, args, val);
        }
        if (OAString.isNotEmpty(this.city)) {
            if (sql.length() > 0) sql += " AND ";
            String val = OAString.convertToLikeSearch(city);
            if (val.indexOf("%") >= 0) {
                sql += prefix + Address.P_City + " LIKE ?";
            }
            else {
                sql += prefix + Address.P_City + " = ?";
            }
            args = OAArray.add(Object.class, args, val);
        }
        if (OAString.isNotEmpty(this.state)) {
            if (sql.length() > 0) sql += " AND ";
            String val = OAString.convertToLikeSearch(state);
            if (val.indexOf("%") >= 0) {
                sql += prefix + Address.P_State + " LIKE ?";
            }
            else {
                sql += prefix + Address.P_State + " = ?";
            }
            args = OAArray.add(Object.class, args, val);
        }
        if (OAString.isNotEmpty(this.zip)) {
            if (sql.length() > 0) sql += " AND ";
            String val = OAString.convertToLikeSearch(zip);
            if (val.indexOf("%") >= 0) {
                sql += prefix + Address.P_Zip + " LIKE ?";
            }
            else {
                sql += prefix + Address.P_Zip + " = ?";
            }
            args = OAArray.add(Object.class, args, val);
        }
        if (!isNull(P_Type)) {
            if (sql.length() > 0) sql += " AND ";
            if (!isNull(P_Type2) && type != type2) {
                sql += prefix + Address.P_Type + " >= ?";
                args = OAArray.add(Object.class, args, getType());
                sql += " AND " + prefix + Address.P_Type + " <= ?";
                args = OAArray.add(Object.class, args, getType2());
            }
            else {
                sql += prefix + Address.P_Type + " = ?";
                args = OAArray.add(Object.class, args, getType());
            }
        }
    if (OAString.isNotEmpty(this.customQuery)) {
        if (sql.length() > 0) sql += " AND ";
        sql += "(" + getCustomQuery() + ")";
    }
        select.add(sql, args);
    }

    private OAFilter<Address> filterDataSourceFilter;
    public OAFilter<Address> getDataSourceFilter() {
        if (filterDataSourceFilter != null) return filterDataSourceFilter;
        filterDataSourceFilter = new OAFilter<Address>() {
            @Override
            public boolean isUsed(Address address) {
                return AddressSearch.this.isUsedForDataSourceFilter(address);
            }
        };
        return filterDataSourceFilter;
    }
    
    private OAFilter<Address> filterCustomFilter;
    public OAFilter<Address> getCustomFilter() {
        if (filterCustomFilter != null) return filterCustomFilter;
        filterCustomFilter = new OAFilter<Address>() {
            @Override
            public boolean isUsed(Address address) {
                boolean b = AddressSearch.this.isUsedForCustomFilter(address);
                return b;
            }
        };
        return filterCustomFilter;
    }
    
    public boolean isUsedForDataSourceFilter(Address searchAddress) {
        return true;
    }
    public boolean isUsedForCustomFilter(Address searchAddress) {
        return true;
    }
}
