// Generated by OABuilder
package com.oreillyauto.remodel.model.search;

import java.util.logging.*;

import com.viaoa.object.*;
import com.viaoa.hub.*;
import com.viaoa.util.*;
import com.viaoa.filter.*;
import com.viaoa.datasource.*;

import com.oreillyauto.remodel.model.*;
import com.oreillyauto.remodel.model.oa.*;
import com.oreillyauto.remodel.model.oa.propertypath.*;
import com.oreillyauto.remodel.model.oa.search.*;
import com.oreillyauto.remodel.model.oa.filter.*;
import com.oreillyauto.remodel.delegate.ModelDelegate;
import com.oreillyauto.remodel.resource.Resource;

public class SqlTypeSearchModel {
    private static Logger LOG = Logger.getLogger(SqlTypeSearchModel.class.getName());
    
    protected Hub<SqlType> hub;  // search results
    protected Hub<SqlType> hubMultiSelect;
    protected Hub<SqlType> hubSearchFrom;  // hub (optional) to search from
    protected Hub<SqlTypeSearch> hubSqlTypeSearch;  // search data, size=1, AO
    // references used in search
    protected Hub<DataType> hubDataType;
    
    // finder used to find objects in a path
    protected OAFinder<?, SqlType> finder;
    
    // ObjectModels
    protected DataTypeModel modelDataType;
    
    // SearchModels
    protected DataTypeSearchModel modelDataTypeSearch;
    
    // object used for search data
    protected SqlTypeSearch sqlTypeSearch;
    
    public SqlTypeSearchModel() {
    }
    
    public SqlTypeSearchModel(Hub<SqlType> hub) {
        this.hub = hub;
    }
    
    // hub used for search results
    public Hub<SqlType> getHub() {
        if (hub == null) {
            hub = new Hub<SqlType>(SqlType.class);
        }
        return hub;
    }
    
    // hub used to search within
    private HubListener hlSearchFromHub;
    public Hub<SqlType> getSearchFromHub() {
        return hubSearchFrom;
    }
    public void setSearchFromHub(Hub<SqlType> hub) {
        if (this.hlSearchFromHub != null) {
            hubSearchFrom.removeListener(hlSearchFromHub);
            hlSearchFromHub = null;
        }
    
        hubSearchFrom = hub;
        if (hubSearchFrom != null) {
            hlSearchFromHub = new HubListenerAdapter() {
                @Override
                public void onNewList(HubEvent e) {
                    SqlTypeSearchModel.this.getHub().clear();
                }
            };
            hubSearchFrom.addHubListener(hlSearchFromHub);
        }
    }
    public void close() {
        setSearchFromHub(null);
    }
    
    public Hub<SqlType> getMultiSelectHub() {
        if (hubMultiSelect == null) {
            hubMultiSelect = new Hub<>(SqlType.class);
        }
        return hubMultiSelect;
    }
    
    public OAFinder<?, SqlType> getFinder() {
        return finder;
    }
    public void setFinder(OAFinder<?, SqlType> finder) {
        this.finder = finder;
    }
    
    // object used to input query data, to be used by searchHub
    public SqlTypeSearch getSqlTypeSearch() {
        if (sqlTypeSearch != null) return sqlTypeSearch;
        sqlTypeSearch = new SqlTypeSearch();
        return sqlTypeSearch;
    }
    
    // hub for search object - used to bind with UI components for entering search data
    public Hub<SqlTypeSearch> getSqlTypeSearchHub() {
        if (hubSqlTypeSearch == null) {
            hubSqlTypeSearch = new Hub<SqlTypeSearch>(SqlTypeSearch.class);
            hubSqlTypeSearch.add(getSqlTypeSearch());
            hubSqlTypeSearch.setPos(0);
        }
        return hubSqlTypeSearch;
    }
    public Hub<DataType> getDataTypeHub() {
        if (hubDataType != null) return hubDataType;
        hubDataType = new Hub<>(DataType.class);
        Hub<DataType> hub = ModelDelegate.getDataTypes();
        HubCopy<DataType> hc = new HubCopy<>(hub, hubDataType, false);
        hubDataType.setLinkHub(getSqlTypeSearchHub(), SqlTypeSearch.P_DataType); 
        return hubDataType;
    }
    
    public DataTypeModel getDataTypeModel() {
        if (modelDataType != null) return modelDataType;
        modelDataType = new DataTypeModel(getDataTypeHub());
        modelDataType.setDisplayName("Data Type");
        modelDataType.setPluralDisplayName("Data Types");
        modelDataType.setAllowNew(false);
        modelDataType.setAllowSave(true);
        modelDataType.setAllowAdd(false);
        modelDataType.setAllowRemove(false);
        modelDataType.setAllowClear(true);
        modelDataType.setAllowDelete(false);
        modelDataType.setAllowSearch(true);
        modelDataType.setAllowHubSearch(false);
        modelDataType.setAllowGotoEdit(true);
        modelDataType.setViewOnly(true);
        return modelDataType;
    }
    
    public DataTypeSearchModel getDataTypeSearchModel() {
        if (modelDataTypeSearch == null) {
            modelDataTypeSearch = new DataTypeSearchModel();
            getSqlTypeSearch().setDataTypeSearch(modelDataTypeSearch.getDataTypeSearch());
        }
        return modelDataTypeSearch;
    }
    
    public void beforeInput() {
        // hook that is called before search input starts
    }
    
    // uses SqlTypeSearch to build query, and populate Hub 
    public void performSearch() {
        OASelect<SqlType> sel = getSqlTypeSearch().getSelect();
        sel.setSearchHub(getSearchFromHub());
        sel.setFinder(getFinder());
        getHub().select(sel);
    }
    
    // can to overwritten to know when a selection is made
    public void onSelect(SqlType sqlType, Hub<SqlType> hub) {
    }
    // can to overwritten to know when a multi-select is made
    public void onSelect(Hub<SqlType> hub) {
    }
}
