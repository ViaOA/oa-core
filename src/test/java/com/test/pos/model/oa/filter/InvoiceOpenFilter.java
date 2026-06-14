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
public class InvoiceOpenFilter extends OAObject implements CustomHubFilter<Invoice> {
    private static final long serialVersionUID = 1L;

    private static Logger LOG = Logger.getLogger(InvoiceOpenFilter.class.getName());


    public static final String PPCode = ":Open()";
    private Hub<Invoice> hubMaster;
    private Hub<Invoice> hub;
    private HubFilter<Invoice> hubFilter;
    private OAObjectCacheFilter<Invoice> cacheFilter;
    private boolean bUseObjectCache;

    public InvoiceOpenFilter() {
        this(null, null, false);
    }
    public InvoiceOpenFilter(Hub<Invoice> hub) {
        this(null, hub, true);
    }
    public InvoiceOpenFilter(Hub<Invoice> hubMaster, Hub<Invoice> hub) {
        this(hubMaster, hub, false);
    }
    public InvoiceOpenFilter(Hub<Invoice> hubMaster, Hub<Invoice> hubFiltered, boolean bUseObjectCache) {
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
    public HubFilter<Invoice> getHubFilter() {
        if (hubFilter != null) return hubFilter;
        if (hubMaster == null) return null;
        hubFilter = new HubFilter<Invoice>(hubMaster, hub) {
            @Override
            public boolean isUsed(Invoice invoice) {
                return InvoiceOpenFilter.this.isUsed(invoice);
            }
        };
        hubFilter.addDependentProperty(InvoicePP.completed(), false);
        hubFilter.refresh();
        return hubFilter;
    }

    public OAObjectCacheFilter<Invoice> getObjectCacheFilter() {
        if (cacheFilter != null) return cacheFilter;
        if (!bUseObjectCache) return null;
        hub.onBeforeRefresh(e -> reselect());
        cacheFilter = new OAObjectCacheFilter<Invoice>(hub) {
            @Override
            public boolean isUsed(Invoice invoice) {
                return InvoiceOpenFilter.this.isUsed(invoice);
            }
            @Override
            protected void reselect() {
                InvoiceOpenFilter.this.reselect();
            }
        };
        cacheFilter.addDependentProperty(InvoicePP.completed(), false);
        cacheFilter.refresh();
        return cacheFilter;
    }

    public void reselect() {
        // can be overwritten to query datasource
    }

    // ==================
    // this method has custom code that will need to be put into the OABuilder filter

    @Override
   public boolean isUsed(Invoice invoice) {
        // custom code here needs to be put in OABuilder
        return true;
   }
}
