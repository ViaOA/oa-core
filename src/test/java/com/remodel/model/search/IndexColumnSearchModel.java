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

public class IndexColumnSearchModel {
    private static Logger LOG = Logger.getLogger(IndexColumnSearchModel.class.getName());
    
    protected Hub<IndexColumn> hub;  // search results
    protected Hub<IndexColumn> hubMultiSelect;
    protected Hub<IndexColumn> hubSearchFrom;  // hub (optional) to search from
    protected Hub<IndexColumnSearch> hubIndexColumnSearch;  // search data, size=1, AO
    // references used in search
    protected Hub<Column> hubColumn;
    
    // finder used to find objects in a path
    protected OAFinder<?, IndexColumn> finder;
    
    // ObjectModels
    protected ColumnModel modelColumn;
    
    // SearchModels
    protected ColumnSearchModel modelColumnSearch;
    
    // object used for search data
    protected IndexColumnSearch indexColumnSearch;
    
    public IndexColumnSearchModel() {
    }
    
    public IndexColumnSearchModel(Hub<IndexColumn> hub) {
        this.hub = hub;
    }
    
    // hub used for search results
    public Hub<IndexColumn> getHub() {
        if (hub == null) {
            hub = new Hub<IndexColumn>(IndexColumn.class);
        }
        return hub;
    }
    
    // hub used to search within
    private HubListener hlSearchFromHub;
    public Hub<IndexColumn> getSearchFromHub() {
        return hubSearchFrom;
    }
    public void setSearchFromHub(Hub<IndexColumn> hub) {
        if (this.hlSearchFromHub != null) {
            hubSearchFrom.removeListener(hlSearchFromHub);
            hlSearchFromHub = null;
        }
    
        hubSearchFrom = hub;
        if (hubSearchFrom != null) {
            hlSearchFromHub = new HubListenerAdapter() {
                @Override
                public void onNewList(HubEvent e) {
                    IndexColumnSearchModel.this.getHub().clear();
                }
            };
            hubSearchFrom.addHubListener(hlSearchFromHub);
        }
    }
    public void close() {
        setSearchFromHub(null);
    }
    
    public Hub<IndexColumn> getMultiSelectHub() {
        if (hubMultiSelect == null) {
            hubMultiSelect = new Hub<>(IndexColumn.class);
        }
        return hubMultiSelect;
    }
    
    public OAFinder<?, IndexColumn> getFinder() {
        return finder;
    }
    public void setFinder(OAFinder<?, IndexColumn> finder) {
        this.finder = finder;
    }
    
    // object used to input query data, to be used by searchHub
    public IndexColumnSearch getIndexColumnSearch() {
        if (indexColumnSearch != null) return indexColumnSearch;
        indexColumnSearch = new IndexColumnSearch();
        return indexColumnSearch;
    }
    
    // hub for search object - used to bind with UI components for entering search data
    public Hub<IndexColumnSearch> getIndexColumnSearchHub() {
        if (hubIndexColumnSearch == null) {
            hubIndexColumnSearch = new Hub<IndexColumnSearch>(IndexColumnSearch.class);
            hubIndexColumnSearch.add(getIndexColumnSearch());
            hubIndexColumnSearch.setPos(0);
        }
        return hubIndexColumnSearch;
    }
    public Hub<Column> getColumnHub() {
        if (hubColumn != null) return hubColumn;
        hubColumn = getIndexColumnSearchHub().getDetailHub(IndexColumnSearch.P_Column);
        return hubColumn;
    }
    
    public ColumnModel getColumnModel() {
        if (modelColumn != null) return modelColumn;
        modelColumn = new ColumnModel(getColumnHub());
        modelColumn.setDisplayName("Column");
        modelColumn.setPluralDisplayName("Columns");
        modelColumn.setAllowNew(false);
        modelColumn.setAllowSave(true);
        modelColumn.setAllowAdd(false);
        modelColumn.setAllowRemove(false);
        modelColumn.setAllowClear(true);
        modelColumn.setAllowDelete(false);
        modelColumn.setAllowSearch(true);
        modelColumn.setAllowHubSearch(false);
        modelColumn.setAllowGotoEdit(true);
        return modelColumn;
    }
    
    public ColumnSearchModel getColumnSearchModel() {
        if (modelColumnSearch == null) {
            modelColumnSearch = new ColumnSearchModel();
            getIndexColumnSearch().setColumnSearch(modelColumnSearch.getColumnSearch());
        }
        return modelColumnSearch;
    }
    
    public void beforeInput() {
        // hook that is called before search input starts
    }
    
    // uses IndexColumnSearch to build query, and populate Hub 
    public void performSearch() {
        OASelect<IndexColumn> sel = getIndexColumnSearch().getSelect();
        sel.setSearchHub(getSearchFromHub());
        sel.setFinder(getFinder());
        getHub().select(sel);
    }
    
    // can to overwritten to know when a selection is made
    public void onSelect(IndexColumn indexColumn, Hub<IndexColumn> hub) {
    }
    // can to overwritten to know when a multi-select is made
    public void onSelect(Hub<IndexColumn> hub) {
    }
}
