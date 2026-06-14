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
public class ItemSearch extends OAObject {
    private static final long serialVersionUID = 1L;

    private static Logger LOG = Logger.getLogger(ItemSearch.class.getName());

    public static final String P_Name = "Name";
    public static final String P_ItemLine = "ItemLine";
    public static final String P_UseItemLineSearch = "UseItemLineSearch";
    public static final String P_Manufacturer = "Manufacturer";
    public static final String P_UseManufacturerSearch = "UseManufacturerSearch";
    public static final String P_CustomQuery = "CustomQuery";
    public static final String P_MaxResults = "MaxResults";

    protected String name;
    protected ItemLine itemLine;
    protected boolean useItemLineSearch;
    protected ItemLineSearch searchItemLine;
    protected Manufacturer manufacturer;
    protected boolean useManufacturerSearch;
    protected ManufacturerSearch searchManufacturer;
    protected String customQuery;
    protected int maxResults;

    @OAProperty(lowerName = "name", maxLength = 75, displayLength = 22, uiColumnLength = 20)
    public String getName() {
        return name;
    }
    public void setName(String newValue) {
        String old = name;
        fireBeforePropertyChange(P_Name, old, newValue);
        this.name = newValue;
        firePropertyChange(P_Name, old, this.name);
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
    public ItemLine getItemLine() {
        if (itemLine == null) {
            itemLine = (ItemLine) getObject(P_ItemLine);
        }
        return itemLine;
    }
    public void setItemLine(ItemLine newValue) {
        ItemLine old = this.itemLine;
        this.itemLine = newValue;
        firePropertyChange(P_ItemLine, old, this.itemLine);
    }
    public boolean getUseItemLineSearch() {
        return useItemLineSearch;
    }
    public void setUseItemLineSearch(boolean newValue) {
        boolean old = this.useItemLineSearch;
        this.useItemLineSearch = newValue;
        firePropertyChange(P_UseItemLineSearch, old, this.useItemLineSearch);
    }
    public ItemLineSearch getItemLineSearch() {
        return this.searchItemLine;
    }
    public void setItemLineSearch(ItemLineSearch newValue) {
        this.searchItemLine = newValue;
    }

    @OAOne
    public Manufacturer getManufacturer() {
        if (manufacturer == null) {
            manufacturer = (Manufacturer) getObject(P_Manufacturer);
        }
        return manufacturer;
    }
    public void setManufacturer(Manufacturer newValue) {
        Manufacturer old = this.manufacturer;
        this.manufacturer = newValue;
        firePropertyChange(P_Manufacturer, old, this.manufacturer);
    }
    public boolean getUseManufacturerSearch() {
        return useManufacturerSearch;
    }
    public void setUseManufacturerSearch(boolean newValue) {
        boolean old = this.useManufacturerSearch;
        this.useManufacturerSearch = newValue;
        firePropertyChange(P_UseManufacturerSearch, old, this.useManufacturerSearch);
    }
    public ManufacturerSearch getManufacturerSearch() {
        return this.searchManufacturer;
    }
    public void setManufacturerSearch(ManufacturerSearch newValue) {
        this.searchManufacturer = newValue;
    }

    public void reset() {
        setName(null);
        setItemLine(null);
        setUseItemLineSearch(false);
        setManufacturer(null);
        setUseManufacturerSearch(false);
        setCustomQuery(null);
    }

    public boolean isDataEntered() {
        if (getName() != null) return true;
        if (getItemLine() != null) return true;
        if (getUseItemLineSearch()) return true;
        if (getManufacturer() != null) return true;
        if (getUseManufacturerSearch()) return true;
        if (getCustomQuery() != null) return true;
        return false;
    }

    protected String extraWhere;
    protected Object[] extraWhereParams;
    protected OAFilter<Item> filterExtraWhere;

    public void setExtraWhere(String s, Object ... args) {
        this.extraWhere = s;
        this.extraWhereParams = args;
        if (OAString.isNotEmpty(s) && getExtraWhereFilter() == null) {
            OAFilter<Item> f = new OAQueryFilter<Item>(Item.class, s, args);
            setExtraWhereFilter(f);
        }
    }
    public void setExtraWhereFilter(OAFilter<Item> filter) {
        this.filterExtraWhere = filter;
    }
    public OAFilter<Item> getExtraWhereFilter() {
        return this.filterExtraWhere;
    }

    public OASelect<Item> getSelect() {
        final String prefix = "";
        String sql = "";
        String sortOrder = null;
        Object[] args = new Object[0];
        OAFinder finder = null;
        if (OAString.isNotEmpty(this.name)) {
            if (sql.length() > 0) sql += " AND ";
            String val = OAString.convertToLikeSearch(name);
            if (val.indexOf("%") >= 0) {
                sql += Item.P_Name + " LIKE ?";
            }
            else {
                sql += Item.P_Name + " = ?";
            }
            args = OAArray.add(Object.class, args, val);
        }
    if (OAString.isNotEmpty(this.customQuery)) {
        if (sql.length() > 0) sql += " AND ";
        sql += "(" + getCustomQuery() + ")";
    }
        if (!useItemLineSearch && getItemLine() != null) {
            if (sql.length() > 0) sql += " AND ";
            sql += ItemPP.itemLine().pp + " = ?";
            args = OAArray.add(Object.class, args, getItemLine());
            finder = new OAFinder<ItemLine, Item>(getItemLine(), ItemLine.P_Items);
        }
        if (!useManufacturerSearch && getManufacturer() != null) {
            if (sql.length() > 0) sql += " AND ";
            sql += ItemPP.manufacturer().pp + " = ?";
            args = OAArray.add(Object.class, args, getManufacturer());
            finder = new OAFinder<Manufacturer, Item>(getManufacturer(), Manufacturer.P_Items);
        }

        if (OAString.isNotEmpty(extraWhere)) {
            if (sql.length() > 0) sql = "(" + sql + ") AND ";
            sql += extraWhere;
            args = OAArray.add(Object.class, args, extraWhereParams);
        }

        OASelect<Item> select = new OASelect<Item>(Item.class, sql, args, sortOrder);
        if (getExtraWhereFilter() != null && getExtraWhereFilter().updateSelect(select)) {
            select.setFilter(new OAAndFilter(this.getCustomFilter(), getExtraWhereFilter()));
        }
        else select.setFilter(this.getCustomFilter());
        select.setDataSourceFilter(this.getDataSourceFilter());
        select.setFinder(finder);
        if (getMaxResults() > 0) select.setMax(getMaxResults());
        if (useItemLineSearch && getItemLineSearch() != null) {
            getItemLineSearch().appendSelect(ItemPP.itemLine().pp, select);
        }
        if (useManufacturerSearch && getManufacturerSearch() != null) {
            getManufacturerSearch().appendSelect(ItemPP.manufacturer().pp, select);
        }
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
                sql += prefix + Item.P_Name + " LIKE ?";
            }
            else {
                sql += prefix + Item.P_Name + " = ?";
            }
            args = OAArray.add(Object.class, args, val);
        }
    if (OAString.isNotEmpty(this.customQuery)) {
        if (sql.length() > 0) sql += " AND ";
        sql += "(" + getCustomQuery() + ")";
    }
        if (!useItemLineSearch && getItemLine() != null) {
            if (sql.length() > 0) sql += " AND ";
            sql += prefix + ItemPP.itemLine().pp + " = ?";
            args = OAArray.add(Object.class, args, getItemLine());
        }
        if (useItemLineSearch && getItemLineSearch() != null) {
            getItemLineSearch().appendSelect(prefix + ItemPP.itemLine().pp, select);
        }
        if (!useManufacturerSearch && getManufacturer() != null) {
            if (sql.length() > 0) sql += " AND ";
            sql += prefix + ItemPP.manufacturer().pp + " = ?";
            args = OAArray.add(Object.class, args, getManufacturer());
        }
        if (useManufacturerSearch && getManufacturerSearch() != null) {
            getManufacturerSearch().appendSelect(prefix + ItemPP.manufacturer().pp, select);
        }
        select.add(sql, args);
    }

    private OAFilter<Item> filterDataSourceFilter;
    public OAFilter<Item> getDataSourceFilter() {
        if (filterDataSourceFilter != null) return filterDataSourceFilter;
        filterDataSourceFilter = new OAFilter<Item>() {
            @Override
            public boolean isUsed(Item item) {
                return ItemSearch.this.isUsedForDataSourceFilter(item);
            }
        };
        return filterDataSourceFilter;
    }
    
    private OAFilter<Item> filterCustomFilter;
    public OAFilter<Item> getCustomFilter() {
        if (filterCustomFilter != null) return filterCustomFilter;
        filterCustomFilter = new OAFilter<Item>() {
            @Override
            public boolean isUsed(Item item) {
                boolean b = ItemSearch.this.isUsedForCustomFilter(item);
                return b;
            }
        };
        return filterCustomFilter;
    }
    
    public boolean isUsedForDataSourceFilter(Item searchItem) {
        return true;
    }
    public boolean isUsedForCustomFilter(Item searchItem) {
        return true;
    }
}
