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
public class InvoiceSearch extends OAObject {
    private static final long serialVersionUID = 1L;

    private static Logger LOG = Logger.getLogger(InvoiceSearch.class.getName());

    public static final String P_Id = "Id";
    public static final String P_Id2 = "Id2";
    public static final String P_Created = "Created";
    public static final String P_Created2 = "Created2";
    public static final String P_Customer = "Customer";
    public static final String P_UseCustomerSearch = "UseCustomerSearch";
    public static final String P_CustomQuery = "CustomQuery";
    public static final String P_MaxResults = "MaxResults";

    protected int id;
    protected int id2;
    protected OADateTime created;
    protected OADateTime created2;
    protected Customer customer;
    protected boolean useCustomerSearch;
    protected CustomerSearch searchCustomer;
    protected String customQuery;
    protected int maxResults;

    @OAProperty(lowerName = "id", displayLength = 6)
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
    public Customer getCustomer() {
        if (customer == null) {
            customer = (Customer) getObject(P_Customer);
        }
        return customer;
    }
    public void setCustomer(Customer newValue) {
        Customer old = this.customer;
        this.customer = newValue;
        firePropertyChange(P_Customer, old, this.customer);
    }
    public boolean getUseCustomerSearch() {
        return useCustomerSearch;
    }
    public void setUseCustomerSearch(boolean newValue) {
        boolean old = this.useCustomerSearch;
        this.useCustomerSearch = newValue;
        firePropertyChange(P_UseCustomerSearch, old, this.useCustomerSearch);
    }
    public CustomerSearch getCustomerSearch() {
        return this.searchCustomer;
    }
    public void setCustomerSearch(CustomerSearch newValue) {
        this.searchCustomer = newValue;
    }

    public void reset() {
        setId(0);
        setNull(P_Id);
        setId2(0);
        setNull(P_Id2);
        setCreated(null);
        setCreated2(null);
        setCustomer(null);
        setUseCustomerSearch(false);
        setCustomQuery(null);
    }

    public boolean isDataEntered() {
        if (!isNull(P_Id)) return true;
        if (getCreated() != null) return true;
        if (getCustomer() != null) return true;
        if (getUseCustomerSearch()) return true;
        if (getCustomQuery() != null) return true;
        return false;
    }

    protected String extraWhere;
    protected Object[] extraWhereParams;
    protected OAFilter<Invoice> filterExtraWhere;

    public void setExtraWhere(String s, Object ... args) {
        this.extraWhere = s;
        this.extraWhereParams = args;
        if (OAString.isNotEmpty(s) && getExtraWhereFilter() == null) {
            OAFilter<Invoice> f = new OAQueryFilter<Invoice>(Invoice.class, s, args);
            setExtraWhereFilter(f);
        }
    }
    public void setExtraWhereFilter(OAFilter<Invoice> filter) {
        this.filterExtraWhere = filter;
    }
    public OAFilter<Invoice> getExtraWhereFilter() {
        return this.filterExtraWhere;
    }

    public OASelect<Invoice> getSelect() {
        final String prefix = "";
        String sql = "";
        String sortOrder = null;
        Object[] args = new Object[0];
        OAFinder finder = null;
        if (!isNull(P_Id)) {
            if (sql.length() > 0) sql += " AND ";
            if (!isNull(P_Id2) && id != id2) {
                sql += Invoice.P_Id + " >= ?";
                args = OAArray.add(Object.class, args, getId());
                sql += " AND " + Invoice.P_Id + " <= ?";
                args = OAArray.add(Object.class, args, getId2());
            }
            else {
                sql += Invoice.P_Id + " = ?";
                args = OAArray.add(Object.class, args, getId());
            }
        }
        if (created != null) {
            if (sql.length() > 0) sql += " AND ";
            if (created2 != null && !created.equals(created2)) {
                sql += Invoice.P_Created + " >= ?";
                args = OAArray.add(Object.class, args, this.created);
                sql += " AND " + Invoice.P_Created + " <= ?";
                args = OAArray.add(Object.class, args, this.created2);
            }
            else {
                sql += Invoice.P_Created + " = ?";
                args = OAArray.add(Object.class, args, this.created);
            }
        }
    if (OAString.isNotEmpty(this.customQuery)) {
        if (sql.length() > 0) sql += " AND ";
        sql += "(" + getCustomQuery() + ")";
    }
        if (!useCustomerSearch && getCustomer() != null) {
            if (sql.length() > 0) sql += " AND ";
            sql += InvoicePP.customer().pp + " = ?";
            args = OAArray.add(Object.class, args, getCustomer());
            finder = new OAFinder<Customer, Invoice>(getCustomer(), Customer.P_Invoices);
        }

        if (OAString.isNotEmpty(extraWhere)) {
            if (sql.length() > 0) sql = "(" + sql + ") AND ";
            sql += extraWhere;
            args = OAArray.add(Object.class, args, extraWhereParams);
        }

        OASelect<Invoice> select = new OASelect<Invoice>(Invoice.class, sql, args, sortOrder);
        if (getExtraWhereFilter() != null && getExtraWhereFilter().updateSelect(select)) {
            select.setFilter(new OAAndFilter(this.getCustomFilter(), getExtraWhereFilter()));
        }
        else select.setFilter(this.getCustomFilter());
        select.setDataSourceFilter(this.getDataSourceFilter());
        select.setFinder(finder);
        if (getMaxResults() > 0) select.setMax(getMaxResults());
        if (useCustomerSearch && getCustomerSearch() != null) {
            getCustomerSearch().appendSelect(InvoicePP.customer().pp, select);
        }
        return select;
    }

    public void appendSelect(final String fromName, final OASelect select) {
        final String prefix = fromName + ".";
        String sql = "";
        Object[] args = new Object[0];
        if (!isNull(P_Id)) {
            if (sql.length() > 0) sql += " AND ";
            if (!isNull(P_Id2) && id != id2) {
                sql += prefix + Invoice.P_Id + " >= ?";
                args = OAArray.add(Object.class, args, getId());
                sql += " AND " + prefix + Invoice.P_Id + " <= ?";
                args = OAArray.add(Object.class, args, getId2());
            }
            else {
                sql += prefix + Invoice.P_Id + " = ?";
                args = OAArray.add(Object.class, args, getId());
            }
        }
        if (created != null) {
            if (sql.length() > 0) sql += " AND ";
            if (created2 != null && !created.equals(created2)) {
                sql += prefix + Invoice.P_Created + " >= ?";
                args = OAArray.add(Object.class, args, this.created);
                sql += " AND " + prefix + Invoice.P_Created + " <= ?";
                args = OAArray.add(Object.class, args, this.created2);
            }
            else {
                sql += prefix + Invoice.P_Created + " = ?";
                args = OAArray.add(Object.class, args, this.created);
            }
        }
    if (OAString.isNotEmpty(this.customQuery)) {
        if (sql.length() > 0) sql += " AND ";
        sql += "(" + getCustomQuery() + ")";
    }
        if (!useCustomerSearch && getCustomer() != null) {
            if (sql.length() > 0) sql += " AND ";
            sql += prefix + InvoicePP.customer().pp + " = ?";
            args = OAArray.add(Object.class, args, getCustomer());
        }
        if (useCustomerSearch && getCustomerSearch() != null) {
            getCustomerSearch().appendSelect(prefix + InvoicePP.customer().pp, select);
        }
        select.add(sql, args);
    }

    private OAFilter<Invoice> filterDataSourceFilter;
    public OAFilter<Invoice> getDataSourceFilter() {
        if (filterDataSourceFilter != null) return filterDataSourceFilter;
        filterDataSourceFilter = new OAFilter<Invoice>() {
            @Override
            public boolean isUsed(Invoice invoice) {
                return InvoiceSearch.this.isUsedForDataSourceFilter(invoice);
            }
        };
        return filterDataSourceFilter;
    }
    
    private OAFilter<Invoice> filterCustomFilter;
    public OAFilter<Invoice> getCustomFilter() {
        if (filterCustomFilter != null) return filterCustomFilter;
        filterCustomFilter = new OAFilter<Invoice>() {
            @Override
            public boolean isUsed(Invoice invoice) {
                boolean b = InvoiceSearch.this.isUsedForCustomFilter(invoice);
                return b;
            }
        };
        return filterCustomFilter;
    }
    
    public boolean isUsedForDataSourceFilter(Invoice searchInvoice) {
        return true;
    }
    public boolean isUsedForCustomFilter(Invoice searchInvoice) {
        return true;
    }
}
