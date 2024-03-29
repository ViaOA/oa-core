// Generated by OABuilder
package com.messagedesigner.model.oa.filter;

import java.util.logging.*;

import com.messagedesigner.delegate.oa.MessageTypeDelegate;
import com.messagedesigner.model.oa.*;
import com.messagedesigner.model.oa.propertypath.*;
import com.messagedesigner.model.oa.search.*;
import com.messagedesigner.model.search.*;
import com.messagedesigner.model.oa.MessageType;
import com.messagedesigner.model.oa.filter.MessageTypeNotVerifiedFilter;
import com.messagedesigner.model.oa.propertypath.MessageTypePP;
import com.viaoa.annotation.*;
import com.viaoa.object.*;
import com.viaoa.hub.*;
import com.viaoa.util.*;
import java.util.*;

@OAClass(useDataSource=false, localOnly=true)
@OAClassFilter(name = "NotVerified", displayName = "Not Verified", hasInputParams = false)
public class MessageTypeNotVerifiedFilter extends OAObject implements CustomHubFilter<MessageType> {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(MessageTypeNotVerifiedFilter.class.getName());

    public static final String PPCode = ":NotVerified()";
    private Hub<MessageType> hubMaster;
    private Hub<MessageType> hub;
    private HubFilter<MessageType> hubFilter;
    private OAObjectCacheFilter<MessageType> cacheFilter;
    private boolean bUseObjectCache;

    public MessageTypeNotVerifiedFilter() {
        this(null, null, false);
    }
    public MessageTypeNotVerifiedFilter(Hub<MessageType> hub) {
        this(null, hub, true);
    }
    public MessageTypeNotVerifiedFilter(Hub<MessageType> hubMaster, Hub<MessageType> hub) {
        this(hubMaster, hub, false);
    }
    public MessageTypeNotVerifiedFilter(Hub<MessageType> hubMaster, Hub<MessageType> hubFiltered, boolean bUseObjectCache) {
        this.hubMaster = hubMaster;
        this.hub = hubFiltered;
        this.bUseObjectCache = bUseObjectCache;
        if (hubMaster != null) getHubFilter();
        if (bUseObjectCache) getObjectCacheFilter();
    }


    public void reset() {
    }

    public boolean isDataEntered() {
        return false;
    }
    public void refresh() {
        if (hubFilter != null) getHubFilter().refresh();
        if (cacheFilter != null) getObjectCacheFilter().refresh();
    }

    @Override
    public HubFilter<MessageType> getHubFilter() {
        if (hubFilter != null) return hubFilter;
        if (hubMaster == null) return null;
        hubFilter = new HubFilter<MessageType>(hubMaster, hub) {
            @Override
            public boolean isUsed(MessageType messageType) {
                return MessageTypeNotVerifiedFilter.this.isUsed(messageType);
            }
        };
        hubFilter.addDependentProperty(MessageTypePP.verified(), false);
        hubFilter.refresh();
        return hubFilter;
    }

    public OAObjectCacheFilter<MessageType> getObjectCacheFilter() {
        if (cacheFilter != null) return cacheFilter;
        if (!bUseObjectCache) return null;
        cacheFilter = new OAObjectCacheFilter<MessageType>(hub) {
            @Override
            public boolean isUsed(MessageType messageType) {
                return MessageTypeNotVerifiedFilter.this.isUsed(messageType);
            }
            @Override
            protected void reselect() {
                MessageTypeNotVerifiedFilter.this.reselect();
            }
        };
        cacheFilter.addDependentProperty(MessageTypePP.verified(), false);
        cacheFilter.refresh();
        return cacheFilter;
    }

    public void reselect() {
        // can be overwritten to query datasource
    }

    // ==================
    // this method has custom code that will need to be put into the OABuilder filter

    @Override
   public boolean isUsed(MessageType messageType) {
        // custom code here needs to be put in OABuilder
        return true;
   }
}
