// Generated by OABuilder
package com.corptostore.model.search;

import java.util.logging.*;

import com.viaoa.object.*;
import com.viaoa.hub.*;
import com.viaoa.util.*;
import com.viaoa.filter.*;
import com.corptostore.delegate.ModelDelegate;
import com.corptostore.model.*;
import com.corptostore.model.oa.*;
import com.corptostore.model.oa.filter.*;
import com.corptostore.model.oa.propertypath.*;
import com.corptostore.model.oa.search.*;
import com.corptostore.resource.Resource;
import com.corptostore.model.oa.PurgeWindow;
import com.corptostore.model.oa.search.PurgeWindowSearch;
import com.corptostore.model.search.PurgeWindowSearchModel;
import com.viaoa.datasource.*;

public class PurgeWindowSearchModel {
    private static Logger LOG = Logger.getLogger(PurgeWindowSearchModel.class.getName());
    
    protected Hub<PurgeWindow> hub;  // search results
    protected Hub<PurgeWindow> hubMultiSelect;
    protected Hub<PurgeWindow> hubSearchFrom;  // hub (optional) to search from
    protected Hub<PurgeWindowSearch> hubPurgeWindowSearch;  // search data, size=1, AO
    
    // finder used to find objects in a path
    protected OAFinder<?, PurgeWindow> finder;
    
    // object used for search data
    protected PurgeWindowSearch purgeWindowSearch;
    
    public PurgeWindowSearchModel() {
    }
    
    public PurgeWindowSearchModel(Hub<PurgeWindow> hub) {
        this.hub = hub;
    }
    
    // hub used for search results
    public Hub<PurgeWindow> getHub() {
        if (hub == null) {
            hub = new Hub<PurgeWindow>(PurgeWindow.class);
        }
        return hub;
    }
    
    // hub used to search within
    private HubListener hlSearchFromHub;
    public Hub<PurgeWindow> getSearchFromHub() {
        return hubSearchFrom;
    }
    public void setSearchFromHub(Hub<PurgeWindow> hub) {
        if (this.hlSearchFromHub != null) {
            hubSearchFrom.removeListener(hlSearchFromHub);
            hlSearchFromHub = null;
        }
    
        hubSearchFrom = hub;
        if (hubSearchFrom != null) {
            hlSearchFromHub = new HubListenerAdapter() {
                @Override
                public void onNewList(HubEvent e) {
                    PurgeWindowSearchModel.this.getHub().clear();
                }
            };
            hubSearchFrom.addHubListener(hlSearchFromHub);
        }
    }
    public void close() {
        setSearchFromHub(null);
    }
    
    public Hub<PurgeWindow> getMultiSelectHub() {
        if (hubMultiSelect == null) {
            hubMultiSelect = new Hub<>(PurgeWindow.class);
        }
        return hubMultiSelect;
    }
    
    public OAFinder<?, PurgeWindow> getFinder() {
        return finder;
    }
    public void setFinder(OAFinder<?, PurgeWindow> finder) {
        this.finder = finder;
    }
    
    // object used to input query data, to be used by searchHub
    public PurgeWindowSearch getPurgeWindowSearch() {
        if (purgeWindowSearch != null) return purgeWindowSearch;
        purgeWindowSearch = new PurgeWindowSearch();
        return purgeWindowSearch;
    }
    
    // hub for search object - used to bind with UI components for entering search data
    public Hub<PurgeWindowSearch> getPurgeWindowSearchHub() {
        if (hubPurgeWindowSearch == null) {
            hubPurgeWindowSearch = new Hub<PurgeWindowSearch>(PurgeWindowSearch.class);
            hubPurgeWindowSearch.add(getPurgeWindowSearch());
            hubPurgeWindowSearch.setPos(0);
        }
        return hubPurgeWindowSearch;
    }
    
    
    
    public void beforeInput() {
        // hook that is called before search input starts
    }
    
    // uses PurgeWindowSearch to build query, and populate Hub 
    public void performSearch() {
        OASelect<PurgeWindow> sel = getPurgeWindowSearch().getSelect();
        sel.setSearchHub(getSearchFromHub());
        sel.setFinder(getFinder());
        getHub().select(sel);
    }
    
    // can to overwritten to know when a selection is made
    public void onSelect(PurgeWindow purgeWindow, Hub<PurgeWindow> hub) {
    }
    // can to overwritten to know when a multi-select is made
    public void onSelect(Hub<PurgeWindow> hub) {
    }
}
