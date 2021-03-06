// Generated by OABuilder
package com.cdi.model.oa.search;

import java.util.logging.*;
import com.cdi.model.oa.*;
import com.cdi.model.oa.propertypath.*;
import com.viaoa.annotation.*;
import com.viaoa.datasource.*;
import com.viaoa.object.*;
import com.viaoa.hub.*;
import com.viaoa.util.*;
import com.viaoa.filter.OAQueryFilter;
import com.cdi.delegate.ModelDelegate;

@OAClass(useDataSource=false, localOnly=true)
public class SalesOrderItemSearch extends OAObject {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(SalesOrderItemSearch.class.getName());
    public static final String P_Id = "Id";
    public static final String P_SalesOrderItemSalesOrder = "SalesOrderItemSalesOrder";
    public static final String P_UseSalesOrderItemSalesOrderSearch = "UseSalesOrderItemSalesOrderSearch";
    public static final String P_SalesOrderItemServiceCode = "SalesOrderItemServiceCode";
    public static final String P_UseSalesOrderItemServiceCodeSearch = "UseSalesOrderItemServiceCodeSearch";
    public static final String P_SalesOrderItemItem = "SalesOrderItemItem";
    public static final String P_UseSalesOrderItemItemSearch = "UseSalesOrderItemItemSearch";
    public static final String P_MaxResults = "MaxResults";
    public static final String P_SortByType = "SortByType";
    public static final String P_SortByDesc = "SortByDesc";

    protected int id;
    protected SalesOrder salesOrderItemSalesOrder;
    protected boolean useSalesOrderItemSalesOrderSearch;
    protected SalesOrderSearch searchSalesOrderItemSalesOrder;
    protected ServiceCode salesOrderItemServiceCode;
    protected boolean useSalesOrderItemServiceCodeSearch;
    protected ServiceCodeSearch searchSalesOrderItemServiceCode;
    protected Item salesOrderItemItem;
    protected boolean useSalesOrderItemItemSearch;
    protected ItemSearch searchSalesOrderItemItem;
    protected int maxResults;
    protected int sortByType;
    protected boolean sortByDesc;

    public static final int SORTBYTYPE_None = 0;
    public static final int SORTBYTYPE_id = 1;
    public static final int SORTBYTYPE_date = 2;
    public static final int SORTBYTYPE_customerNumber = 3;
    public static final int SORTBYTYPE_name = 4;
    public static final int SORTBYTYPE_code = 5;
    public static final int SORTBYTYPE_name1 = 6;
    public static final Hub<String> hubSortByType;
    static {
        hubSortByType = new Hub<String>(String.class);
        hubSortByType.add("None");
        hubSortByType.add("Id");
        hubSortByType.add("Date");
        hubSortByType.add("Customer Number");
        hubSortByType.add("Name");
        hubSortByType.add("Code");
        hubSortByType.add("Name1");
    }

    public SalesOrderItemSearch() {
        reset();
    }

    public int getId() {
        return id;
    }
    public void setId(int newValue) {
        int old = id;
        fireBeforePropertyChange(P_Id, old, newValue);
        this.id = newValue;
        firePropertyChange(P_Id, old, this.id);
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

    public int getSortByType() {
        return this.sortByType;
    }
    public void setSortByType(int newValue) {
        fireBeforePropertyChange(P_SortByType, this.sortByType, newValue);
        int old = this.sortByType;
        this.sortByType = newValue;
        firePropertyChange(P_SortByType, old, this.sortByType);
    }

    public boolean getSortByDesc() {
        return this.sortByDesc;
    }
    public void setSortByDesc(boolean newValue) {
        fireBeforePropertyChange(P_SortByDesc, this.sortByDesc, newValue);
        boolean old = this.sortByDesc;
        this.sortByDesc = newValue;
        firePropertyChange(P_SortByDesc, old, this.sortByDesc);
    }

    @OAOne
    public SalesOrder getSalesOrderItemSalesOrder() {
        if (salesOrderItemSalesOrder == null) {
            salesOrderItemSalesOrder = (SalesOrder) getObject(P_SalesOrderItemSalesOrder);
        }
        return salesOrderItemSalesOrder;
    }
    public void setSalesOrderItemSalesOrder(SalesOrder newValue) {
        SalesOrder old = this.salesOrderItemSalesOrder;
        this.salesOrderItemSalesOrder = newValue;
        firePropertyChange(P_SalesOrderItemSalesOrder, old, this.salesOrderItemSalesOrder);
    }
    public boolean getUseSalesOrderItemSalesOrderSearch() {
        return useSalesOrderItemSalesOrderSearch;
    }
    public void setUseSalesOrderItemSalesOrderSearch(boolean newValue) {
        boolean old = this.useSalesOrderItemSalesOrderSearch;
        this.useSalesOrderItemSalesOrderSearch = newValue;
        firePropertyChange(P_UseSalesOrderItemSalesOrderSearch, old, this.useSalesOrderItemSalesOrderSearch);
    }
    public SalesOrderSearch getSalesOrderItemSalesOrderSearch() {
        return this.searchSalesOrderItemSalesOrder;
    }
    public void setSalesOrderItemSalesOrderSearch(SalesOrderSearch newValue) {
        this.searchSalesOrderItemSalesOrder = newValue;
    }

    @OAOne
    public ServiceCode getSalesOrderItemServiceCode() {
        if (salesOrderItemServiceCode == null) {
            salesOrderItemServiceCode = (ServiceCode) getObject(P_SalesOrderItemServiceCode);
        }
        return salesOrderItemServiceCode;
    }
    public void setSalesOrderItemServiceCode(ServiceCode newValue) {
        ServiceCode old = this.salesOrderItemServiceCode;
        this.salesOrderItemServiceCode = newValue;
        firePropertyChange(P_SalesOrderItemServiceCode, old, this.salesOrderItemServiceCode);
    }
    public boolean getUseSalesOrderItemServiceCodeSearch() {
        return useSalesOrderItemServiceCodeSearch;
    }
    public void setUseSalesOrderItemServiceCodeSearch(boolean newValue) {
        boolean old = this.useSalesOrderItemServiceCodeSearch;
        this.useSalesOrderItemServiceCodeSearch = newValue;
        firePropertyChange(P_UseSalesOrderItemServiceCodeSearch, old, this.useSalesOrderItemServiceCodeSearch);
    }
    public ServiceCodeSearch getSalesOrderItemServiceCodeSearch() {
        return this.searchSalesOrderItemServiceCode;
    }
    public void setSalesOrderItemServiceCodeSearch(ServiceCodeSearch newValue) {
        this.searchSalesOrderItemServiceCode = newValue;
    }

    @OAOne
    public Item getSalesOrderItemItem() {
        if (salesOrderItemItem == null) {
            salesOrderItemItem = (Item) getObject(P_SalesOrderItemItem);
        }
        return salesOrderItemItem;
    }
    public void setSalesOrderItemItem(Item newValue) {
        Item old = this.salesOrderItemItem;
        this.salesOrderItemItem = newValue;
        firePropertyChange(P_SalesOrderItemItem, old, this.salesOrderItemItem);
    }
    public boolean getUseSalesOrderItemItemSearch() {
        return useSalesOrderItemItemSearch;
    }
    public void setUseSalesOrderItemItemSearch(boolean newValue) {
        boolean old = this.useSalesOrderItemItemSearch;
        this.useSalesOrderItemItemSearch = newValue;
        firePropertyChange(P_UseSalesOrderItemItemSearch, old, this.useSalesOrderItemItemSearch);
    }
    public ItemSearch getSalesOrderItemItemSearch() {
        return this.searchSalesOrderItemItem;
    }
    public void setSalesOrderItemItemSearch(ItemSearch newValue) {
        this.searchSalesOrderItemItem = newValue;
    }

    public void reset() {
        setId(0);
        setNull(P_Id);
        setSalesOrderItemSalesOrder(null);
        setUseSalesOrderItemSalesOrderSearch(false);
        setSalesOrderItemServiceCode(null);
        setUseSalesOrderItemServiceCodeSearch(false);
        setSalesOrderItemItem(null);
        setUseSalesOrderItemItemSearch(false);
        setSortByType(SORTBYTYPE_None);
        setNull(P_SortByType);
        setSortByDesc(false);
        setNull(P_SortByDesc);
    }

    public boolean isDataEntered() {
        if (!isNull(P_Id)) return true;
        if (getSalesOrderItemSalesOrder() != null) return true;
        if (getUseSalesOrderItemSalesOrderSearch()) return true;
        if (getSalesOrderItemServiceCode() != null) return true;
        if (getUseSalesOrderItemServiceCodeSearch()) return true;
        if (getSalesOrderItemItem() != null) return true;
        if (getUseSalesOrderItemItemSearch()) return true;
        return false;
    }

    protected String extraWhere;
    protected Object[] extraWhereParams;
    protected OAFilter<SalesOrderItem> filterExtraWhere;

    public void setExtraWhere(String s, Object ... args) {
        this.extraWhere = s;
        this.extraWhereParams = args;
        if (!OAString.isEmpty(s) && getExtraWhereFilter() == null) {
            OAFilter<SalesOrderItem> f = new OAQueryFilter<SalesOrderItem>(SalesOrderItem.class, s, args);
            setExtraWhereFilter(f);
        }
    }
    public void setExtraWhereFilter(OAFilter<SalesOrderItem> filter) {
        this.filterExtraWhere = filter;
    }
    public OAFilter<SalesOrderItem> getExtraWhereFilter() {
        return this.filterExtraWhere;
    }

    public OASelect<SalesOrderItem> getSelect() {
        String sql = "";
        String sortOrder = null;
        Object[] args = new Object[0];

        switch (getSortByType()) {
            case SORTBYTYPE_None: break;
            case SORTBYTYPE_id: sortOrder = SalesOrderItemPP.salesOrder().id(); break;
            case SORTBYTYPE_date: sortOrder = SalesOrderItemPP.salesOrder().date(); break;
            case SORTBYTYPE_customerNumber: sortOrder = SalesOrderItemPP.salesOrder().salesCustomer().customerNumber(); break;
            case SORTBYTYPE_name: sortOrder = SalesOrderItemPP.salesOrder().salesCustomer().name(); break;
            case SORTBYTYPE_code: sortOrder = SalesOrderItemPP.item().code(); break;
            case SORTBYTYPE_name1: sortOrder = SalesOrderItemPP.item().name(); break;
        }
        if (OAString.isNotEmpty(sortOrder) && getSortByDesc()) sortOrder += " DESC";

        OAFinder finder = null;
        if (!isNull(P_Id)) {
            if (sql.length() > 0) sql += " AND ";
            sql += SalesOrderItem.P_Id + " = ?";
            args = OAArray.add(Object.class, args, this.id);
        }
        if (!useSalesOrderItemSalesOrderSearch && getSalesOrderItemSalesOrder() != null) {
            if (sql.length() > 0) sql += " AND ";
            sql += SalesOrderItemPP.salesOrder().pp + " = ?";
            args = OAArray.add(Object.class, args, getSalesOrderItemSalesOrder());
            finder = new OAFinder<SalesOrder, SalesOrderItem>(getSalesOrderItemSalesOrder(), SalesOrder.P_SalesOrderItems);
        }
        if (!useSalesOrderItemServiceCodeSearch && getSalesOrderItemServiceCode() != null) {
            if (sql.length() > 0) sql += " AND ";
            sql += SalesOrderItemPP.serviceCode().pp + " = ?";
            args = OAArray.add(Object.class, args, getSalesOrderItemServiceCode());
        }
        if (!useSalesOrderItemItemSearch && getSalesOrderItemItem() != null) {
            if (sql.length() > 0) sql += " AND ";
            sql += SalesOrderItemPP.item().pp + " = ?";
            args = OAArray.add(Object.class, args, getSalesOrderItemItem());
        }

        if (!OAString.isEmpty(extraWhere)) {
            if (sql.length() > 0) sql = "(" + sql + ") AND ";
            sql += extraWhere;
            args = OAArray.add(Object.class, args, extraWhereParams);
        }

        OASelect<SalesOrderItem> select = new OASelect<SalesOrderItem>(SalesOrderItem.class, sql, args, sortOrder);
        select.setDataSourceFilter(this.getDataSourceFilter());
        select.setFilter(this.getCustomFilter());
        select.setFinder(finder);
        if (getMaxResults() > 0) select.setMax(getMaxResults());
        if (useSalesOrderItemSalesOrderSearch && getSalesOrderItemSalesOrderSearch() != null) {
            getSalesOrderItemSalesOrderSearch().appendSelect(SalesOrderItemPP.salesOrder().pp, select);
        }
        if (useSalesOrderItemServiceCodeSearch && getSalesOrderItemServiceCodeSearch() != null) {
            getSalesOrderItemServiceCodeSearch().appendSelect(SalesOrderItemPP.serviceCode().pp, select);
        }
        if (useSalesOrderItemItemSearch && getSalesOrderItemItemSearch() != null) {
            getSalesOrderItemItemSearch().appendSelect(SalesOrderItemPP.item().pp, select);
        }
        return select;
    }

    public void appendSelect(final String fromName, final OASelect select) {
        final String prefix = fromName + ".";
        String sql = "";
        Object[] args = new Object[0];
        if (!isNull(P_Id)) {
            if (sql.length() > 0) sql += " AND ";
            sql += SalesOrderItem.P_Id + " = ?";
            args = OAArray.add(Object.class, args, this.id);
        }
        if (!useSalesOrderItemSalesOrderSearch && getSalesOrderItemSalesOrder() != null) {
            if (sql.length() > 0) sql += " AND ";
            sql += prefix + SalesOrderItemPP.salesOrder().pp + " = ?";
            args = OAArray.add(Object.class, args, getSalesOrderItemSalesOrder());
        }
        if (useSalesOrderItemSalesOrderSearch && getSalesOrderItemSalesOrderSearch() != null) {
            getSalesOrderItemSalesOrderSearch().appendSelect(prefix + SalesOrderItemPP.salesOrder().pp, select);
        }
        if (!useSalesOrderItemServiceCodeSearch && getSalesOrderItemServiceCode() != null) {
            if (sql.length() > 0) sql += " AND ";
            sql += prefix + SalesOrderItemPP.serviceCode().pp + " = ?";
            args = OAArray.add(Object.class, args, getSalesOrderItemServiceCode());
        }
        if (useSalesOrderItemServiceCodeSearch && getSalesOrderItemServiceCodeSearch() != null) {
            getSalesOrderItemServiceCodeSearch().appendSelect(prefix + SalesOrderItemPP.serviceCode().pp, select);
        }
        if (!useSalesOrderItemItemSearch && getSalesOrderItemItem() != null) {
            if (sql.length() > 0) sql += " AND ";
            sql += prefix + SalesOrderItemPP.item().pp + " = ?";
            args = OAArray.add(Object.class, args, getSalesOrderItemItem());
        }
        if (useSalesOrderItemItemSearch && getSalesOrderItemItemSearch() != null) {
            getSalesOrderItemItemSearch().appendSelect(prefix + SalesOrderItemPP.item().pp, select);
        }
        select.add(sql, args);
    }

    private OAFilter<SalesOrderItem> filterDataSourceFilter;
    public OAFilter<SalesOrderItem> getDataSourceFilter() {
        if (filterDataSourceFilter != null) return filterDataSourceFilter;
        filterDataSourceFilter = new OAFilter<SalesOrderItem>() {
            @Override
            public boolean isUsed(SalesOrderItem salesOrderItem) {
                return SalesOrderItemSearch.this.isUsedForDataSourceFilter(salesOrderItem);
            }
        };
        return filterDataSourceFilter;
    }
    
    private OAFilter<SalesOrderItem> filterCustomFilter;
    public OAFilter<SalesOrderItem> getCustomFilter() {
        if (filterCustomFilter != null) return filterCustomFilter;
        filterCustomFilter = new OAFilter<SalesOrderItem>() {
            @Override
            public boolean isUsed(SalesOrderItem salesOrderItem) {
                boolean b = SalesOrderItemSearch.this.isUsedForCustomFilter(salesOrderItem);
                if (b && filterExtraWhere != null) b = filterExtraWhere.isUsed(salesOrderItem);
                return b;
            }
        };
        return filterCustomFilter;
    }
    
    public boolean isUsedForDataSourceFilter(SalesOrderItem searchSalesOrderItem) {
        if (!isNull(P_Id)) {
            if (!OACompare.isEqual(searchSalesOrderItem.getId(), id)) return false;
        }
        if (salesOrderItemSalesOrder != null) {
            if (!OACompare.isEqual(searchSalesOrderItem.getSalesOrder(), salesOrderItemSalesOrder)) return false;
        }
        if (salesOrderItemServiceCode != null) {
            if (!OACompare.isEqual(searchSalesOrderItem.getServiceCode(), salesOrderItemServiceCode)) return false;
        }
        if (salesOrderItemItem != null) {
            if (!OACompare.isEqual(searchSalesOrderItem.getItem(), salesOrderItemItem)) return false;
        }
        if (useSalesOrderItemSalesOrderSearch && getSalesOrderItemSalesOrderSearch() != null) {
            SalesOrder salesOrder = (SalesOrder) searchSalesOrderItem.getProperty(SalesOrderItemPP.salesOrder().pp);
            if (salesOrder == null) return false;
            if (!getSalesOrderItemSalesOrderSearch().isUsedForDataSourceFilter(salesOrder)) return false;
        }
        if (useSalesOrderItemServiceCodeSearch && getSalesOrderItemServiceCodeSearch() != null) {
            ServiceCode serviceCode = (ServiceCode) searchSalesOrderItem.getProperty(SalesOrderItemPP.serviceCode().pp);
            if (serviceCode == null) return false;
            if (!getSalesOrderItemServiceCodeSearch().isUsedForDataSourceFilter(serviceCode)) return false;
        }
        if (useSalesOrderItemItemSearch && getSalesOrderItemItemSearch() != null) {
            Item item = (Item) searchSalesOrderItem.getProperty(SalesOrderItemPP.item().pp);
            if (item == null) return false;
            if (!getSalesOrderItemItemSearch().isUsedForDataSourceFilter(item)) return false;
        }
        return true;
    }
    public boolean isUsedForCustomFilter(SalesOrderItem searchSalesOrderItem) {
        return true;
    }
}
