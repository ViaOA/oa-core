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
import com.corptostore.model.filter.SendClosedFilterModel;
import com.corptostore.model.oa.Send;
import com.corptostore.model.oa.filter.SendClosedFilter;
import com.viaoa.annotation.*;
import com.viaoa.hub.*;
import com.viaoa.util.*;
import com.viaoa.datasource.*;

public class SendClosedFilterModel {
    private static Logger LOG = Logger.getLogger(SendClosedFilterModel.class.getName());
    
    // Hubs
    protected Hub<SendClosedFilter> hubFilter;
    
    // ObjectModels
    
    // object used for filter data
    protected SendClosedFilter filter;
    
    public SendClosedFilterModel(Hub<Send> hubMaster, Hub<Send> hub) {
        filter = new SendClosedFilter(hubMaster, hub);
    }
    public SendClosedFilterModel(Hub<Send> hub) {
        filter = new SendClosedFilter(hub);
    }
    
    // object used to input query data, to be used by filterHub
    public SendClosedFilter getFilter() {
        return filter;
    }
    
    // hub for filter UI object - used to bind with UI components for entering filter data
    public Hub<SendClosedFilter> getFilterHub() {
        if (hubFilter == null) {
            hubFilter = new Hub<SendClosedFilter>(SendClosedFilter.class);
            hubFilter.add(getFilter());
            hubFilter.setPos(0);
        }
        return hubFilter;
    }
    
    
    
    // get the Filtered hub
    public Hub<Send> getHub() {
        return getFilter().getHubFilter().getHub();
    }
}
