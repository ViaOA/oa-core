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
public class ProductSearch extends OAObject {
    private static final long serialVersionUID = 1L;

    private static Logger LOG = Logger.getLogger(ProductSearch.class.getName());

    public static final String P_ItemVariant = "ItemVariant";
    public static final String P_UseItemVariantSearch = "UseItemVariantSearch";
    public static final String P_ItemPack = "ItemPack";
    public static final String P_UseItemPackSearch = "UseItemPackSearch";
    public static final String P_CustomQuery = "CustomQuery";
    public static final String P_MaxResults = "MaxResults";

    protected ItemVariant itemVariant;
    protected boolean useItemVariantSearch;
    protected ItemVariantSearch searchItemVariant;
    protected ItemPack itemPack;
    protected boolean useItemPackSearch;
    protected ItemPackSearch searchItemPack;
    protected String customQuery;
    protected int maxResults;


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
    public ItemVariant getItemVariant() {
        if (itemVariant == null) {
            itemVariant = (ItemVariant) getObject(P_ItemVariant);
        }
        return itemVariant;
    }
    public void setItemVariant(ItemVariant newValue) {
        ItemVariant old = this.itemVariant;
        this.itemVariant = newValue;
        firePropertyChange(P_ItemVariant, old, this.itemVariant);
    }
    public boolean getUseItemVariantSearch() {
        return useItemVariantSearch;
    }
    public void setUseItemVariantSearch(boolean newValue) {
        boolean old = this.useItemVariantSearch;
        this.useItemVariantSearch = newValue;
        firePropertyChange(P_UseItemVariantSearch, old, this.useItemVariantSearch);
    }
    public ItemVariantSearch getItemVariantSearch() {
        return this.searchItemVariant;
    }
    public void setItemVariantSearch(ItemVariantSearch newValue) {
        this.searchItemVariant = newValue;
    }

    @OAOne
    public ItemPack getItemPack() {
        if (itemPack == null) {
            itemPack = (ItemPack) getObject(P_ItemPack);
        }
        return itemPack;
    }
    public void setItemPack(ItemPack newValue) {
        ItemPack old = this.itemPack;
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
    public ItemPackSearch getItemPackSearch() {
        return this.searchItemPack;
    }
    public void setItemPackSearch(ItemPackSearch newValue) {
        this.searchItemPack = newValue;
    }

    public void reset() {
        setItemVariant(null);
        setUseItemVariantSearch(false);
        setItemPack(null);
        setUseItemPackSearch(false);
        setCustomQuery(null);
    }

    public boolean isDataEntered() {
        if (getItemVariant() != null) return true;
        if (getUseItemVariantSearch()) return true;
        if (getItemPack() != null) return true;
        if (getUseItemPackSearch()) return true;
        if (getCustomQuery() != null) return true;
        return false;
    }

    protected String extraWhere;
    protected Object[] extraWhereParams;
    protected OAFilter<Product> filterExtraWhere;

    public void setExtraWhere(String s, Object ... args) {
        this.extraWhere = s;
        this.extraWhereParams = args;
        if (OAString.isNotEmpty(s) && getExtraWhereFilter() == null) {
            OAFilter<Product> f = new OAQueryFilter<Product>(Product.class, s, args);
            setExtraWhereFilter(f);
        }
    }
    public void setExtraWhereFilter(OAFilter<Product> filter) {
        this.filterExtraWhere = filter;
    }
    public OAFilter<Product> getExtraWhereFilter() {
        return this.filterExtraWhere;
    }

    public OASelect<Product> getSelect() {
        final String prefix = "";
        String sql = "";
        String sortOrder = null;
        Object[] args = new Object[0];
        OAFinder finder = null;
    if (OAString.isNotEmpty(this.customQuery)) {
        if (sql.length() > 0) sql += " AND ";
        sql += "(" + getCustomQuery() + ")";
    }
        if (!useItemVariantSearch && getItemVariant() != null) {
            if (sql.length() > 0) sql += " AND ";
            sql += ProductPP.itemVariant().pp + " = ?";
            args = OAArray.add(Object.class, args, getItemVariant());
            finder = new OAFinder<ItemVariant, Product>(getItemVariant(), ItemVariant.P_Products);
        }
        if (!useItemPackSearch && getItemPack() != null) {
            if (sql.length() > 0) sql += " AND ";
            sql += ProductPP.itemPack().pp + " = ?";
            args = OAArray.add(Object.class, args, getItemPack());
            finder = new OAFinder<ItemPack, Product>(getItemPack(), ItemPack.P_Products);
        }

        if (OAString.isNotEmpty(extraWhere)) {
            if (sql.length() > 0) sql = "(" + sql + ") AND ";
            sql += extraWhere;
            args = OAArray.add(Object.class, args, extraWhereParams);
        }

        OASelect<Product> select = new OASelect<Product>(Product.class, sql, args, sortOrder);
        if (getExtraWhereFilter() != null && getExtraWhereFilter().updateSelect(select)) {
            select.setFilter(new OAAndFilter(this.getCustomFilter(), getExtraWhereFilter()));
        }
        else select.setFilter(this.getCustomFilter());
        select.setDataSourceFilter(this.getDataSourceFilter());
        select.setFinder(finder);
        if (getMaxResults() > 0) select.setMax(getMaxResults());
        if (useItemVariantSearch && getItemVariantSearch() != null) {
            getItemVariantSearch().appendSelect(ProductPP.itemVariant().pp, select);
        }
        if (useItemPackSearch && getItemPackSearch() != null) {
            getItemPackSearch().appendSelect(ProductPP.itemPack().pp, select);
        }
        return select;
    }

    public void appendSelect(final String fromName, final OASelect select) {
        final String prefix = fromName + ".";
        String sql = "";
        Object[] args = new Object[0];
    if (OAString.isNotEmpty(this.customQuery)) {
        if (sql.length() > 0) sql += " AND ";
        sql += "(" + getCustomQuery() + ")";
    }
        if (!useItemVariantSearch && getItemVariant() != null) {
            if (sql.length() > 0) sql += " AND ";
            sql += prefix + ProductPP.itemVariant().pp + " = ?";
            args = OAArray.add(Object.class, args, getItemVariant());
        }
        if (useItemVariantSearch && getItemVariantSearch() != null) {
            getItemVariantSearch().appendSelect(prefix + ProductPP.itemVariant().pp, select);
        }
        if (!useItemPackSearch && getItemPack() != null) {
            if (sql.length() > 0) sql += " AND ";
            sql += prefix + ProductPP.itemPack().pp + " = ?";
            args = OAArray.add(Object.class, args, getItemPack());
        }
        if (useItemPackSearch && getItemPackSearch() != null) {
            getItemPackSearch().appendSelect(prefix + ProductPP.itemPack().pp, select);
        }
        select.add(sql, args);
    }

    private OAFilter<Product> filterDataSourceFilter;
    public OAFilter<Product> getDataSourceFilter() {
        if (filterDataSourceFilter != null) return filterDataSourceFilter;
        filterDataSourceFilter = new OAFilter<Product>() {
            @Override
            public boolean isUsed(Product product) {
                return ProductSearch.this.isUsedForDataSourceFilter(product);
            }
        };
        return filterDataSourceFilter;
    }
    
    private OAFilter<Product> filterCustomFilter;
    public OAFilter<Product> getCustomFilter() {
        if (filterCustomFilter != null) return filterCustomFilter;
        filterCustomFilter = new OAFilter<Product>() {
            @Override
            public boolean isUsed(Product product) {
                boolean b = ProductSearch.this.isUsedForCustomFilter(product);
                return b;
            }
        };
        return filterCustomFilter;
    }
    
    public boolean isUsedForDataSourceFilter(Product searchProduct) {
        return true;
    }
    public boolean isUsedForCustomFilter(Product searchProduct) {
        return true;
    }
}
