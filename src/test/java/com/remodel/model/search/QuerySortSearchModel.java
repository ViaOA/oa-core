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

public class QuerySortSearchModel {
    private static Logger LOG = Logger.getLogger(QuerySortSearchModel.class.getName());
    
    protected Hub<QuerySort> hub;  // search results
    protected Hub<QuerySort> hubMultiSelect;
    protected Hub<QuerySort> hubSearchFrom;  // hub (optional) to search from
    protected Hub<QuerySortSearch> hubQuerySortSearch;  // search data, size=1, AO
    
    // finder used to find objects in a path
    protected OAFinder<?, QuerySort> finder;
    
    // object used for search data
    protected QuerySortSearch querySortSearch;
    
    public QuerySortSearchModel() {
    }
    
    public QuerySortSearchModel(Hub<QuerySort> hub) {
        this.hub = hub;
    }
    
    // hub used for search results
    public Hub<QuerySort> getHub() {
        if (hub == null) {
            hub = new Hub<QuerySort>(QuerySort.class);
        }
        return hub;
    }
    
    // hub used to search within
    private HubListener hlSearchFromHub;
    public Hub<QuerySort> getSearchFromHub() {
        return hubSearchFrom;
    }
    public void setSearchFromHub(Hub<QuerySort> hub) {
        if (this.hlSearchFromHub != null) {
            hubSearchFrom.removeListener(hlSearchFromHub);
            hlSearchFromHub = null;
        }
    
        hubSearchFrom = hub;
        if (hubSearchFrom != null) {
            hlSearchFromHub = new HubListenerAdapter() {
                @Override
                public void onNewList(HubEvent e) {
                    QuerySortSearchModel.this.getHub().clear();
                }
            };
            hubSearchFrom.addHubListener(hlSearchFromHub);
        }
    }
    public void close() {
        setSearchFromHub(null);
    }
    
    public Hub<QuerySort> getMultiSelectHub() {
        if (hubMultiSelect == null) {
            hubMultiSelect = new Hub<>(QuerySort.class);
        }
        return hubMultiSelect;
    }
    
    public OAFinder<?, QuerySort> getFinder() {
        return finder;
    }
    public void setFinder(OAFinder<?, QuerySort> finder) {
        this.finder = finder;
    }
    
    // object used to input query data, to be used by searchHub
    public QuerySortSearch getQuerySortSearch() {
        if (querySortSearch != null) return querySortSearch;
        querySortSearch = new QuerySortSearch();
        return querySortSearch;
    }
    
    // hub for search object - used to bind with UI components for entering search data
    public Hub<QuerySortSearch> getQuerySortSearchHub() {
        if (hubQuerySortSearch == null) {
            hubQuerySortSearch = new Hub<QuerySortSearch>(QuerySortSearch.class);
            hubQuerySortSearch.add(getQuerySortSearch());
            hubQuerySortSearch.setPos(0);
        }
        return hubQuerySortSearch;
    }
    
    
    
    public void beforeInput() {
        // hook that is called before search input starts
    }
    
    // uses QuerySortSearch to build query, and populate Hub 
    public void performSearch() {
        OASelect<QuerySort> sel = getQuerySortSearch().getSelect();
        sel.setSearchHub(getSearchFromHub());
        sel.setFinder(getFinder());
        getHub().select(sel);
    }
    
    // can to overwritten to know when a selection is made
    public void onSelect(QuerySort querySort, Hub<QuerySort> hub) {
    }
    // can to overwritten to know when a multi-select is made
    public void onSelect(Hub<QuerySort> hub) {
    }
}
