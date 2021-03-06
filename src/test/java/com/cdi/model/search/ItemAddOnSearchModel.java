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

public class ItemAddOnSearchModel {
    private static Logger LOG = Logger.getLogger(ItemAddOnSearchModel.class.getName());
    
    protected Hub<ItemAddOn> hub;  // search results
    protected Hub<ItemAddOn> hubMultiSelect;
    protected Hub<ItemAddOn> hubSearchFrom;  // hub (optional) to search from
    protected Hub<ItemAddOnSearch> hubItemAddOnSearch;  // search data, size=1, AO
    
    // finder used to find objects in a path
    protected OAFinder<?, ItemAddOn> finder;
    
    // object used for search data
    protected ItemAddOnSearch searchItemAddOn;
    
    public ItemAddOnSearchModel() {
    }
    
    public ItemAddOnSearchModel(Hub<ItemAddOn> hub) {
        this.hub = hub;
    }
    
    // hub used for search results
    public Hub<ItemAddOn> getHub() {
        if (hub == null) {
            hub = new Hub<ItemAddOn>(ItemAddOn.class);
        }
        return hub;
    }
    
    // hub used to search within
    private HubListener hlSearchFromHub;
    public Hub<ItemAddOn> getSearchFromHub() {
        return hubSearchFrom;
    }
    public void setSearchFromHub(Hub<ItemAddOn> hub) {
        if (this.hlSearchFromHub != null) {
            hubSearchFrom.removeListener(hlSearchFromHub);
            hlSearchFromHub = null;
        }
    
        hubSearchFrom = hub;
        if (hubSearchFrom != null) {
            hlSearchFromHub = new HubListenerAdapter() {
                @Override
                public void onNewList(HubEvent e) {
                    ItemAddOnSearchModel.this.getHub().clear();
                }
            };
            hubSearchFrom.addHubListener(hlSearchFromHub);
        }
    }
    public void close() {
        setSearchFromHub(null);
    }
    
    public Hub<ItemAddOn> getMultiSelectHub() {
        if (hubMultiSelect == null) {
            hubMultiSelect = new Hub<>(ItemAddOn.class);
        }
        return hubMultiSelect;
    }
    
    public OAFinder<?, ItemAddOn> getFinder() {
        return finder;
    }
    public void setFinder(OAFinder<?, ItemAddOn> finder) {
        this.finder = finder;
    }
    
    // object used to input query data, to be used by searchHub
    public ItemAddOnSearch getItemAddOnSearch() {
        if (searchItemAddOn != null) return searchItemAddOn;
        searchItemAddOn = new ItemAddOnSearch();
        return searchItemAddOn;
    }
    
    // hub for search object - used to bind with UI components for entering search data
    public Hub<ItemAddOnSearch> getItemAddOnSearchHub() {
        if (hubItemAddOnSearch == null) {
            hubItemAddOnSearch = new Hub<ItemAddOnSearch>(ItemAddOnSearch.class);
            hubItemAddOnSearch.add(getItemAddOnSearch());
            hubItemAddOnSearch.setPos(0);
        }
        return hubItemAddOnSearch;
    }
    
    
    
    public void beforeInput() {
        // hook that is called before search input starts
    }
    
    // uses ItemAddOnSearch to build query, and populate Hub 
    public void performSearch() {
        OASelect<ItemAddOn> sel = getItemAddOnSearch().getSelect();
        sel.setSearchHub(getSearchFromHub());
        sel.setFinder(getFinder());
        getHub().select(sel);
    }
    
    // can to overwritten to know when a selection is made
    public void onSelect(ItemAddOn itemAddOn, Hub<ItemAddOn> hub) {
    }
    // can to overwritten to know when a multi-select is made
    public void onSelect(Hub<ItemAddOn> hub) {
    }
}

