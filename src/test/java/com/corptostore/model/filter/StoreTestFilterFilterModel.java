// Generated by OABuilder
package com.corptostore.model.filter;

import java.util.logging.*;

import com.viaoa.object.*;
import com.corptostore.delegate.ModelDelegate;
import com.corptostore.model.*;
import com.corptostore.model.oa.*;
import com.corptostore.model.oa.filter.*;
import com.corptostore.model.oa.propertypath.*;
import com.corptostore.model.search.*;
import com.corptostore.resource.Resource;
import com.corptostore.model.filter.StoreTestFilterFilterModel;
import com.corptostore.model.oa.Store;
import com.corptostore.model.oa.filter.StoreTestFilterFilter;
import com.viaoa.annotation.*;
import com.viaoa.hub.*;
import com.viaoa.util.*;
import com.viaoa.datasource.*;

public class StoreTestFilterFilterModel {
    private static Logger LOG = Logger.getLogger(StoreTestFilterFilterModel.class.getName());
    
    // Hubs
    protected Hub<StoreTestFilterFilter> hubFilter;
    
    // ObjectModels
    
    // object used for filter data
    protected StoreTestFilterFilter filter;
    
    public StoreTestFilterFilterModel(Hub<Store> hubMaster, Hub<Store> hub) {
        filter = new StoreTestFilterFilter(hubMaster, hub);
    }
    public StoreTestFilterFilterModel(Hub<Store> hub) {
        filter = new StoreTestFilterFilter(hub);
    }
    
    // object used to input query data, to be used by filterHub
    public StoreTestFilterFilter getFilter() {
        return filter;
    }
    
    // hub for filter UI object - used to bind with UI components for entering filter data
    public Hub<StoreTestFilterFilter> getFilterHub() {
        if (hubFilter == null) {
            hubFilter = new Hub<StoreTestFilterFilter>(StoreTestFilterFilter.class);
            hubFilter.add(getFilter());
            hubFilter.setPos(0);
        }
        return hubFilter;
    }
    
    
    
    // get the Filtered hub
    public Hub<Store> getHub() {
        return getFilter().getHubFilter().getHub();
    }
}
