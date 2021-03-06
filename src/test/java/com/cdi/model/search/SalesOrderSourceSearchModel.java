// Generated by OABuilder
package com.cdi.model.search;

import java.util.logging.*;

import com.viaoa.object.*;
import com.viaoa.datasource.*;
import com.viaoa.hub.*;
import com.viaoa.util.*;
import com.viaoa.util.filter.*;
import com.cdi.model.*;
import com.cdi.model.oa.*;
import com.cdi.model.oa.propertypath.*;
import com.cdi.model.oa.search.*;
import com.cdi.model.oa.filter.*;
import com.cdi.delegate.ModelDelegate;
import com.cdi.resource.Resource;

public class SalesOrderSourceSearchModel {
    private static Logger LOG = Logger.getLogger(SalesOrderSourceSearchModel.class.getName());
    
    protected Hub<SalesOrderSource> hub;  // search results
    protected Hub<SalesOrderSource> hubMultiSelect;
    protected Hub<SalesOrderSource> hubSearchFrom;  // hub (optional) to search from
    protected Hub<SalesOrderSourceSearch> hubSalesOrderSourceSearch;  // search data, size=1, AO
    
    // finder used to find objects in a path
    protected OAFinder<?, SalesOrderSource> finder;
    
    // object used for search data
    protected SalesOrderSourceSearch searchSalesOrderSource;
    
    public SalesOrderSourceSearchModel() {
    }
    
    public SalesOrderSourceSearchModel(Hub<SalesOrderSource> hub) {
        this.hub = hub;
    }
    
    // hub used for search results
    public Hub<SalesOrderSource> getHub() {
        if (hub == null) {
            hub = new Hub<SalesOrderSource>(SalesOrderSource.class);
        }
        return hub;
    }
    
    // hub used to search within
    private HubListener hlSearchFromHub;
    public Hub<SalesOrderSource> getSearchFromHub() {
        return hubSearchFrom;
    }
    public void setSearchFromHub(Hub<SalesOrderSource> hub) {
        if (this.hlSearchFromHub != null) {
            hubSearchFrom.removeListener(hlSearchFromHub);
            hlSearchFromHub = null;
        }
    
        hubSearchFrom = hub;
        if (hubSearchFrom != null) {
            hlSearchFromHub = new HubListenerAdapter() {
                @Override
                public void onNewList(HubEvent e) {
                    SalesOrderSourceSearchModel.this.getHub().clear();
                }
            };
            hubSearchFrom.addHubListener(hlSearchFromHub);
        }
    }
    public void close() {
        setSearchFromHub(null);
    }
    
    public Hub<SalesOrderSource> getMultiSelectHub() {
        if (hubMultiSelect == null) {
            hubMultiSelect = new Hub<>(SalesOrderSource.class);
        }
        return hubMultiSelect;
    }
    
    public OAFinder<?, SalesOrderSource> getFinder() {
        return finder;
    }
    public void setFinder(OAFinder<?, SalesOrderSource> finder) {
        this.finder = finder;
    }
    
    // object used to input query data, to be used by searchHub
    public SalesOrderSourceSearch getSalesOrderSourceSearch() {
        if (searchSalesOrderSource != null) return searchSalesOrderSource;
        searchSalesOrderSource = new SalesOrderSourceSearch();
        return searchSalesOrderSource;
    }
    
    // hub for search object - used to bind with UI components for entering search data
    public Hub<SalesOrderSourceSearch> getSalesOrderSourceSearchHub() {
        if (hubSalesOrderSourceSearch == null) {
            hubSalesOrderSourceSearch = new Hub<SalesOrderSourceSearch>(SalesOrderSourceSearch.class);
            hubSalesOrderSourceSearch.add(getSalesOrderSourceSearch());
            hubSalesOrderSourceSearch.setPos(0);
        }
        return hubSalesOrderSourceSearch;
    }
    
    
    
    public void beforeInput() {
        // hook that is called before search input starts
    }
    
    // uses SalesOrderSourceSearch to build query, and populate Hub 
    public void performSearch() {
        OASelect<SalesOrderSource> sel = getSalesOrderSourceSearch().getSelect();
        sel.setSearchHub(getSearchFromHub());
        sel.setFinder(getFinder());
        getHub().select(sel);
    }
    
    // can to overwritten to know when a selection is made
    public void onSelect(SalesOrderSource salesOrderSource, Hub<SalesOrderSource> hub) {
    }
    // can to overwritten to know when a multi-select is made
    public void onSelect(Hub<SalesOrderSource> hub) {
    }
}

