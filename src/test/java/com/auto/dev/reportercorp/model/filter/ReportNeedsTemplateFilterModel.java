package com.auto.dev.reportercorp.model.filter;

import java.util.logging.Logger;

import com.auto.dev.reportercorp.model.oa.Report;
import com.auto.dev.reportercorp.model.oa.filter.ReportNeedsTemplateFilter;
import com.viaoa.hub.Hub;

public class ReportNeedsTemplateFilterModel {
	private static Logger LOG = Logger.getLogger(ReportNeedsTemplateFilterModel.class.getName());

	// Hubs
	protected Hub<ReportNeedsTemplateFilter> hubFilter;

	// ObjectModels

	// object used for filter data
	protected ReportNeedsTemplateFilter filter;

	public ReportNeedsTemplateFilterModel(Hub<Report> hubMaster, Hub<Report> hub) {
		filter = new ReportNeedsTemplateFilter(hubMaster, hub);
	}

	public ReportNeedsTemplateFilterModel(Hub<Report> hub) {
		filter = new ReportNeedsTemplateFilter(hub);
	}

	// object used to input query data, to be used by filterHub
	public ReportNeedsTemplateFilter getFilter() {
		return filter;
	}

	// hub for filter UI object - used to bind with UI components for entering filter data
	public Hub<ReportNeedsTemplateFilter> getFilterHub() {
		if (hubFilter == null) {
			hubFilter = new Hub<ReportNeedsTemplateFilter>(ReportNeedsTemplateFilter.class);
			hubFilter.add(getFilter());
			hubFilter.setPos(0);
		}
		return hubFilter;
	}

	// get the Filtered hub
	public Hub<Report> getHub() {
		return getFilter().getHubFilter().getHub();
	}
}
