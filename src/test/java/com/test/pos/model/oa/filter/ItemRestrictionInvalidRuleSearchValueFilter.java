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
@OAClassFilter(name = "InvalidRuleSearchValue", displayName = "Invalid Rule Search Value", hasInputParams = false)
public class ItemRestrictionInvalidRuleSearchValueFilter extends OAObject implements CustomHubFilter<ItemRestriction> {
    private static final long serialVersionUID = 1L;

    private static Logger LOG = Logger.getLogger(ItemRestrictionInvalidRuleSearchValueFilter.class.getName());


    public static final String PPCode = ":InvalidRuleSearchValue()";
    private Hub<ItemRestriction> hubMaster;
    private Hub<ItemRestriction> hub;
    private HubFilter<ItemRestriction> hubFilter;
    private OAObjectCacheFilter<ItemRestriction> cacheFilter;
    private boolean bUseObjectCache;

    public ItemRestrictionInvalidRuleSearchValueFilter() {
        this(null, null, false);
    }
    public ItemRestrictionInvalidRuleSearchValueFilter(Hub<ItemRestriction> hub) {
        this(null, hub, true);
    }
    public ItemRestrictionInvalidRuleSearchValueFilter(Hub<ItemRestriction> hubMaster, Hub<ItemRestriction> hub) {
        this(hubMaster, hub, false);
    }
    public ItemRestrictionInvalidRuleSearchValueFilter(Hub<ItemRestriction> hubMaster, Hub<ItemRestriction> hubFiltered, boolean bUseObjectCache) {
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
    public HubFilter<ItemRestriction> getHubFilter() {
        if (hubFilter != null) return hubFilter;
        if (hubMaster == null) return null;
        hubFilter = new HubFilter<ItemRestriction>(hubMaster, hub) {
            @Override
            public boolean isUsed(ItemRestriction itemRestriction) {
                return ItemRestrictionInvalidRuleSearchValueFilter.this.isUsed(itemRestriction);
            }
        };
        hubFilter.addDependentProperty(ItemRestrictionPP.itemRuleType(), false);
        hubFilter.addDependentProperty(ItemRestrictionPP.line(), false);
        hubFilter.addDependentProperty(ItemRestrictionPP.productLineCode(), false);
        hubFilter.addDependentProperty(ItemRestrictionPP.productLineSubcode(), false);
        hubFilter.addDependentProperty(ItemRestrictionPP.item(), false);
        hubFilter.addDependentProperty(ItemRestrictionPP.locationRuleType(), false);
        hubFilter.addDependentProperty(ItemRestrictionPP.storeId(), false);
        hubFilter.addDependentProperty(ItemRestrictionPP.zipcode(), false);
        hubFilter.addDependentProperty(ItemRestrictionPP.state(), false);
        hubFilter.addDependentProperty(ItemRestrictionPP.county(), false);
        hubFilter.addDependentProperty(ItemRestrictionPP.ruleSearchValue(), false);
        hubFilter.refresh();
        return hubFilter;
    }

    public OAObjectCacheFilter<ItemRestriction> getObjectCacheFilter() {
        if (cacheFilter != null) return cacheFilter;
        if (!bUseObjectCache) return null;
        hub.onBeforeRefresh(e -> reselect());
        cacheFilter = new OAObjectCacheFilter<ItemRestriction>(hub) {
            @Override
            public boolean isUsed(ItemRestriction itemRestriction) {
                return ItemRestrictionInvalidRuleSearchValueFilter.this.isUsed(itemRestriction);
            }
            @Override
            protected void reselect() {
                ItemRestrictionInvalidRuleSearchValueFilter.this.reselect();
            }
        };
        cacheFilter.addDependentProperty(ItemRestrictionPP.itemRuleType(), false);
        cacheFilter.addDependentProperty(ItemRestrictionPP.line(), false);
        cacheFilter.addDependentProperty(ItemRestrictionPP.productLineCode(), false);
        cacheFilter.addDependentProperty(ItemRestrictionPP.productLineSubcode(), false);
        cacheFilter.addDependentProperty(ItemRestrictionPP.item(), false);
        cacheFilter.addDependentProperty(ItemRestrictionPP.locationRuleType(), false);
        cacheFilter.addDependentProperty(ItemRestrictionPP.storeId(), false);
        cacheFilter.addDependentProperty(ItemRestrictionPP.zipcode(), false);
        cacheFilter.addDependentProperty(ItemRestrictionPP.state(), false);
        cacheFilter.addDependentProperty(ItemRestrictionPP.county(), false);
        cacheFilter.addDependentProperty(ItemRestrictionPP.ruleSearchValue(), false);
        cacheFilter.refresh();
        return cacheFilter;
    }

    public void reselect() {
        // can be overwritten to query datasource
    }

    // ==================
    // this method has custom code that will need to be put into the OABuilder filter

    @Override
    public boolean isUsed(ItemRestriction itemRestriction) {
        boolean b = itemRestriction.getVerifyRuleSearchValue();
        return b == false;
    }
}
