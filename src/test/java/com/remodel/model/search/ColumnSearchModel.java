// Generated by OABuilder
package com.remodel.model.search;

import java.util.logging.*;

import com.viaoa.object.*;
import com.viaoa.hub.*;
import com.viaoa.util.*;
import com.viaoa.filter.*;
import com.remodel.delegate.ModelDelegate;
import com.remodel.model.*;
import com.remodel.model.oa.*;
import com.remodel.model.oa.filter.*;
import com.remodel.model.oa.propertypath.*;
import com.remodel.model.oa.search.*;
import com.remodel.resource.Resource;
import com.viaoa.datasource.*;

public class ColumnSearchModel {
    private static Logger LOG = Logger.getLogger(ColumnSearchModel.class.getName());
    
    protected Hub<Column> hub;  // search results
    protected Hub<Column> hubMultiSelect;
    protected Hub<Column> hubSearchFrom;  // hub (optional) to search from
    protected Hub<ColumnSearch> hubColumnSearch;  // search data, size=1, AO
    // references used in search
    protected Hub<Table> hubTable;
    
    // finder used to find objects in a path
    protected OAFinder<?, Column> finder;
    
    // ObjectModels
    protected TableModel modelTable;
    
    // SearchModels
    protected TableSearchModel modelTableSearch;
    
    // object used for search data
    protected ColumnSearch columnSearch;
    
    public ColumnSearchModel() {
    }
    
    public ColumnSearchModel(Hub<Column> hub) {
        this.hub = hub;
    }
    
    // hub used for search results
    public Hub<Column> getHub() {
        if (hub == null) {
            hub = new Hub<Column>(Column.class);
        }
        return hub;
    }
    
    // hub used to search within
    private HubListener hlSearchFromHub;
    public Hub<Column> getSearchFromHub() {
        return hubSearchFrom;
    }
    public void setSearchFromHub(Hub<Column> hub) {
        if (this.hlSearchFromHub != null) {
            hubSearchFrom.removeListener(hlSearchFromHub);
            hlSearchFromHub = null;
        }
    
        hubSearchFrom = hub;
        if (hubSearchFrom != null) {
            hlSearchFromHub = new HubListenerAdapter() {
                @Override
                public void onNewList(HubEvent e) {
                    ColumnSearchModel.this.getHub().clear();
                }
            };
            hubSearchFrom.addHubListener(hlSearchFromHub);
        }
    }
    public void close() {
        setSearchFromHub(null);
    }
    
    public Hub<Column> getMultiSelectHub() {
        if (hubMultiSelect == null) {
            hubMultiSelect = new Hub<>(Column.class);
        }
        return hubMultiSelect;
    }
    
    public OAFinder<?, Column> getFinder() {
        return finder;
    }
    public void setFinder(OAFinder<?, Column> finder) {
        this.finder = finder;
    }
    
    // object used to input query data, to be used by searchHub
    public ColumnSearch getColumnSearch() {
        if (columnSearch != null) return columnSearch;
        columnSearch = new ColumnSearch();
        return columnSearch;
    }
    
    // hub for search object - used to bind with UI components for entering search data
    public Hub<ColumnSearch> getColumnSearchHub() {
        if (hubColumnSearch == null) {
            hubColumnSearch = new Hub<ColumnSearch>(ColumnSearch.class);
            hubColumnSearch.add(getColumnSearch());
            hubColumnSearch.setPos(0);
        }
        return hubColumnSearch;
    }
    public Hub<Table> getTableHub() {
        if (hubTable != null) return hubTable;
        hubTable = getColumnSearchHub().getDetailHub(ColumnSearch.P_Table);
        return hubTable;
    }
    
    public TableModel getTableModel() {
        if (modelTable != null) return modelTable;
        modelTable = new TableModel(getTableHub());
        modelTable.setDisplayName("Table");
        modelTable.setPluralDisplayName("Tables");
        modelTable.setAllowNew(false);
        modelTable.setAllowSave(true);
        modelTable.setAllowAdd(false);
        modelTable.setAllowRemove(false);
        modelTable.setAllowClear(true);
        modelTable.setAllowDelete(false);
        modelTable.setAllowSearch(true);
        modelTable.setAllowHubSearch(false);
        modelTable.setAllowGotoEdit(true);
        return modelTable;
    }
    
    public TableSearchModel getTableSearchModel() {
        if (modelTableSearch == null) {
            modelTableSearch = new TableSearchModel();
            getColumnSearch().setTableSearch(modelTableSearch.getTableSearch());
        }
        return modelTableSearch;
    }
    
    public void beforeInput() {
        // hook that is called before search input starts
    }
    
    // uses ColumnSearch to build query, and populate Hub 
    public void performSearch() {
        OASelect<Column> sel = getColumnSearch().getSelect();
        sel.setSearchHub(getSearchFromHub());
        sel.setFinder(getFinder());
        getHub().select(sel);
    }
    
    // can to overwritten to know when a selection is made
    public void onSelect(Column column, Hub<Column> hub) {
    }
    // can to overwritten to know when a multi-select is made
    public void onSelect(Hub<Column> hub) {
    }
}

