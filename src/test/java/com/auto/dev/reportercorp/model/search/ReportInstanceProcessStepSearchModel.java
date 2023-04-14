package com.auto.dev.reportercorp.model.search;

import java.util.logging.Logger;

import com.auto.dev.reportercorp.model.oa.ReportInstanceProcessStep;
import com.auto.dev.reportercorp.model.oa.search.ReportInstanceProcessStepSearch;
import com.viaoa.datasource.OASelect;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubEvent;
import com.viaoa.hub.HubListener;
import com.viaoa.hub.HubListenerAdapter;
import com.viaoa.object.OAFinder;

public class ReportInstanceProcessStepSearchModel {
	private static Logger LOG = Logger.getLogger(ReportInstanceProcessStepSearchModel.class.getName());

	protected Hub<ReportInstanceProcessStep> hub; // search results
	protected Hub<ReportInstanceProcessStep> hubMultiSelect;
	protected Hub<ReportInstanceProcessStep> hubSearchFrom; // hub (optional) to search from
	protected Hub<ReportInstanceProcessStepSearch> hubReportInstanceProcessStepSearch; // search data, size=1, AO

	// finder used to find objects in a path
	protected OAFinder<?, ReportInstanceProcessStep> finder;

	// object used for search data
	protected ReportInstanceProcessStepSearch reportInstanceProcessStepSearch;

	public ReportInstanceProcessStepSearchModel() {
	}

	public ReportInstanceProcessStepSearchModel(Hub<ReportInstanceProcessStep> hub) {
		this.hub = hub;
	}

	// hub used for search results
	public Hub<ReportInstanceProcessStep> getHub() {
		if (hub == null) {
			hub = new Hub<ReportInstanceProcessStep>(ReportInstanceProcessStep.class);
		}
		return hub;
	}

	// hub used to search within
	private HubListener hlSearchFromHub;

	public Hub<ReportInstanceProcessStep> getSearchFromHub() {
		return hubSearchFrom;
	}

	public void setSearchFromHub(Hub<ReportInstanceProcessStep> hub) {
		if (this.hlSearchFromHub != null) {
			hubSearchFrom.removeListener(hlSearchFromHub);
			hlSearchFromHub = null;
		}

		hubSearchFrom = hub;
		if (hubSearchFrom != null) {
			hlSearchFromHub = new HubListenerAdapter() {
				@Override
				public void onNewList(HubEvent e) {
					ReportInstanceProcessStepSearchModel.this.getHub().clear();
				}
			};
			hubSearchFrom.addHubListener(hlSearchFromHub);
		}
	}

	public void close() {
		setSearchFromHub(null);
	}

	public Hub<ReportInstanceProcessStep> getMultiSelectHub() {
		if (hubMultiSelect == null) {
			hubMultiSelect = new Hub<>(ReportInstanceProcessStep.class);
		}
		return hubMultiSelect;
	}

	public OAFinder<?, ReportInstanceProcessStep> getFinder() {
		return finder;
	}

	public void setFinder(OAFinder<?, ReportInstanceProcessStep> finder) {
		this.finder = finder;
	}

	// object used to input query data, to be used by searchHub
	public ReportInstanceProcessStepSearch getReportInstanceProcessStepSearch() {
		if (reportInstanceProcessStepSearch != null) {
			return reportInstanceProcessStepSearch;
		}
		reportInstanceProcessStepSearch = new ReportInstanceProcessStepSearch();
		return reportInstanceProcessStepSearch;
	}

	// hub for search object - used to bind with UI components for entering search data
	public Hub<ReportInstanceProcessStepSearch> getReportInstanceProcessStepSearchHub() {
		if (hubReportInstanceProcessStepSearch == null) {
			hubReportInstanceProcessStepSearch = new Hub<ReportInstanceProcessStepSearch>(ReportInstanceProcessStepSearch.class);
			hubReportInstanceProcessStepSearch.add(getReportInstanceProcessStepSearch());
			hubReportInstanceProcessStepSearch.setPos(0);
		}
		return hubReportInstanceProcessStepSearch;
	}

	public void beforeInput() {
		// hook that is called before search input starts
	}

	// uses ReportInstanceProcessStepSearch to build query, and populate Hub 
	public void performSearch() {
		OASelect<ReportInstanceProcessStep> sel = getReportInstanceProcessStepSearch().getSelect();
		sel.setSearchHub(getSearchFromHub());
		sel.setFinder(getFinder());
		getHub().select(sel);
	}

	// can to overwritten to know when a selection is made
	public void onSelect(ReportInstanceProcessStep reportInstanceProcessStep, Hub<ReportInstanceProcessStep> hub) {
	}

	// can to overwritten to know when a multi-select is made
	public void onSelect(Hub<ReportInstanceProcessStep> hub) {
	}
}
