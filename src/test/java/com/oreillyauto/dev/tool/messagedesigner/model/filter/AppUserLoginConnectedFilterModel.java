// Generated by OABuilder
package com.oreillyauto.dev.tool.messagedesigner.model.filter;

import java.util.logging.*;

import com.viaoa.object.*;
import com.viaoa.annotation.*;
import com.viaoa.hub.*;
import com.viaoa.util.*;
import com.viaoa.datasource.*;

import com.oreillyauto.dev.tool.messagedesigner.model.*;
import com.oreillyauto.dev.tool.messagedesigner.model.oa.*;
import com.oreillyauto.dev.tool.messagedesigner.model.oa.propertypath.*;
import com.oreillyauto.dev.tool.messagedesigner.model.oa.filter.*;
import com.oreillyauto.dev.tool.messagedesigner.model.search.*;
import com.oreillyauto.dev.tool.messagedesigner.delegate.ModelDelegate;
import com.oreillyauto.dev.tool.messagedesigner.resource.Resource;

public class AppUserLoginConnectedFilterModel {
    private static Logger LOG = Logger.getLogger(AppUserLoginConnectedFilterModel.class.getName());
    
    // Hubs
    protected Hub<AppUserLoginConnectedFilter> hubFilter;
    
    // ObjectModels
    
    // object used for filter data
    protected AppUserLoginConnectedFilter filter;
    
    public AppUserLoginConnectedFilterModel(Hub<AppUserLogin> hubMaster, Hub<AppUserLogin> hub) {
        filter = new AppUserLoginConnectedFilter(hubMaster, hub);
    }
    public AppUserLoginConnectedFilterModel(Hub<AppUserLogin> hub) {
        filter = new AppUserLoginConnectedFilter(hub);
    }
    
    // object used to input query data, to be used by filterHub
    public AppUserLoginConnectedFilter getFilter() {
        return filter;
    }
    
    // hub for filter UI object - used to bind with UI components for entering filter data
    public Hub<AppUserLoginConnectedFilter> getFilterHub() {
        if (hubFilter == null) {
            hubFilter = new Hub<AppUserLoginConnectedFilter>(AppUserLoginConnectedFilter.class);
            hubFilter.add(getFilter());
            hubFilter.setPos(0);
        }
        return hubFilter;
    }
    
    
    
    // get the Filtered hub
    public Hub<AppUserLogin> getHub() {
        return getFilter().getHubFilter().getHub();
    }
}
