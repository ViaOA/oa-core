// Generated by OABuilder
package com.messagedesigner.model.oa.search;

import java.util.logging.*;

import com.messagedesigner.model.oa.*;
import com.messagedesigner.model.oa.propertypath.*;
import com.messagedesigner.model.oa.JsonType;
import com.messagedesigner.model.oa.search.JsonTypeSearch;
import com.viaoa.annotation.*;
import com.viaoa.object.*;
import com.viaoa.hub.*;
import com.viaoa.util.*;
import com.viaoa.util.OADateTime;
import com.viaoa.datasource.*;
import com.viaoa.filter.*;

@OAClass(useDataSource=false, localOnly=true)
public class JsonTypeSearch extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(JsonTypeSearch.class.getName());
    public static final String P_Id = "Id";
    public static final String P_Id2 = "Id2";
    public static final String P_Created = "Created";
    public static final String P_Name = "Name";
    public static final String P_Type = "Type";
    public static final String P_Type2 = "Type2";
    public static final String P_MaxResults = "MaxResults";

    protected int id;
    protected int id2;
    protected OADateTime created;
    protected String name;
    protected int type;
    protected int type2;
    protected int maxResults;

    public int getId() {
        return id;
    }
    public void setId(int newValue) {
        int old = id;
        fireBeforePropertyChange(P_Id, old, newValue);
        this.id = newValue;
        firePropertyChange(P_Id, old, this.id);
        if (isLoading()) return;
        if (id > id2) setId2(this.id);
    } 
    public int getId2() {
        return id2;
    }
    public void setId2(int newValue) {
        int old = id2;
        fireBeforePropertyChange(P_Id2, old, newValue);
        this.id2 = newValue;
        firePropertyChange(P_Id2, old, this.id2);
        if (isLoading()) return;
        if (id > id2) setId(this.id2);
    }

    public OADateTime getCreated() {
        return created;
    }
    public void setCreated(OADateTime newValue) {
        OADateTime old = created;
        fireBeforePropertyChange(P_Created, old, newValue);
        this.created = newValue;
        firePropertyChange(P_Created, old, this.created);
    }
      

    public String getName() {
        return name;
    }
    public void setName(String newValue) {
        String old = name;
        fireBeforePropertyChange(P_Name, old, newValue);
        this.name = newValue;
        firePropertyChange(P_Name, old, this.name);
    }
      

    public int getType() {
        return type;
    }
    public void setType(int newValue) {
        int old = type;
        fireBeforePropertyChange(P_Type, old, newValue);
        this.type = newValue;
        firePropertyChange(P_Type, old, this.type);
        firePropertyChange(P_Type + "String");
        firePropertyChange(P_Type + "Enum");
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
        firePropertyChange(P_Type + "String");
        firePropertyChange(P_Type + "Enum");
        if (isLoading()) return;
        if (type > type2) setType(this.type2);
    }

    public String getTypeString() {
        JsonType.Type type = getTypeEnum();
        if (type == null) return null;
        return type.name();
    }
    public void setTypeString(String val) {
        int x = -1;
        if (OAString.isNotEmpty(val)) {
            JsonType.Type type = JsonType.Type.valueOf(val);
            if (type != null) x = type.ordinal();
        }
        if (x < 0) setNull(P_Type);
        else setType(x);
    }


    public JsonType.Type getTypeEnum() {
        if (isNull(P_Type)) return null;
        final int val = getType();
        if (val < 0 || val >= JsonType.Type.values().length) return null;
        return JsonType.Type.values()[val];
    }

    public void setTypeEnum(JsonType.Type val) {
        if (val == null) {
            setNull(P_Type);
        }
        else {
            setType(val.ordinal());
        }
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
        setId(0);
        setNull(P_Id);
        setId2(0);
        setNull(P_Id2);
        setCreated(null);
        setName(null);
        setType(0);
        setNull(P_Type);
        setType2(0);
        setNull(P_Type2);
    }

    public boolean isDataEntered() {
        if (!isNull(P_Id)) return true;
        if (getCreated() != null) return true;
        if (getName() != null) return true;
        if (!isNull(P_Type)) return true;
        return false;
    }

    protected String extraWhere;
    protected Object[] extraWhereParams;
    protected OAFilter<JsonType> filterExtraWhere;

    public void setExtraWhere(String s, Object ... args) {
        this.extraWhere = s;
        this.extraWhereParams = args;
        if (OAString.isNotEmpty(s) && getExtraWhereFilter() == null) {
            OAFilter<JsonType> f = new OAQueryFilter<JsonType>(JsonType.class, s, args);
            setExtraWhereFilter(f);
        }
    }
    public void setExtraWhereFilter(OAFilter<JsonType> filter) {
        this.filterExtraWhere = filter;
    }
    public OAFilter<JsonType> getExtraWhereFilter() {
        return this.filterExtraWhere;
    }

    public OASelect<JsonType> getSelect() {
        final String prefix = "";
        String sql = "";
        String sortOrder = null;
        Object[] args = new Object[0];
        if (!isNull(P_Id)) {
            if (sql.length() > 0) sql += " AND ";
            if (!isNull(P_Id2) && id != id2) {
                sql += JsonType.P_Id + " >= ?";
                args = OAArray.add(Object.class, args, getId());
                sql += " AND " + JsonType.P_Id + " <= ?";
                args = OAArray.add(Object.class, args, getId2());
            }
            else {
                sql += JsonType.P_Id + " = ?";
                args = OAArray.add(Object.class, args, getId());
            }
        }
        if (created != null) {
            if (sql.length() > 0) sql += " AND ";
            sql += JsonType.P_Created + " = ?";
            args = OAArray.add(Object.class, args, this.created);
        }
        if (OAString.isNotEmpty(this.name)) {
            if (sql.length() > 0) sql += " AND ";
            String value = OAString.convertToLikeSearch(name);
            if (value.indexOf("%") >= 0) {
                sql += JsonType.P_Name + " LIKE ?";
            }
            else {
                sql += JsonType.P_Name + " = ?";
            }
            args = OAArray.add(Object.class, args, value);
        }
        if (!isNull(P_Type)) {
            if (sql.length() > 0) sql += " AND ";
            if (!isNull(P_Type2) && type != type2) {
                sql += JsonType.P_Type + " >= ?";
                args = OAArray.add(Object.class, args, getType());
                sql += " AND " + JsonType.P_Type + " <= ?";
                args = OAArray.add(Object.class, args, getType2());
            }
            else {
                sql += JsonType.P_Type + " = ?";
                args = OAArray.add(Object.class, args, getType());
            }
        }

        if (OAString.isNotEmpty(extraWhere)) {
            if (sql.length() > 0) sql = "(" + sql + ") AND ";
            sql += extraWhere;
            args = OAArray.add(Object.class, args, extraWhereParams);
        }

        OASelect<JsonType> select = new OASelect<JsonType>(JsonType.class, sql, args, sortOrder);
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
        if (!isNull(P_Id)) {
            if (sql.length() > 0) sql += " AND ";
            if (!isNull(P_Id2) && id != id2) {
                sql += prefix + JsonType.P_Id + " >= ?";
                args = OAArray.add(Object.class, args, getId());
                sql += " AND " + prefix + JsonType.P_Id + " <= ?";
                args = OAArray.add(Object.class, args, getId2());
            }
            else {
                sql += prefix + JsonType.P_Id + " = ?";
                args = OAArray.add(Object.class, args, getId());
            }
        }
        if (created != null) {
            if (sql.length() > 0) sql += " AND ";
            sql += prefix + JsonType.P_Created + " = ?";
            args = OAArray.add(Object.class, args, this.created);
        }
        if (OAString.isNotEmpty(this.name)) {
            if (sql.length() > 0) sql += " AND ";
            String value = OAString.convertToLikeSearch(name);
            if (value.indexOf("%") >= 0) {
                sql += prefix + JsonType.P_Name + " LIKE ?";
            }
            else {
                sql += prefix + JsonType.P_Name + " = ?";
            }
            args = OAArray.add(Object.class, args, value);
        }
        if (!isNull(P_Type)) {
            if (sql.length() > 0) sql += " AND ";
            if (!isNull(P_Type2) && type != type2) {
                sql += prefix + JsonType.P_Type + " >= ?";
                args = OAArray.add(Object.class, args, getType());
                sql += " AND " + prefix + JsonType.P_Type + " <= ?";
                args = OAArray.add(Object.class, args, getType2());
            }
            else {
                sql += prefix + JsonType.P_Type + " = ?";
                args = OAArray.add(Object.class, args, getType());
            }
        }
        select.add(sql, args);
    }

    private OAFilter<JsonType> filterDataSourceFilter;
    public OAFilter<JsonType> getDataSourceFilter() {
        if (filterDataSourceFilter != null) return filterDataSourceFilter;
        filterDataSourceFilter = new OAFilter<JsonType>() {
            @Override
            public boolean isUsed(JsonType jsonType) {
                return JsonTypeSearch.this.isUsedForDataSourceFilter(jsonType);
            }
        };
        return filterDataSourceFilter;
    }
    
    private OAFilter<JsonType> filterCustomFilter;
    public OAFilter<JsonType> getCustomFilter() {
        if (filterCustomFilter != null) return filterCustomFilter;
        filterCustomFilter = new OAFilter<JsonType>() {
            @Override
            public boolean isUsed(JsonType jsonType) {
                boolean b = JsonTypeSearch.this.isUsedForCustomFilter(jsonType);
                return b;
            }
        };
        return filterCustomFilter;
    }
    
    public boolean isUsedForDataSourceFilter(JsonType searchJsonType) {
        return true;
    }
    public boolean isUsedForCustomFilter(JsonType searchJsonType) {
        return true;
    }
}