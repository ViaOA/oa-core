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
import com.corptostore.model.oa.ThreadInfo;
import com.corptostore.model.oa.search.ThreadInfoSearch;
import com.corptostore.model.search.ThreadInfoSearchModel;
import com.viaoa.datasource.*;

public class ThreadInfoSearchModel {
    private static Logger LOG = Logger.getLogger(ThreadInfoSearchModel.class.getName());
    
    protected Hub<ThreadInfo> hub;  // search results
    protected Hub<ThreadInfo> hubMultiSelect;
    protected Hub<ThreadInfo> hubSearchFrom;  // hub (optional) to search from
    protected Hub<ThreadInfoSearch> hubThreadInfoSearch;  // search data, size=1, AO
    
    // finder used to find objects in a path
    protected OAFinder<?, ThreadInfo> finder;
    
    // object used for search data
    protected ThreadInfoSearch threadInfoSearch;
    
    public ThreadInfoSearchModel() {
    }
    
    public ThreadInfoSearchModel(Hub<ThreadInfo> hub) {
        this.hub = hub;
    }
    
    // hub used for search results
    public Hub<ThreadInfo> getHub() {
        if (hub == null) {
            hub = new Hub<ThreadInfo>(ThreadInfo.class);
        }
        return hub;
    }
    
    // hub used to search within
    private HubListener hlSearchFromHub;
    public Hub<ThreadInfo> getSearchFromHub() {
        return hubSearchFrom;
    }
    public void setSearchFromHub(Hub<ThreadInfo> hub) {
        if (this.hlSearchFromHub != null) {
            hubSearchFrom.removeListener(hlSearchFromHub);
            hlSearchFromHub = null;
        }
    
        hubSearchFrom = hub;
        if (hubSearchFrom != null) {
            hlSearchFromHub = new HubListenerAdapter() {
                @Override
                public void onNewList(HubEvent e) {
                    ThreadInfoSearchModel.this.getHub().clear();
                }
            };
            hubSearchFrom.addHubListener(hlSearchFromHub);
        }
    }
    public void close() {
        setSearchFromHub(null);
    }
    
    public Hub<ThreadInfo> getMultiSelectHub() {
        if (hubMultiSelect == null) {
            hubMultiSelect = new Hub<>(ThreadInfo.class);
        }
        return hubMultiSelect;
    }
    
    public OAFinder<?, ThreadInfo> getFinder() {
        return finder;
    }
    public void setFinder(OAFinder<?, ThreadInfo> finder) {
        this.finder = finder;
    }
    
    // object used to input query data, to be used by searchHub
    public ThreadInfoSearch getThreadInfoSearch() {
        if (threadInfoSearch != null) return threadInfoSearch;
        threadInfoSearch = new ThreadInfoSearch();
        return threadInfoSearch;
    }
    
    // hub for search object - used to bind with UI components for entering search data
    public Hub<ThreadInfoSearch> getThreadInfoSearchHub() {
        if (hubThreadInfoSearch == null) {
            hubThreadInfoSearch = new Hub<ThreadInfoSearch>(ThreadInfoSearch.class);
            hubThreadInfoSearch.add(getThreadInfoSearch());
            hubThreadInfoSearch.setPos(0);
        }
        return hubThreadInfoSearch;
    }
    
    
    
    public void beforeInput() {
        // hook that is called before search input starts
    }
    
    // uses ThreadInfoSearch to build query, and populate Hub 
    public void performSearch() {
        OASelect<ThreadInfo> sel = getThreadInfoSearch().getSelect();
        sel.setSearchHub(getSearchFromHub());
        sel.setFinder(getFinder());
        getHub().select(sel);
    }
    
    // can to overwritten to know when a selection is made
    public void onSelect(ThreadInfo threadInfo, Hub<ThreadInfo> hub) {
    }
    // can to overwritten to know when a multi-select is made
    public void onSelect(Hub<ThreadInfo> hub) {
    }
}

