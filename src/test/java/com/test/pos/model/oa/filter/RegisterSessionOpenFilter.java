package com.test.pos.model.oa.filter;

import java.util.*;
import java.util.logging.*;
import com.test.pos.model.oa.*;
import com.test.pos.model.oa.propertypath.*;
import com.viaoa.annotation.*;
import com.viaoa.object.*;
import com.viaoa.hub.*;
import com.viaoa.hub.filter.*;
import com.test.pos.model.oa.search.*;
import com.viaoa.cache.OAObjectCacheFilter;

@OAClass(useDataSource=false, localOnly=true)
@OAClassFilter(name = "Open", displayName = "Open", hasInputParams = false)
public class RegisterSessionOpenFilter extends OAObject implements CustomHubFilter<RegisterSession> {
    private static final long serialVersionUID = 1L;

    private static Logger LOG = Logger.getLogger(RegisterSessionOpenFilter.class.getName());


    public static final String PPCode = ":Open()";
    private Hub<RegisterSession> hubMaster;
    private Hub<RegisterSession> hub;
    private HubFilter<RegisterSession> hubFilter;
    private OAObjectCacheFilter<RegisterSession> cacheFilter;
    private boolean bUseObjectCache;

    public RegisterSessionOpenFilter() {
        this(null, null, false);
    }
    public RegisterSessionOpenFilter(Hub<RegisterSession> hub) {
        this(null, hub, true);
    }
    public RegisterSessionOpenFilter(Hub<RegisterSession> hubMaster, Hub<RegisterSession> hub) {
        this(hubMaster, hub, false);
    }
    public RegisterSessionOpenFilter(Hub<RegisterSession> hubMaster, Hub<RegisterSession> hubFiltered, boolean bUseObjectCache) {
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
    public HubFilter<RegisterSession> getHubFilter() {
        if (hubFilter != null) return hubFilter;
        if (hubMaster == null) return null;
        hubFilter = new HubFilter<RegisterSession>(hubMaster, hub) {
            @Override
            public boolean isUsed(RegisterSession registerSession) {
                return RegisterSessionOpenFilter.this.isUsed(registerSession);
            }
        };
        hubFilter.addDependentProperty(RegisterSessionPP.ended(), false);
        hubFilter.refresh();
        return hubFilter;
    }

    public OAObjectCacheFilter<RegisterSession> getObjectCacheFilter() {
        if (cacheFilter != null) return cacheFilter;
        if (!bUseObjectCache) return null;
        hub.onBeforeRefresh(e -> reselect());
        cacheFilter = new OAObjectCacheFilter<RegisterSession>(hub) {
            @Override
            public boolean isUsed(RegisterSession registerSession) {
                return RegisterSessionOpenFilter.this.isUsed(registerSession);
            }
            @Override
            protected void reselect() {
                RegisterSessionOpenFilter.this.reselect();
            }
        };
        cacheFilter.addDependentProperty(RegisterSessionPP.ended(), false);
        cacheFilter.refresh();
        return cacheFilter;
    }

    public void reselect() {
        // can be overwritten to query datasource
    }

    // ==================
    // this method has custom code that will need to be put into the OABuilder filter

    @Override
    public boolean isUsed(RegisterSession registerSession) {
        return (registerSession.getEnded() == null);
    }
}
