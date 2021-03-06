// Generated by OABuilder
package com.cdi.model.oa.filter;

import java.util.logging.*;
import com.cdi.model.oa.*;
import com.cdi.model.oa.propertypath.*;
import com.viaoa.annotation.*;
import com.viaoa.object.*;
import com.viaoa.hub.*;
import com.viaoa.util.*;
import java.util.*;
import com.cdi.model.search.*;
import com.cdi.model.oa.search.*;
import java.util.Calendar;

@OAClass(useDataSource=false, localOnly=true)
@OAClassFilter(name = "Open", displayName = "Open", hasInputParams = false)
public class WorkOrderOpenFilter extends OAObject implements CustomHubFilter<WorkOrder> {
    private static final long serialVersionUID = 1L;
    private static Logger LOG = Logger.getLogger(WorkOrderOpenFilter.class.getName());

    public static final String PPCode = ":Open()";
    private Hub<WorkOrder> hubMaster;
    private Hub<WorkOrder> hub;
    private HubFilter<WorkOrder> hubFilter;
    private OAObjectCacheFilter<WorkOrder> cacheFilter;
    private boolean bUseObjectCache;

    public WorkOrderOpenFilter() {
        this(null, null, false);
    }
    public WorkOrderOpenFilter(Hub<WorkOrder> hub) {
        this(null, hub, true);
    }
    public WorkOrderOpenFilter(Hub<WorkOrder> hubMaster, Hub<WorkOrder> hub) {
        this(hubMaster, hub, false);
    }
    public WorkOrderOpenFilter(Hub<WorkOrder> hubMaster, Hub<WorkOrder> hubFiltered, boolean bUseObjectCache) {
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
    public HubFilter<WorkOrder> getHubFilter() {
        if (hubFilter != null) return hubFilter;
        if (hubMaster == null) return null;
        hubFilter = new HubFilter<WorkOrder>(hubMaster, hub) {
            @Override
            public boolean isUsed(WorkOrder workOrder) {
                return WorkOrderOpenFilter.this.isUsed(workOrder);
            }
        };
        hubFilter.addDependentProperty(WorkOrderPP.completeDate(), false);
        hubFilter.addDependentProperty(WorkOrderPP.order().dateCompleted(), false);
        hubFilter.refresh();
        return hubFilter;
    }

    public OAObjectCacheFilter<WorkOrder> getObjectCacheFilter() {
        if (cacheFilter != null) return cacheFilter;
        if (!bUseObjectCache) return null;
        cacheFilter = new OAObjectCacheFilter<WorkOrder>(hub) {
            @Override
            public boolean isUsed(WorkOrder workOrder) {
                return WorkOrderOpenFilter.this.isUsed(workOrder);
            }
            @Override
            protected void reselect() {
                WorkOrderOpenFilter.this.reselect();
            }
        };
        cacheFilter.addDependentProperty(WorkOrderPP.completeDate(), false);
        cacheFilter.addDependentProperty(WorkOrderPP.order().dateCompleted(), false);
        cacheFilter.refresh();
        return cacheFilter;
    }

    public void reselect() {
        // can be overwritten to query datasource
    }

    // ==================
    // this method has custom code that will need to be put into the OABuilder filter

    @Override
    public boolean isUsed(WorkOrder workOrder) {
        if (workOrder == null) return false;
        if (workOrder.getCompleteDate() != null) return false;
        Order order = workOrder.getOrder();
        return (order != null && order.getDateCompleted() == null);
    }
}
