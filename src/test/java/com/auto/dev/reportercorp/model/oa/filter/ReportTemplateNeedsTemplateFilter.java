package com.auto.dev.reportercorp.model.oa.filter;

import java.util.logging.Logger;

import com.auto.dev.reportercorp.model.oa.ReportTemplate;
import com.auto.dev.reportercorp.model.oa.propertypath.ReportTemplatePP;
import com.viaoa.annotation.OAClass;
import com.viaoa.annotation.OAClassFilter;
import com.viaoa.hub.CustomHubFilter;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubFilter;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectCacheFilter;

@OAClass(useDataSource = false, localOnly = true)
@OAClassFilter(name = "NeedsTemplate", displayName = "Needs Template", hasInputParams = false)
public class ReportTemplateNeedsTemplateFilter extends OAObject implements CustomHubFilter<ReportTemplate> {
	private static final long serialVersionUID = 1L;

	private static Logger LOG = Logger.getLogger(ReportTemplateNeedsTemplateFilter.class.getName());

	public static final String PPCode = ":NeedsTemplate()";
	private Hub<ReportTemplate> hubMaster;
	private Hub<ReportTemplate> hub;
	private HubFilter<ReportTemplate> hubFilter;
	private OAObjectCacheFilter<ReportTemplate> cacheFilter;
	private boolean bUseObjectCache;

	public ReportTemplateNeedsTemplateFilter() {
		this(null, null, false);
	}

	public ReportTemplateNeedsTemplateFilter(Hub<ReportTemplate> hub) {
		this(null, hub, true);
	}

	public ReportTemplateNeedsTemplateFilter(Hub<ReportTemplate> hubMaster, Hub<ReportTemplate> hub) {
		this(hubMaster, hub, false);
	}

	public ReportTemplateNeedsTemplateFilter(Hub<ReportTemplate> hubMaster, Hub<ReportTemplate> hubFiltered, boolean bUseObjectCache) {
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
	public HubFilter<ReportTemplate> getHubFilter() {
		if (hubFilter != null) {
			return hubFilter;
		}
		if (hubMaster == null) {
			return null;
		}
		hubFilter = new HubFilter<ReportTemplate>(hubMaster, hub) {
			@Override
			public boolean isUsed(ReportTemplate reportTemplate) {
				return ReportTemplateNeedsTemplateFilter.this.isUsed(reportTemplate);
			}
		};
		hubFilter.addDependentProperty(ReportTemplatePP.hasTemplate(), false);
		hubFilter.refresh();
		return hubFilter;
	}

	public OAObjectCacheFilter<ReportTemplate> getObjectCacheFilter() {
		if (cacheFilter != null) {
			return cacheFilter;
		}
		if (!bUseObjectCache) {
			return null;
		}
		hub.onBeforeRefresh(e -> reselect());
		cacheFilter = new OAObjectCacheFilter<ReportTemplate>(hub) {
			@Override
			public boolean isUsed(ReportTemplate reportTemplate) {
				return ReportTemplateNeedsTemplateFilter.this.isUsed(reportTemplate);
			}

			@Override
			protected void reselect() {
				ReportTemplateNeedsTemplateFilter.this.reselect();
			}
		};
		cacheFilter.addDependentProperty(ReportTemplatePP.hasTemplate(), false);
		cacheFilter.refresh();
		return cacheFilter;
	}

	public void reselect() {
		// can be overwritten to query datasource
	}

	// ==================
	// this method has custom code that will need to be put into the OABuilder filter

	@Override
	public boolean isUsed(ReportTemplate reportTemplate) {
		return (reportTemplate != null && !reportTemplate.getHasTemplate());
	}
}
