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
import com.viaoa.find.OAFinder;

@OAClass(useDataSource=false, localOnly=true)
public class ProductSerialCodeSearch extends OAObject {
    private static final long serialVersionUID = 1L;

    private static Logger LOG = Logger.getLogger(ProductSerialCodeSearch.class.getName());

    public static final String P_SerialCode = "SerialCode";
    public static final String P_ItemPack = "ItemPack";
    public static final String P_UseItemPackSearch = "UseItemPackSearch";
    public static final String P_CustomQuery = "CustomQuery";
    public static final String P_MaxResults = "MaxResults";

    protected String serialCode;
    protected Product itemPack;
    protected boolean useItemPackSearch;
    protected ProductSearch searchItemPack;
    protected String customQuery;
    protected int maxResults;

    @OAProperty(lowerName = "serialCode", displayName = "Serial Code", maxLength = 35, displayLength = 18)
    public String getSerialCode() {
        return serialCode;
    }
    public void setSerialCode(String newValue) {
        String old = serialCode;
        fireBeforePropertyChange(P_SerialCode, old, newValue);
        this.serialCode = newValue;
        firePropertyChange(P_SerialCode, old, this.serialCode);
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
    public Product getItemPack() {
        if (itemPack == null) {
            itemPack = (Product) getObject(P_ItemPack);
        }
        return itemPack;
    }
    public void setItemPack(Product newValue) {
        Product old = this.itemPack;
        this.itemPack = newValue;
        firePropertyChange(P_ItemPack, old, this.itemPack);
    }
    public boolean getUseItemPackSearch() {
        return useItemPackSearch;
    }
    public void setUseItemPackSearch(boolean newValue) {
        boolean old = this.useItemPackSearch;
        this.useItemPackSearch = newValue;
        firePropertyChange(P_UseItemPackSearch, old, this.useItemPackSearch);
    }
    public ProductSearch getItemPackSearch() {
        return this.searchItemPack;
    }
    public void setItemPackSearch(ProductSearch newValue) {
        this.searchItemPack = newValue;
    }

    public void reset() {
        setSerialCode(null);
        setItemPack(null);
        setUseItemPackSearch(false);
        setCustomQuery(null);
    }

    public boolean isDataEntered() {
        if (getSerialCode() != null) return true;
        if (getItemPack() != null) return true;
        if (getUseItemPackSearch()) return true;
        if (getCustomQuery() != null) return true;
        return false;
    }

    protected String extraWhere;
    protected Object[] extraWhereParams;
    protected OAFilter<ProductSerialCode> filterExtraWhere;

    public void setExtraWhere(String s, Object ... args) {
        this.extraWhere = s;
        this.extraWhereParams = args;
        if (OAString.isNotEmpty(s) && getExtraWhereFilter() == null) {
            OAFilter<ProductSerialCode> f = new OAQueryFilter<ProductSerialCode>(ProductSerialCode.class, s, args);
            setExtraWhereFilter(f);
        }
    }
    public void setExtraWhereFilter(OAFilter<ProductSerialCode> filter) {
        this.filterExtraWhere = filter;
    }
    public OAFilter<ProductSerialCode> getExtraWhereFilter() {
        return this.filterExtraWhere;
    }

    public OASelect<ProductSerialCode> getSelect() {
        final String prefix = "";
        String sql = "";
        String sortOrder = null;
        Object[] args = new Object[0];
        OAFinder finder = null;
        if (OAString.isNotEmpty(this.serialCode)) {
            if (sql.length() > 0) sql += " AND ";
            String val = OAString.convertToLikeSearch(serialCode);
            if (val.indexOf("%") >= 0) {
                sql += ProductSerialCode.P_SerialCode + " LIKE ?";
            }
            else {
                sql += ProductSerialCode.P_SerialCode + " = ?";
            }
            args = OAArray.add(Object.class, args, val);
        }
    if (OAString.isNotEmpty(this.customQuery)) {
        if (sql.length() > 0) sql += " AND ";
        sql += "(" + getCustomQuery() + ")";
    }
        if (!useItemPackSearch && getItemPack() != null) {
            if (sql.length() > 0) sql += " AND ";
            sql += ProductSerialCodePP.product().pp + " = ?";
            args = OAArray.add(Object.class, args, getItemPack());
            finder = new OAFinder<Product, ProductSerialCode>(getItemPack(), Product.P_ProductSerialCodes);
        }

        if (OAString.isNotEmpty(extraWhere)) {
            if (sql.length() > 0) sql = "(" + sql + ") AND ";
            sql += extraWhere;
            args = OAArray.add(Object.class, args, extraWhereParams);
        }

        OASelect<ProductSerialCode> select = new OASelect<ProductSerialCode>(ProductSerialCode.class, sql, args, sortOrder);
        if (getExtraWhereFilter() != null && getExtraWhereFilter().updateSelect(select)) {
            select.setFilter(new OAAndFilter(this.getCustomFilter(), getExtraWhereFilter()));
        }
        else select.setFilter(this.getCustomFilter());
        select.setDataSourceFilter(this.getDataSourceFilter());
        select.setFinder(finder);
        if (getMaxResults() > 0) select.setMax(getMaxResults());
        if (useItemPackSearch && getItemPackSearch() != null) {
            getItemPackSearch().appendSelect(ProductSerialCodePP.product().pp, select);
        }
        return select;
    }

    public void appendSelect(final String fromName, final OASelect select) {
        final String prefix = fromName + ".";
        String sql = "";
        Object[] args = new Object[0];
        if (OAString.isNotEmpty(this.serialCode)) {
            if (sql.length() > 0) sql += " AND ";
            String val = OAString.convertToLikeSearch(serialCode);
            if (val.indexOf("%") >= 0) {
                sql += prefix + ProductSerialCode.P_SerialCode + " LIKE ?";
            }
            else {
                sql += prefix + ProductSerialCode.P_SerialCode + " = ?";
            }
            args = OAArray.add(Object.class, args, val);
        }
    if (OAString.isNotEmpty(this.customQuery)) {
        if (sql.length() > 0) sql += " AND ";
        sql += "(" + getCustomQuery() + ")";
    }
        if (!useItemPackSearch && getItemPack() != null) {
            if (sql.length() > 0) sql += " AND ";
            sql += prefix + ProductSerialCodePP.product().pp + " = ?";
            args = OAArray.add(Object.class, args, getItemPack());
        }
        if (useItemPackSearch && getItemPackSearch() != null) {
            getItemPackSearch().appendSelect(prefix + ProductSerialCodePP.product().pp, select);
        }
        select.add(sql, args);
    }

    private OAFilter<ProductSerialCode> filterDataSourceFilter;
    public OAFilter<ProductSerialCode> getDataSourceFilter() {
        if (filterDataSourceFilter != null) return filterDataSourceFilter;
        filterDataSourceFilter = new OAFilter<ProductSerialCode>() {
            @Override
            public boolean isUsed(ProductSerialCode productSerialCode) {
                return ProductSerialCodeSearch.this.isUsedForDataSourceFilter(productSerialCode);
            }
        };
        return filterDataSourceFilter;
    }
    
    private OAFilter<ProductSerialCode> filterCustomFilter;
    public OAFilter<ProductSerialCode> getCustomFilter() {
        if (filterCustomFilter != null) return filterCustomFilter;
        filterCustomFilter = new OAFilter<ProductSerialCode>() {
            @Override
            public boolean isUsed(ProductSerialCode productSerialCode) {
                boolean b = ProductSerialCodeSearch.this.isUsedForCustomFilter(productSerialCode);
                return b;
            }
        };
        return filterCustomFilter;
    }
    
    public boolean isUsedForDataSourceFilter(ProductSerialCode searchProductSerialCode) {
        return true;
    }
    public boolean isUsedForCustomFilter(ProductSerialCode searchProductSerialCode) {
        return true;
    }
}
