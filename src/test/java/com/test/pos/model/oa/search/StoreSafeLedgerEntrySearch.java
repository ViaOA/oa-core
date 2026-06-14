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
public class StoreSafeLedgerEntrySearch extends OAObject {
    private static final long serialVersionUID = 1L;

    private static Logger LOG = Logger.getLogger(StoreSafeLedgerEntrySearch.class.getName());

    public static final String P_Created = "Created";
    public static final String P_Type = "Type";
    public static final String P_Type2 = "Type2";
    public static final String P_Amount = "Amount";
    public static final String P_Amount2 = "Amount2";
    public static final String P_CustomQuery = "CustomQuery";
    public static final String P_MaxResults = "MaxResults";

    protected OADateTime created;
    protected int type;
    protected int type2;
    protected double amount;
    protected double amount2;
    protected String customQuery;
    protected int maxResults;

    @OAProperty(lowerName = "created", defaultValue = "new OADateTime()", displayLength = 15, ignoreTimeZone = true)
    public OADateTime getCreated() {
        return created;
    }
    public void setCreated(OADateTime newValue) {
        OADateTime old = created;
        fireBeforePropertyChange(P_Created, old, newValue);
        this.created = newValue;
        firePropertyChange(P_Created, old, this.created);
    }
      
    @OAProperty(lowerName = "type", displayLength = 22, uiColumnLength = 19)
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
    @OAProperty(lowerName = "looseCashAmount", displayName = "Loose Cash Amount", decimalPlaces = 2, isCurrency = true, displayLength = 9, uiColumnLength = 17)
    public double getAmount() {
        return amount;
    }
    public void setAmount(double newValue) {
        double old = amount;
        fireBeforePropertyChange(P_Amount, old, newValue);
        this.amount = newValue;
        firePropertyChange(P_Amount, old, this.amount);
        if (isLoading()) return;
        if (amount > amount2) setAmount2(this.amount);
    } 
    public double getAmount2() {
        return amount2;
    }
    public void setAmount2(double newValue) {
        double old = amount2;
        fireBeforePropertyChange(P_Amount2, old, newValue);
        this.amount2 = newValue;
        firePropertyChange(P_Amount2, old, this.amount2);
        if (isLoading()) return;
        if (amount > amount2) setAmount(this.amount2);
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
        setCreated(null);
        setType(0);
        setNull(P_Type);
        setType2(0);
        setNull(P_Type2);
        setAmount(0);
        setNull(P_Amount);
        setAmount2(0);
        setNull(P_Amount2);
        setCustomQuery(null);
    }

    public boolean isDataEntered() {
        if (getCreated() != null) return true;
        if (!isNull(P_Type)) return true;
        if (!isNull(P_Amount)) return true;
        if (getCustomQuery() != null) return true;
        return false;
    }

    protected String extraWhere;
    protected Object[] extraWhereParams;
    protected OAFilter<StoreSafeLedgerEntry> filterExtraWhere;

    public void setExtraWhere(String s, Object ... args) {
        this.extraWhere = s;
        this.extraWhereParams = args;
        if (OAString.isNotEmpty(s) && getExtraWhereFilter() == null) {
            OAFilter<StoreSafeLedgerEntry> f = new OAQueryFilter<StoreSafeLedgerEntry>(StoreSafeLedgerEntry.class, s, args);
            setExtraWhereFilter(f);
        }
    }
    public void setExtraWhereFilter(OAFilter<StoreSafeLedgerEntry> filter) {
        this.filterExtraWhere = filter;
    }
    public OAFilter<StoreSafeLedgerEntry> getExtraWhereFilter() {
        return this.filterExtraWhere;
    }

    public OASelect<StoreSafeLedgerEntry> getSelect() {
        final String prefix = "";
        String sql = "";
        String sortOrder = null;
        Object[] args = new Object[0];
        if (created != null) {
            if (sql.length() > 0) sql += " AND ";
            sql += StoreSafeLedgerEntry.P_Created + " = ?";
            args = OAArray.add(Object.class, args, this.created);
        }
        if (!isNull(P_Type)) {
            if (sql.length() > 0) sql += " AND ";
            if (!isNull(P_Type2) && type != type2) {
                sql += StoreSafeLedgerEntry.P_Type + " >= ?";
                args = OAArray.add(Object.class, args, getType());
                sql += " AND " + StoreSafeLedgerEntry.P_Type + " <= ?";
                args = OAArray.add(Object.class, args, getType2());
            }
            else {
                sql += StoreSafeLedgerEntry.P_Type + " = ?";
                args = OAArray.add(Object.class, args, getType());
            }
        }
        if (!isNull(P_Amount)) {
            if (sql.length() > 0) sql += " AND ";
            if (!isNull(P_Amount2) && amount != amount2) {
                sql += StoreSafeLedgerEntry.P_LooseCashAmount + " >= ?";
                args = OAArray.add(Object.class, args, getAmount());
                sql += " AND " + StoreSafeLedgerEntry.P_LooseCashAmount + " <= ?";
                args = OAArray.add(Object.class, args, getAmount2());
            }
            else {
                sql += StoreSafeLedgerEntry.P_LooseCashAmount + " = ?";
                args = OAArray.add(Object.class, args, getAmount());
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

        OASelect<StoreSafeLedgerEntry> select = new OASelect<StoreSafeLedgerEntry>(StoreSafeLedgerEntry.class, sql, args, sortOrder);
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
        if (created != null) {
            if (sql.length() > 0) sql += " AND ";
            sql += prefix + StoreSafeLedgerEntry.P_Created + " = ?";
            args = OAArray.add(Object.class, args, this.created);
        }
        if (!isNull(P_Type)) {
            if (sql.length() > 0) sql += " AND ";
            if (!isNull(P_Type2) && type != type2) {
                sql += prefix + StoreSafeLedgerEntry.P_Type + " >= ?";
                args = OAArray.add(Object.class, args, getType());
                sql += " AND " + prefix + StoreSafeLedgerEntry.P_Type + " <= ?";
                args = OAArray.add(Object.class, args, getType2());
            }
            else {
                sql += prefix + StoreSafeLedgerEntry.P_Type + " = ?";
                args = OAArray.add(Object.class, args, getType());
            }
        }
        if (!isNull(P_Amount)) {
            if (sql.length() > 0) sql += " AND ";
            if (!isNull(P_Amount2) && amount != amount2) {
                sql += prefix + StoreSafeLedgerEntry.P_LooseCashAmount + " >= ?";
                args = OAArray.add(Object.class, args, getAmount());
                sql += " AND " + prefix + StoreSafeLedgerEntry.P_LooseCashAmount + " <= ?";
                args = OAArray.add(Object.class, args, getAmount2());
            }
            else {
                sql += prefix + StoreSafeLedgerEntry.P_LooseCashAmount + " = ?";
                args = OAArray.add(Object.class, args, getAmount());
            }
        }
    if (OAString.isNotEmpty(this.customQuery)) {
        if (sql.length() > 0) sql += " AND ";
        sql += "(" + getCustomQuery() + ")";
    }
        select.add(sql, args);
    }

    private OAFilter<StoreSafeLedgerEntry> filterDataSourceFilter;
    public OAFilter<StoreSafeLedgerEntry> getDataSourceFilter() {
        if (filterDataSourceFilter != null) return filterDataSourceFilter;
        filterDataSourceFilter = new OAFilter<StoreSafeLedgerEntry>() {
            @Override
            public boolean isUsed(StoreSafeLedgerEntry storeSafeLedgerEntry) {
                return StoreSafeLedgerEntrySearch.this.isUsedForDataSourceFilter(storeSafeLedgerEntry);
            }
        };
        return filterDataSourceFilter;
    }
    
    private OAFilter<StoreSafeLedgerEntry> filterCustomFilter;
    public OAFilter<StoreSafeLedgerEntry> getCustomFilter() {
        if (filterCustomFilter != null) return filterCustomFilter;
        filterCustomFilter = new OAFilter<StoreSafeLedgerEntry>() {
            @Override
            public boolean isUsed(StoreSafeLedgerEntry storeSafeLedgerEntry) {
                boolean b = StoreSafeLedgerEntrySearch.this.isUsedForCustomFilter(storeSafeLedgerEntry);
                return b;
            }
        };
        return filterCustomFilter;
    }
    
    public boolean isUsedForDataSourceFilter(StoreSafeLedgerEntry searchStoreSafeLedgerEntry) {
        return true;
    }
    public boolean isUsedForCustomFilter(StoreSafeLedgerEntry searchStoreSafeLedgerEntry) {
        return true;
    }
}
