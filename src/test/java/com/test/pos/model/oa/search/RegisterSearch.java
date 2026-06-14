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
public class RegisterSearch extends OAObject {
    private static final long serialVersionUID = 1L;

    private static Logger LOG = Logger.getLogger(RegisterSearch.class.getName());

    public static final String P_Code = "Code";
    public static final String P_CustomQuery = "CustomQuery";
    public static final String P_MaxResults = "MaxResults";

    protected String code;
    protected String customQuery;
    protected int maxResults;

    @OAProperty(lowerName = "code", maxLength = 15, displayLength = 10)
    public String getCode() {
        return code;
    }
    public void setCode(String newValue) {
        String old = code;
        fireBeforePropertyChange(P_Code, old, newValue);
        this.code = newValue;
        firePropertyChange(P_Code, old, this.code);
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
        setCode(null);
        setCustomQuery(null);
    }

    public boolean isDataEntered() {
        if (getCode() != null) return true;
        if (getCustomQuery() != null) return true;
        return false;
    }

    protected String extraWhere;
    protected Object[] extraWhereParams;
    protected OAFilter<Register> filterExtraWhere;

    public void setExtraWhere(String s, Object ... args) {
        this.extraWhere = s;
        this.extraWhereParams = args;
        if (OAString.isNotEmpty(s) && getExtraWhereFilter() == null) {
            OAFilter<Register> f = new OAQueryFilter<Register>(Register.class, s, args);
            setExtraWhereFilter(f);
        }
    }
    public void setExtraWhereFilter(OAFilter<Register> filter) {
        this.filterExtraWhere = filter;
    }
    public OAFilter<Register> getExtraWhereFilter() {
        return this.filterExtraWhere;
    }

    public OASelect<Register> getSelect() {
        final String prefix = "";
        String sql = "";
        String sortOrder = null;
        Object[] args = new Object[0];
        if (OAString.isNotEmpty(this.code)) {
            if (sql.length() > 0) sql += " AND ";
            String val = OAString.convertToLikeSearch(code);
            if (val.indexOf("%") >= 0) {
                sql += Register.P_Code + " LIKE ?";
            }
            else {
                sql += Register.P_Code + " = ?";
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

        OASelect<Register> select = new OASelect<Register>(Register.class, sql, args, sortOrder);
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
        if (OAString.isNotEmpty(this.code)) {
            if (sql.length() > 0) sql += " AND ";
            String val = OAString.convertToLikeSearch(code);
            if (val.indexOf("%") >= 0) {
                sql += prefix + Register.P_Code + " LIKE ?";
            }
            else {
                sql += prefix + Register.P_Code + " = ?";
            }
            args = OAArray.add(Object.class, args, val);
        }
    if (OAString.isNotEmpty(this.customQuery)) {
        if (sql.length() > 0) sql += " AND ";
        sql += "(" + getCustomQuery() + ")";
    }
        select.add(sql, args);
    }

    private OAFilter<Register> filterDataSourceFilter;
    public OAFilter<Register> getDataSourceFilter() {
        if (filterDataSourceFilter != null) return filterDataSourceFilter;
        filterDataSourceFilter = new OAFilter<Register>() {
            @Override
            public boolean isUsed(Register register) {
                return RegisterSearch.this.isUsedForDataSourceFilter(register);
            }
        };
        return filterDataSourceFilter;
    }
    
    private OAFilter<Register> filterCustomFilter;
    public OAFilter<Register> getCustomFilter() {
        if (filterCustomFilter != null) return filterCustomFilter;
        filterCustomFilter = new OAFilter<Register>() {
            @Override
            public boolean isUsed(Register register) {
                boolean b = RegisterSearch.this.isUsedForCustomFilter(register);
                return b;
            }
        };
        return filterCustomFilter;
    }
    
    public boolean isUsedForDataSourceFilter(Register searchRegister) {
        return true;
    }
    public boolean isUsedForCustomFilter(Register searchRegister) {
        return true;
    }
}
