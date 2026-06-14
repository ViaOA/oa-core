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
import com.viaoa.datetime.OADate;
import com.viaoa.datasource.*;
import com.viaoa.filter.*;
import com.viaoa.find.*;

@OAClass(useDataSource=false, localOnly=true)
public class PriceBookEntrySearch extends OAObject {
    private static final long serialVersionUID = 1L;

    private static Logger LOG = Logger.getLogger(PriceBookEntrySearch.class.getName());

    public static final String P_Name = "Name";
    public static final String P_FromDate = "FromDate";
    public static final String P_ToDate = "ToDate";
    public static final String P_Promotion = "Promotion";
    public static final String P_PromotionUseNull = "PromotionUseNull";
    public static final String P_PromotionUseNotNull = "PromotionUseNotNull";
    public static final String P_Priority = "Priority";
    public static final String P_Priority2 = "Priority2";
    public static final String P_CustomQuery = "CustomQuery";
    public static final String P_MaxResults = "MaxResults";

    protected String name;
    protected OADate fromDate;
    protected OADate toDate;
    protected boolean promotion;
    protected boolean promotionUseNull;
    protected boolean promotionUseNotNull;
    protected int priority;
    protected int priority2;
    protected String customQuery;
    protected int maxResults;

    @OAProperty(lowerName = "name", maxLength = 30, displayLength = 18)
    public String getName() {
        return name;
    }
    public void setName(String newValue) {
        String old = name;
        fireBeforePropertyChange(P_Name, old, newValue);
        this.name = newValue;
        firePropertyChange(P_Name, old, this.name);
    }
      
    @OAProperty(lowerName = "fromDate", displayName = "From Date", displayLength = 8, uiColumnLength = 9)
    public OADate getFromDate() {
        return fromDate;
    }
    public void setFromDate(OADate newValue) {
        OADate old = fromDate;
        fireBeforePropertyChange(P_FromDate, old, newValue);
        this.fromDate = newValue;
        firePropertyChange(P_FromDate, old, this.fromDate);
    }
      
    @OAProperty(lowerName = "toDate", displayName = "To Date", displayLength = 8)
    public OADate getToDate() {
        return toDate;
    }
    public void setToDate(OADate newValue) {
        OADate old = toDate;
        fireBeforePropertyChange(P_ToDate, old, newValue);
        this.toDate = newValue;
        firePropertyChange(P_ToDate, old, this.toDate);
    }
      
    @OAProperty(lowerName = "promotion", displayLength = 5, uiColumnLength = 9)
    public boolean getPromotion() {
        return promotion;
    }
    public boolean isPromotion() {
        return getPromotion();
    }
    public void setPromotion(boolean newValue) {
        boolean old = promotion;
        fireBeforePropertyChange(P_Promotion, old, newValue);
        this.promotion = newValue;
        firePropertyChange(P_Promotion, old, this.promotion);
    }
      
    public boolean getPromotionUseNull() {
        return promotionUseNull;
    }
    public void setPromotionUseNull(boolean newValue) {
        boolean old = this.promotionUseNull;
        this.promotionUseNull = newValue;
        firePropertyChange(P_PromotionUseNull, old, this.promotionUseNull);
    }
    public boolean getPromotionUseNotNull() {
        return promotionUseNotNull;
    }
    public void setPromotionUseNotNull(boolean newValue) {
        boolean old = this.promotionUseNotNull;
        this.promotionUseNotNull = newValue;
        firePropertyChange(P_PromotionUseNotNull, old, this.promotionUseNotNull);
    }
    @OAProperty(lowerName = "priority", displayLength = 6, uiColumnLength = 8)
    public int getPriority() {
        return priority;
    }
    public void setPriority(int newValue) {
        int old = priority;
        fireBeforePropertyChange(P_Priority, old, newValue);
        this.priority = newValue;
        firePropertyChange(P_Priority, old, this.priority);
        if (isLoading()) return;
        if (priority > priority2) setPriority2(this.priority);
    } 
    public int getPriority2() {
        return priority2;
    }
    public void setPriority2(int newValue) {
        int old = priority2;
        fireBeforePropertyChange(P_Priority2, old, newValue);
        this.priority2 = newValue;
        firePropertyChange(P_Priority2, old, this.priority2);
        if (isLoading()) return;
        if (priority > priority2) setPriority(this.priority2);
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
        setFromDate(null);
        setToDate(null);
        setPromotion(false);
        setNull(P_Promotion);
        setPromotionUseNull(false);
        setPromotionUseNotNull(false);
        setPriority(0);
        setNull(P_Priority);
        setPriority2(0);
        setNull(P_Priority2);
        setCustomQuery(null);
    }

    public boolean isDataEntered() {
        if (getName() != null) return true;
        if (getFromDate() != null) return true;
        if (getToDate() != null) return true;
        if (!isNull(P_Promotion)) return true;
        if (getPromotionUseNull()) return true;
        if (getPromotionUseNotNull()) return true;
        if (!isNull(P_Priority)) return true;
        if (getCustomQuery() != null) return true;
        return false;
    }

    protected String extraWhere;
    protected Object[] extraWhereParams;
    protected OAFilter<PriceBookEntry> filterExtraWhere;

    public void setExtraWhere(String s, Object ... args) {
        this.extraWhere = s;
        this.extraWhereParams = args;
        if (OAString.isNotEmpty(s) && getExtraWhereFilter() == null) {
            OAFilter<PriceBookEntry> f = new OAQueryFilter<PriceBookEntry>(PriceBookEntry.class, s, args);
            setExtraWhereFilter(f);
        }
    }
    public void setExtraWhereFilter(OAFilter<PriceBookEntry> filter) {
        this.filterExtraWhere = filter;
    }
    public OAFilter<PriceBookEntry> getExtraWhereFilter() {
        return this.filterExtraWhere;
    }

    public OASelect<PriceBookEntry> getSelect() {
        final String prefix = "";
        String sql = "";
        String sortOrder = null;
        Object[] args = new Object[0];
        if (OAString.isNotEmpty(this.name)) {
            if (sql.length() > 0) sql += " AND ";
            String val = OAString.convertToLikeSearch(name);
            if (val.indexOf("%") >= 0) {
                sql += PriceBookEntry.P_Name + " LIKE ?";
            }
            else {
                sql += PriceBookEntry.P_Name + " = ?";
            }
            args = OAArray.add(Object.class, args, val);
        }
        if (fromDate != null) {
            if (sql.length() > 0) sql += " AND ";
            sql += PriceBookEntry.P_FromDate + " = ?";
            args = OAArray.add(Object.class, args, this.fromDate);
        }
        if (toDate != null) {
            if (sql.length() > 0) sql += " AND ";
            sql += PriceBookEntry.P_ToDate + " = ?";
            args = OAArray.add(Object.class, args, this.toDate);
        }
        if (promotionUseNull) {
            if (sql.length() > 0) sql += " AND ";
            sql += PriceBookEntry.P_Promotion + " = null";
        }
        else if (promotionUseNotNull) {
            if (sql.length() > 0) sql += " AND ";
            sql += PriceBookEntry.P_Promotion + " != null";
        }
        if (!isNull(P_Promotion)) {
            if (sql.length() > 0) sql += " AND ";
            sql += PriceBookEntry.P_Promotion + " = ?";
            args = OAArray.add(Object.class, args, this.promotion);
        }
        if (!isNull(P_Priority)) {
            if (sql.length() > 0) sql += " AND ";
            if (!isNull(P_Priority2) && priority != priority2) {
                sql += PriceBookEntry.P_Priority + " >= ?";
                args = OAArray.add(Object.class, args, getPriority());
                sql += " AND " + PriceBookEntry.P_Priority + " <= ?";
                args = OAArray.add(Object.class, args, getPriority2());
            }
            else {
                sql += PriceBookEntry.P_Priority + " = ?";
                args = OAArray.add(Object.class, args, getPriority());
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

        OASelect<PriceBookEntry> select = new OASelect<PriceBookEntry>(PriceBookEntry.class, sql, args, sortOrder);
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
                sql += prefix + PriceBookEntry.P_Name + " LIKE ?";
            }
            else {
                sql += prefix + PriceBookEntry.P_Name + " = ?";
            }
            args = OAArray.add(Object.class, args, val);
        }
        if (fromDate != null) {
            if (sql.length() > 0) sql += " AND ";
            sql += prefix + PriceBookEntry.P_FromDate + " = ?";
            args = OAArray.add(Object.class, args, this.fromDate);
        }
        if (toDate != null) {
            if (sql.length() > 0) sql += " AND ";
            sql += prefix + PriceBookEntry.P_ToDate + " = ?";
            args = OAArray.add(Object.class, args, this.toDate);
        }
        if (promotionUseNull) {
            if (sql.length() > 0) sql += " AND ";
            sql += prefix + PriceBookEntry.P_Promotion + " = null";
        }
        else if (promotionUseNotNull) {
            if (sql.length() > 0) sql += " AND ";
            sql += prefix + PriceBookEntry.P_Promotion + " != null";
        }
        if (!isNull(P_Promotion)) {
            if (sql.length() > 0) sql += " AND ";
            sql += prefix + PriceBookEntry.P_Promotion + " = ?";
            args = OAArray.add(Object.class, args, this.promotion);
        }
        if (!isNull(P_Priority)) {
            if (sql.length() > 0) sql += " AND ";
            if (!isNull(P_Priority2) && priority != priority2) {
                sql += prefix + PriceBookEntry.P_Priority + " >= ?";
                args = OAArray.add(Object.class, args, getPriority());
                sql += " AND " + prefix + PriceBookEntry.P_Priority + " <= ?";
                args = OAArray.add(Object.class, args, getPriority2());
            }
            else {
                sql += prefix + PriceBookEntry.P_Priority + " = ?";
                args = OAArray.add(Object.class, args, getPriority());
            }
        }
    if (OAString.isNotEmpty(this.customQuery)) {
        if (sql.length() > 0) sql += " AND ";
        sql += "(" + getCustomQuery() + ")";
    }
        select.add(sql, args);
    }

    private OAFilter<PriceBookEntry> filterDataSourceFilter;
    public OAFilter<PriceBookEntry> getDataSourceFilter() {
        if (filterDataSourceFilter != null) return filterDataSourceFilter;
        filterDataSourceFilter = new OAFilter<PriceBookEntry>() {
            @Override
            public boolean isUsed(PriceBookEntry priceBookEntry) {
                return PriceBookEntrySearch.this.isUsedForDataSourceFilter(priceBookEntry);
            }
        };
        return filterDataSourceFilter;
    }
    
    private OAFilter<PriceBookEntry> filterCustomFilter;
    public OAFilter<PriceBookEntry> getCustomFilter() {
        if (filterCustomFilter != null) return filterCustomFilter;
        filterCustomFilter = new OAFilter<PriceBookEntry>() {
            @Override
            public boolean isUsed(PriceBookEntry priceBookEntry) {
                boolean b = PriceBookEntrySearch.this.isUsedForCustomFilter(priceBookEntry);
                return b;
            }
        };
        return filterCustomFilter;
    }
    
    public boolean isUsedForDataSourceFilter(PriceBookEntry searchPriceBookEntry) {
        return true;
    }
    public boolean isUsedForCustomFilter(PriceBookEntry searchPriceBookEntry) {
        return true;
    }
}
