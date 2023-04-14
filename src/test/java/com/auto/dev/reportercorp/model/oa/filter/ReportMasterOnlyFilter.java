package com.auto.dev.reportercorp.model.oa.filter;

import java.util.logging.Logger;

import com.auto.dev.reportercorp.model.oa.Report;
import com.auto.dev.reportercorp.model.oa.propertypath.ReportPP;
import com.viaoa.annotation.OAClass;
import com.viaoa.annotation.OAClassFilter;
import com.viaoa.hub.CustomHubFilter;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubFilter;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectCacheFilter;

@OAClass(useDataSource = false, localOnly = true)
@OAClassFilter(name = "MasterOnly", displayName = "Master Only", hasInputParams = false)
public class ReportMasterOnlyFilter extends OAObject implements CustomHubFilter<Report> {
	private static final long serialVersionUID = 1L;

	private static Logger LOG = Logger.getLogger(ReportMasterOnlyFilter.class.getName());

	public static final String PPCode = ":MasterOnly()";
	private Hub<Report> hubMaster;
	private Hub<Report> hub;
	private HubFilter<Report> hubFilter;
	private OAObjectCacheFilter<Report> cacheFilter;
	private boolean bUseObjectCache;

	public ReportMasterOnlyFilter() {
		this(null, null, false);
	}

	public ReportMasterOnlyFilter(Hub<Report> hub) {
		this(null, hub, true);
	}

	public ReportMasterOnlyFilter(Hub<Report> hubMaster, Hub<Report> hub) {
		this(hubMaster, hub, false);
	}

	public ReportMasterOnlyFilter(Hub<Report> hubMaster, Hub<Report> hubFiltered, boolean bUseObjectCache) {
		this.hubMaster = hubMaster;
		this.hub = hubFiltered;
		this.bUseObjectCache = bUseObjectCache;
		if (hubMaster != null) {
			getHubFilter();
		}
		if (bUseObjectCache) {
			getObjectCacheFilter();
		}
	}

	public void reset() {
	}

	public boolean isDataEntered() {
		return false;
	}

	public void refresh() {
		if (hubFilter != null) {
			getHubFilter().refresh();
		}
		if (cacheFilter != null) {
			getObjectCacheFilter().refresh();
		}
	}

	@Override
	public HubFilter<Report> getHubFilter() {
		if (hubFilter != null) {
			return hubFilter;
		}
		if (hubMaster == null) {
			return null;
		}
		hubFilter = new HubFilter<Report>(hubMaster, hub) {
			@Override
			public boolean isUsed(Report report) {
				return ReportMasterOnlyFilter.this.isUsed(report);
			}
		};
		hubFilter.addDependentProperty(ReportPP.parentReport().pp, false);
		hubFilter.refresh();
		return hubFilter;
	}

	public OAObjectCacheFilter<Report> getObjectCacheFilter() {
		if (cacheFilter != null) {
			return cacheFilter;
		}
		if (!bUseObjectCache) {
			return null;
		}
		hub.onBeforeRefresh(e -> reselect());
		cacheFilter = new OAObjectCacheFilter<Report>(hub) {
			@Override
			public boolean isUsed(Report report) {
				return ReportMasterOnlyFilter.this.isUsed(report);
			}

			@Override
			protected void reselect() {
				ReportMasterOnlyFilter.this.reselect();
			}
		};
		cacheFilter.addDependentProperty(ReportPP.parentReport().pp, false);
		cacheFilter.refresh();
		return cacheFilter;
	}

	public void reselect() {
		// can be overwritten to query datasource
	}

	// ==================
	// this method has custom code that will need to be put into the OABuilder filter

	@Override
	public boolean isUsed(Report report) {
		if (report == null) {
			return false;
		}
		boolean b = report.isNull(Report.P_ParentReport);
		return b;
	}
}
