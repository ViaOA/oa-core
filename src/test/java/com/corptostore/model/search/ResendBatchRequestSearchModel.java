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
import com.corptostore.model.oa.ResendBatchRequest;
import com.corptostore.model.oa.search.ResendBatchRequestSearch;
import com.corptostore.model.search.ResendBatchRequestSearchModel;
import com.viaoa.datasource.*;

public class ResendBatchRequestSearchModel {
    private static Logger LOG = Logger.getLogger(ResendBatchRequestSearchModel.class.getName());
    
    protected Hub<ResendBatchRequest> hub;  // search results
    protected Hub<ResendBatchRequest> hubMultiSelect;
    protected Hub<ResendBatchRequest> hubSearchFrom;  // hub (optional) to search from
    protected Hub<ResendBatchRequestSearch> hubResendBatchRequestSearch;  // search data, size=1, AO
    
    // finder used to find objects in a path
    protected OAFinder<?, ResendBatchRequest> finder;
    
    // object used for search data
    protected ResendBatchRequestSearch resendBatchRequestSearch;
    
    public ResendBatchRequestSearchModel() {
    }
    
    public ResendBatchRequestSearchModel(Hub<ResendBatchRequest> hub) {
        this.hub = hub;
    }
    
    // hub used for search results
    public Hub<ResendBatchRequest> getHub() {
        if (hub == null) {
            hub = new Hub<ResendBatchRequest>(ResendBatchRequest.class);
        }
        return hub;
    }
    
    // hub used to search within
    private HubListener hlSearchFromHub;
    public Hub<ResendBatchRequest> getSearchFromHub() {
        return hubSearchFrom;
    }
    public void setSearchFromHub(Hub<ResendBatchRequest> hub) {
        if (this.hlSearchFromHub != null) {
            hubSearchFrom.removeListener(hlSearchFromHub);
            hlSearchFromHub = null;
        }
    
        hubSearchFrom = hub;
        if (hubSearchFrom != null) {
            hlSearchFromHub = new HubListenerAdapter() {
                @Override
                public void onNewList(HubEvent e) {
                    ResendBatchRequestSearchModel.this.getHub().clear();
                }
            };
            hubSearchFrom.addHubListener(hlSearchFromHub);
        }
    }
    public void close() {
        setSearchFromHub(null);
    }
    
    public Hub<ResendBatchRequest> getMultiSelectHub() {
        if (hubMultiSelect == null) {
            hubMultiSelect = new Hub<>(ResendBatchRequest.class);
        }
        return hubMultiSelect;
    }
    
    public OAFinder<?, ResendBatchRequest> getFinder() {
        return finder;
    }
    public void setFinder(OAFinder<?, ResendBatchRequest> finder) {
        this.finder = finder;
    }
    
    // object used to input query data, to be used by searchHub
    public ResendBatchRequestSearch getResendBatchRequestSearch() {
        if (resendBatchRequestSearch != null) return resendBatchRequestSearch;
        resendBatchRequestSearch = new ResendBatchRequestSearch();
        return resendBatchRequestSearch;
    }
    
    // hub for search object - used to bind with UI components for entering search data
    public Hub<ResendBatchRequestSearch> getResendBatchRequestSearchHub() {
        if (hubResendBatchRequestSearch == null) {
            hubResendBatchRequestSearch = new Hub<ResendBatchRequestSearch>(ResendBatchRequestSearch.class);
            hubResendBatchRequestSearch.add(getResendBatchRequestSearch());
            hubResendBatchRequestSearch.setPos(0);
        }
        return hubResendBatchRequestSearch;
    }
    
    
    
    public void beforeInput() {
        // hook that is called before search input starts
    }
    
    // uses ResendBatchRequestSearch to build query, and populate Hub 
    public void performSearch() {
        OASelect<ResendBatchRequest> sel = getResendBatchRequestSearch().getSelect();
        sel.setSearchHub(getSearchFromHub());
        sel.setFinder(getFinder());
        getHub().select(sel);
    }
    
    // can to overwritten to know when a selection is made
    public void onSelect(ResendBatchRequest resendBatchRequest, Hub<ResendBatchRequest> hub) {
    }
    // can to overwritten to know when a multi-select is made
    public void onSelect(Hub<ResendBatchRequest> hub) {
    }
}
