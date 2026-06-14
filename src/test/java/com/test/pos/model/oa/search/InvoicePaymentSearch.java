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
import com.viaoa.find.OAFinder;

@OAClass(useDataSource=false, localOnly=true)
public class InvoicePaymentSearch extends OAObject {
    private static final long serialVersionUID = 1L;

    private static Logger LOG = Logger.getLogger(InvoicePaymentSearch.class.getName());

    public static final String P_Created = "Created";
    public static final String P_Created2 = "Created2";
    public static final String P_Type = "Type";
    public static final String P_Invoice = "Invoice";
    public static final String P_UseInvoiceSearch = "UseInvoiceSearch";
    public static final String P_CustomQuery = "CustomQuery";
    public static final String P_MaxResults = "MaxResults";

    protected OADateTime created;
    protected OADateTime created2;
    protected int type;
    protected Invoice invoice;
    protected boolean useInvoiceSearch;
    protected InvoiceSearch searchInvoice;
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
        if (isLoading()) return;
        if (created != null) {
            if (created2 == null) setCreated2(this.created.plusDays(1));
            else if (created.compareTo(created2) > 0) setCreated2(this.created.plusDays(1));
        }
    } 
    public OADateTime getCreated2() {
        return created2;
    }
    public void setCreated2(OADateTime newValue) {
        OADateTime old = created2;
        fireBeforePropertyChange(P_Created2, old, newValue);
        this.created2 = newValue;
        firePropertyChange(P_Created2, old, this.created2);
        if (created != null && created2 != null) {
            if (created.compareTo(created2) > 0) setCreated(this.created2);
        }
    }
    @OAProperty(lowerName = "type", displayLength = 10, uiColumnLength = 11)
    public int getType() {
        return type;
    }
    public void setType(int newValue) {
        int old = type;
        fireBeforePropertyChange(P_Type, old, newValue);
        this.type = newValue;
        firePropertyChange(P_Type, old, this.type);
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

    @OAOne
    public Invoice getInvoice() {
        if (invoice == null) {
            invoice = (Invoice) getObject(P_Invoice);
        }
        return invoice;
    }
    public void setInvoice(Invoice newValue) {
        Invoice old = this.invoice;
        this.invoice = newValue;
        firePropertyChange(P_Invoice, old, this.invoice);
    }
    public boolean getUseInvoiceSearch() {
        return useInvoiceSearch;
    }
    public void setUseInvoiceSearch(boolean newValue) {
        boolean old = this.useInvoiceSearch;
        this.useInvoiceSearch = newValue;
        firePropertyChange(P_UseInvoiceSearch, old, this.useInvoiceSearch);
    }
    public InvoiceSearch getInvoiceSearch() {
        return this.searchInvoice;
    }
    public void setInvoiceSearch(InvoiceSearch newValue) {
        this.searchInvoice = newValue;
    }

    public void reset() {
        setCreated(null);
        setCreated2(null);
        setType(0);
        setNull(P_Type);
        setInvoice(null);
        setUseInvoiceSearch(false);
        setCustomQuery(null);
    }

    public boolean isDataEntered() {
        if (getCreated() != null) return true;
        if (!isNull(P_Type)) return true;
        if (getInvoice() != null) return true;
        if (getUseInvoiceSearch()) return true;
        if (getCustomQuery() != null) return true;
        return false;
    }

    protected String extraWhere;
    protected Object[] extraWhereParams;
    protected OAFilter<InvoicePayment> filterExtraWhere;

    public void setExtraWhere(String s, Object ... args) {
        this.extraWhere = s;
        this.extraWhereParams = args;
        if (OAString.isNotEmpty(s) && getExtraWhereFilter() == null) {
            OAFilter<InvoicePayment> f = new OAQueryFilter<InvoicePayment>(InvoicePayment.class, s, args);
            setExtraWhereFilter(f);
        }
    }
    public void setExtraWhereFilter(OAFilter<InvoicePayment> filter) {
        this.filterExtraWhere = filter;
    }
    public OAFilter<InvoicePayment> getExtraWhereFilter() {
        return this.filterExtraWhere;
    }

    public OASelect<InvoicePayment> getSelect() {
        final String prefix = "";
        String sql = "";
        String sortOrder = null;
        Object[] args = new Object[0];
        OAFinder finder = null;
        if (created != null) {
            if (sql.length() > 0) sql += " AND ";
            if (created2 != null && !created.equals(created2)) {
                sql += InvoicePayment.P_Created + " >= ?";
                args = OAArray.add(Object.class, args, this.created);
                sql += " AND " + InvoicePayment.P_Created + " <= ?";
                args = OAArray.add(Object.class, args, this.created2);
            }
            else {
                sql += InvoicePayment.P_Created + " = ?";
                args = OAArray.add(Object.class, args, this.created);
            }
        }
        if (!isNull(P_Type)) {
            if (sql.length() > 0) sql += " AND ";
            sql += InvoicePayment.P_Type + " = ?";
            args = OAArray.add(Object.class, args, this.type);
        }
    if (OAString.isNotEmpty(this.customQuery)) {
        if (sql.length() > 0) sql += " AND ";
        sql += "(" + getCustomQuery() + ")";
    }
        if (!useInvoiceSearch && getInvoice() != null) {
            if (sql.length() > 0) sql += " AND ";
            sql += InvoicePaymentPP.invoice().pp + " = ?";
            args = OAArray.add(Object.class, args, getInvoice());
            finder = new OAFinder<Invoice, InvoicePayment>(getInvoice(), Invoice.P_InvoicePayments);
        }

        if (OAString.isNotEmpty(extraWhere)) {
            if (sql.length() > 0) sql = "(" + sql + ") AND ";
            sql += extraWhere;
            args = OAArray.add(Object.class, args, extraWhereParams);
        }

        OASelect<InvoicePayment> select = new OASelect<InvoicePayment>(InvoicePayment.class, sql, args, sortOrder);
        if (getExtraWhereFilter() != null && getExtraWhereFilter().updateSelect(select)) {
            select.setFilter(new OAAndFilter(this.getCustomFilter(), getExtraWhereFilter()));
        }
        else select.setFilter(this.getCustomFilter());
        select.setDataSourceFilter(this.getDataSourceFilter());
        select.setFinder(finder);
        if (getMaxResults() > 0) select.setMax(getMaxResults());
        if (useInvoiceSearch && getInvoiceSearch() != null) {
            getInvoiceSearch().appendSelect(InvoicePaymentPP.invoice().pp, select);
        }
        return select;
    }

    public void appendSelect(final String fromName, final OASelect select) {
        final String prefix = fromName + ".";
        String sql = "";
        Object[] args = new Object[0];
        if (created != null) {
            if (sql.length() > 0) sql += " AND ";
            if (created2 != null && !created.equals(created2)) {
                sql += prefix + InvoicePayment.P_Created + " >= ?";
                args = OAArray.add(Object.class, args, this.created);
                sql += " AND " + prefix + InvoicePayment.P_Created + " <= ?";
                args = OAArray.add(Object.class, args, this.created2);
            }
            else {
                sql += prefix + InvoicePayment.P_Created + " = ?";
                args = OAArray.add(Object.class, args, this.created);
            }
        }
        if (!isNull(P_Type)) {
            if (sql.length() > 0) sql += " AND ";
            sql += prefix + InvoicePayment.P_Type + " = ?";
            args = OAArray.add(Object.class, args, this.type);
        }
    if (OAString.isNotEmpty(this.customQuery)) {
        if (sql.length() > 0) sql += " AND ";
        sql += "(" + getCustomQuery() + ")";
    }
        if (!useInvoiceSearch && getInvoice() != null) {
            if (sql.length() > 0) sql += " AND ";
            sql += prefix + InvoicePaymentPP.invoice().pp + " = ?";
            args = OAArray.add(Object.class, args, getInvoice());
        }
        if (useInvoiceSearch && getInvoiceSearch() != null) {
            getInvoiceSearch().appendSelect(prefix + InvoicePaymentPP.invoice().pp, select);
        }
        select.add(sql, args);
    }

    private OAFilter<InvoicePayment> filterDataSourceFilter;
    public OAFilter<InvoicePayment> getDataSourceFilter() {
        if (filterDataSourceFilter != null) return filterDataSourceFilter;
        filterDataSourceFilter = new OAFilter<InvoicePayment>() {
            @Override
            public boolean isUsed(InvoicePayment invoicePayment) {
                return InvoicePaymentSearch.this.isUsedForDataSourceFilter(invoicePayment);
            }
        };
        return filterDataSourceFilter;
    }
    
    private OAFilter<InvoicePayment> filterCustomFilter;
    public OAFilter<InvoicePayment> getCustomFilter() {
        if (filterCustomFilter != null) return filterCustomFilter;
        filterCustomFilter = new OAFilter<InvoicePayment>() {
            @Override
            public boolean isUsed(InvoicePayment invoicePayment) {
                boolean b = InvoicePaymentSearch.this.isUsedForCustomFilter(invoicePayment);
                return b;
            }
        };
        return filterCustomFilter;
    }
    
    public boolean isUsedForDataSourceFilter(InvoicePayment searchInvoicePayment) {
        return true;
    }
    public boolean isUsedForCustomFilter(InvoicePayment searchInvoicePayment) {
        return true;
    }
}
