// Generated by OABuilder
package com.cdi.model.filter;

import java.util.logging.*;

import com.viaoa.object.*;
import com.viaoa.annotation.*;
import com.viaoa.datasource.*;
import com.viaoa.hub.*;
import com.viaoa.util.*;
import com.cdi.model.*;
import com.cdi.model.oa.*;
import com.cdi.model.oa.propertypath.*;
import com.cdi.model.oa.filter.*;
import com.cdi.model.search.*;
import com.cdi.delegate.ModelDelegate;
import com.cdi.resource.Resource;

public class ItemActiveFilterModel {
    private static Logger LOG = Logger.getLogger(ItemActiveFilterModel.class.getName());
    
    // Hubs
    protected Hub<ItemActiveFilter> hubFilter;
    
    // ObjectModels
    
    // object used for filter data
    protected ItemActiveFilter filter;
    
    public ItemActiveFilterModel(Hub<Item> hubMaster, Hub<Item> hub) {
        filter = new ItemActiveFilter(hubMaster, hub);
    }
    public ItemActiveFilterModel(Hub<Item> hub) {
        filter = new ItemActiveFilter(hub);
    }
    
    // object used to input query data, to be used by filterHub
    public ItemActiveFilter getFilter() {
        return filter;
    }
    
    // hub for filter UI object - used to bind with UI components for entering filter data
    public Hub<ItemActiveFilter> getFilterHub() {
        if (hubFilter == null) {
            hubFilter = new Hub<ItemActiveFilter>(ItemActiveFilter.class);
            hubFilter.add(getFilter());
            hubFilter.setPos(0);
        }
        return hubFilter;
    }
    
    
    
    // get the Filtered hub
    public Hub<Item> getHub() {
        return getFilter().getHubFilter().getHub();
    }
}

