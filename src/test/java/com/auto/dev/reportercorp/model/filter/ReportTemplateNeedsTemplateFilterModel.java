package com.auto.dev.reportercorp.model.filter;

import java.util.logging.Logger;

import com.auto.dev.reportercorp.model.oa.ReportTemplate;
import com.auto.dev.reportercorp.model.oa.filter.ReportTemplateNeedsTemplateFilter;
import com.viaoa.hub.Hub;

public class ReportTemplateNeedsTemplateFilterModel {
	private static Logger LOG = Logger.getLogger(ReportTemplateNeedsTemplateFilterModel.class.getName());

	// Hubs
	protected Hub<ReportTemplateNeedsTemplateFilter> hubFilter;

	// ObjectModels

	// object used for filter data
	protected ReportTemplateNeedsTemplateFilter filter;

	public ReportTemplateNeedsTemplateFilterModel(Hub<ReportTemplate> hubMaster, Hub<ReportTemplate> hub) {
		filter = new ReportTemplateNeedsTemplateFilter(hubMaster, hub);
	}

	public ReportTemplateNeedsTemplateFilterModel(Hub<ReportTemplate> hub) {
		filter = new ReportTemplateNeedsTemplateFilter(hub);
	}

	// object used to input query data, to be used by filterHub
	public ReportTemplateNeedsTemplateFilter getFilter() {
		return filter;
	}

	// hub for filter UI object - used to bind with UI components for entering filter data
	public Hub<ReportTemplateNeedsTemplateFilter> getFilterHub() {
		if (hubFilter == null) {
			hubFilter = new Hub<ReportTemplateNeedsTemplateFilter>(ReportTemplateNeedsTemplateFilter.class);
			hubFilter.add(getFilter());
			hubFilter.setPos(0);
		}
		return hubFilter;
	}

	// get the Filtered hub
	public Hub<ReportTemplate> getHub() {
		return getFilter().getHubFilter().getHub();
	}
}
