package com.test.pos.model.oa.filter;

import java.util.logging.*;
import com.test.pos.model.oa.*;
import com.test.pos.model.oa.propertypath.*;
import com.viaoa.annotation.*;
import com.viaoa.object.*;
import com.viaoa.hub.*;
import com.viaoa.hub.filter.*;
import com.viaoa.cache.OAObjectCacheFilter;

@OAClass(useDataSource=false, localOnly=true)
@OAClassFilter(name = "Active", displayName = "Active", hasInputParams = false)
public class TeamMemberActiveFilter extends OAObject implements CustomHubFilter<TeamMember> {
    private static final long serialVersionUID = 1L;

    private static Logger LOG = Logger.getLogger(TeamMemberActiveFilter.class.getName());


    public static final String PPCode = ":Active()";
    private Hub<TeamMember> hubMaster;
    private Hub<TeamMember> hub;
    private HubFilter<TeamMember> hubFilter;
    private OAObjectCacheFilter<TeamMember> cacheFilter;
    private boolean bUseObjectCache;

    public TeamMemberActiveFilter() {
        this(null, null, false);
    }
    public TeamMemberActiveFilter(Hub<TeamMember> hub) {
        this(null, hub, true);
    }
    public TeamMemberActiveFilter(Hub<TeamMember> hubMaster, Hub<TeamMember> hub) {
        this(hubMaster, hub, false);
    }
    public TeamMemberActiveFilter(Hub<TeamMember> hubMaster, Hub<TeamMember> hubFiltered, boolean bUseObjectCache) {
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
    public HubFilter<TeamMember> getHubFilter() {
        if (hubFilter != null) return hubFilter;
        if (hubMaster == null) return null;
        hubFilter = new HubFilter<TeamMember>(hubMaster, hub) {
            @Override
            public boolean isUsed(TeamMember teamMember) {
                return TeamMemberActiveFilter.this.isUsed(teamMember);
            }
        };
        hubFilter.addDependentProperty(TeamMemberPP.inactiveDate(), false);
        hubFilter.refresh();
        return hubFilter;
    }

    public OAObjectCacheFilter<TeamMember> getObjectCacheFilter() {
        if (cacheFilter != null) return cacheFilter;
        if (!bUseObjectCache) return null;
        hub.onBeforeRefresh(e -> reselect());
        cacheFilter = new OAObjectCacheFilter<TeamMember>(hub) {
            @Override
            public boolean isUsed(TeamMember teamMember) {
                return TeamMemberActiveFilter.this.isUsed(teamMember);
            }
            @Override
            protected void reselect() {
                TeamMemberActiveFilter.this.reselect();
            }
        };
        cacheFilter.addDependentProperty(TeamMemberPP.inactiveDate(), false);
        cacheFilter.refresh();
        return cacheFilter;
    }

    public void reselect() {
        // can be overwritten to query datasource
    }

    // ==================
    // this method has custom code that will need to be put into the OABuilder filter

    @Override
   public boolean isUsed(TeamMember teamMember) {
        // custom code here needs to be put in OABuilder
        return true;
   }
}
