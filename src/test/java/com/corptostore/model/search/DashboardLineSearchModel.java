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
import com.corptostore.model.oa.DashboardLine;
import com.corptostore.model.oa.search.DashboardLineSearch;
import com.corptostore.model.search.DashboardLineSearchModel;
import com.viaoa.datasource.*;

public class DashboardLineSearchModel {
    private static Logger LOG = Logger.getLogger(DashboardLineSearchModel.class.getName());
    
    protected Hub<DashboardLine> hub;  // search results
    protected Hub<DashboardLine> hubMultiSelect;
    protected Hub<DashboardLine> hubSearchFrom;  // hub (optional) to search from
    protected Hub<DashboardLineSearch> hubDashboardLineSearch;  // search data, size=1, AO
    
    // finder used to find objects in a path
    protected OAFinder<?, DashboardLine> finder;
    
    // object used for search data
    protected DashboardLineSearch dashboardLineSearch;
    
    public DashboardLineSearchModel() {
    }
    
    public DashboardLineSearchModel(Hub<DashboardLine> hub) {
        this.hub = hub;
    }
    
    // hub used for search results
    public Hub<DashboardLine> getHub() {
        if (hub == null) {
            hub = new Hub<DashboardLine>(DashboardLine.class);
        }
        return hub;
    }
    
    // hub used to search within
    private HubListener hlSearchFromHub;
    public Hub<DashboardLine> getSearchFromHub() {
        return hubSearchFrom;
    }
    public void setSearchFromHub(Hub<DashboardLine> hub) {
        if (this.hlSearchFromHub != null) {
            hubSearchFrom.removeListener(hlSearchFromHub);
            hlSearchFromHub = null;
        }
    
        hubSearchFrom = hub;
        if (hubSearchFrom != null) {
            hlSearchFromHub = new HubListenerAdapter() {
                @Override
                public void onNewList(HubEvent e) {
                    DashboardLineSearchModel.this.getHub().clear();
                }
            };
            hubSearchFrom.addHubListener(hlSearchFromHub);
        }
    }
    public void close() {
        setSearchFromHub(null);
    }
    
    public Hub<DashboardLine> getMultiSelectHub() {
        if (hubMultiSelect == null) {
            hubMultiSelect = new Hub<>(DashboardLine.class);
        }
        return hubMultiSelect;
    }
    
    public OAFinder<?, DashboardLine> getFinder() {
        return finder;
    }
    public void setFinder(OAFinder<?, DashboardLine> finder) {
        this.finder = finder;
    }
    
    // object used to input query data, to be used by searchHub
    public DashboardLineSearch getDashboardLineSearch() {
        if (dashboardLineSearch != null) return dashboardLineSearch;
        dashboardLineSearch = new DashboardLineSearch();
        return dashboardLineSearch;
    }
    
    // hub for search object - used to bind with UI components for entering search data
    public Hub<DashboardLineSearch> getDashboardLineSearchHub() {
        if (hubDashboardLineSearch == null) {
            hubDashboardLineSearch = new Hub<DashboardLineSearch>(DashboardLineSearch.class);
            hubDashboardLineSearch.add(getDashboardLineSearch());
            hubDashboardLineSearch.setPos(0);
        }
        return hubDashboardLineSearch;
    }
    
    
    
    public void beforeInput() {
        // hook that is called before search input starts
    }
    
    // uses DashboardLineSearch to build query, and populate Hub 
    public void performSearch() {
        OASelect<DashboardLine> sel = getDashboardLineSearch().getSelect();
        sel.setSearchHub(getSearchFromHub());
        sel.setFinder(getFinder());
        getHub().select(sel);
    }
    
    // can to overwritten to know when a selection is made
    public void onSelect(DashboardLine dashboardLine, Hub<DashboardLine> hub) {
    }
    // can to overwritten to know when a multi-select is made
    public void onSelect(Hub<DashboardLine> hub) {
    }
}
