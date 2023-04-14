package com.auto.dev.reportercorp.model.filter;

import java.util.logging.Logger;

import com.auto.dev.reportercorp.model.oa.Report;
import com.auto.dev.reportercorp.model.oa.filter.ReportMasterOnlyFilter;
import com.viaoa.hub.Hub;

public class ReportMasterOnlyFilterModel {
	private static Logger LOG = Logger.getLogger(ReportMasterOnlyFilterModel.class.getName());

	// Hubs
	protected Hub<ReportMasterOnlyFilter> hubFilter;

	// ObjectModels

	// object used for filter data
	protected ReportMasterOnlyFilter filter;

	public ReportMasterOnlyFilterModel(Hub<Report> hubMaster, Hub<Report> hub) {
		filter = new ReportMasterOnlyFilter(hubMaster, hub);
	}

	public ReportMasterOnlyFilterModel(Hub<Report> hub) {
		filter = new ReportMasterOnlyFilter(hub);
	}

	// object used to input query data, to be used by filterHub
	public ReportMasterOnlyFilter getFilter() {
		return filter;
	}

	// hub for filter UI object - used to bind with UI components for entering filter data
	public Hub<ReportMasterOnlyFilter> getFilterHub() {
		if (hubFilter == null) {
			hubFilter = new Hub<ReportMasterOnlyFilter>(ReportMasterOnlyFilter.class);
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
