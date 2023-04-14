package com.auto.dev.reportercorp.model.search;

import java.util.logging.Logger;

import com.auto.dev.reportercorp.model.oa.ReportInstanceProcessorInfo;
import com.auto.dev.reportercorp.model.oa.search.ReportInstanceProcessorInfoSearch;
import com.viaoa.datasource.OASelect;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubEvent;
import com.viaoa.hub.HubListener;
import com.viaoa.hub.HubListenerAdapter;
import com.viaoa.object.OAFinder;

public class ReportInstanceProcessorInfoSearchModel {
	private static Logger LOG = Logger.getLogger(ReportInstanceProcessorInfoSearchModel.class.getName());

	protected Hub<ReportInstanceProcessorInfo> hub; // search results
	protected Hub<ReportInstanceProcessorInfo> hubMultiSelect;
	protected Hub<ReportInstanceProcessorInfo> hubSearchFrom; // hub (optional) to search from
	protected Hub<ReportInstanceProcessorInfoSearch> hubReportInstanceProcessorInfoSearch; // search data, size=1, AO

	// finder used to find objects in a path
	protected OAFinder<?, ReportInstanceProcessorInfo> finder;

	// object used for search data
	protected ReportInstanceProcessorInfoSearch reportInstanceProcessorInfoSearch;

	public ReportInstanceProcessorInfoSearchModel() {
	}

	public ReportInstanceProcessorInfoSearchModel(Hub<ReportInstanceProcessorInfo> hub) {
		this.hub = hub;
	}

	// hub used for search results
	public Hub<ReportInstanceProcessorInfo> getHub() {
		if (hub == null) {
			hub = new Hub<ReportInstanceProcessorInfo>(ReportInstanceProcessorInfo.class);
		}
		return hub;
	}

	// hub used to search within
	private HubListener hlSearchFromHub;

	public Hub<ReportInstanceProcessorInfo> getSearchFromHub() {
		return hubSearchFrom;
	}

	public void setSearchFromHub(Hub<ReportInstanceProcessorInfo> hub) {
		if (this.hlSearchFromHub != null) {
			hubSearchFrom.removeListener(hlSearchFromHub);
			hlSearchFromHub = null;
		}

		hubSearchFrom = hub;
		if (hubSearchFrom != null) {
			hlSearchFromHub = new HubListenerAdapter() {
				@Override
				public void onNewList(HubEvent e) {
					ReportInstanceProcessorInfoSearchModel.this.getHub().clear();
				}
			};
			hubSearchFrom.addHubListener(hlSearchFromHub);
		}
	}

	public void close() {
		setSearchFromHub(null);
	}

	public Hub<ReportInstanceProcessorInfo> getMultiSelectHub() {
		if (hubMultiSelect == null) {
			hubMultiSelect = new Hub<>(ReportInstanceProcessorInfo.class);
		}
		return hubMultiSelect;
	}

	public OAFinder<?, ReportInstanceProcessorInfo> getFinder() {
		return finder;
	}

	public void setFinder(OAFinder<?, ReportInstanceProcessorInfo> finder) {
		this.finder = finder;
	}

	// object used to input query data, to be used by searchHub
	public ReportInstanceProcessorInfoSearch getReportInstanceProcessorInfoSearch() {
		if (reportInstanceProcessorInfoSearch != null) {
			return reportInstanceProcessorInfoSearch;
		}
		reportInstanceProcessorInfoSearch = new ReportInstanceProcessorInfoSearch();
		return reportInstanceProcessorInfoSearch;
	}

	// hub for search object - used to bind with UI components for entering search data
	public Hub<ReportInstanceProcessorInfoSearch> getReportInstanceProcessorInfoSearchHub() {
		if (hubReportInstanceProcessorInfoSearch == null) {
			hubReportInstanceProcessorInfoSearch = new Hub<ReportInstanceProcessorInfoSearch>(ReportInstanceProcessorInfoSearch.class);
			hubReportInstanceProcessorInfoSearch.add(getReportInstanceProcessorInfoSearch());
			hubReportInstanceProcessorInfoSearch.setPos(0);
		}
		return hubReportInstanceProcessorInfoSearch;
	}

	public void beforeInput() {
		// hook that is called before search input starts
	}

	// uses ReportInstanceProcessorInfoSearch to build query, and populate Hub 
	public void performSearch() {
		OASelect<ReportInstanceProcessorInfo> sel = getReportInstanceProcessorInfoSearch().getSelect();
		sel.setSearchHub(getSearchFromHub());
		sel.setFinder(getFinder());
		getHub().select(sel);
	}

	// can to overwritten to know when a selection is made
	public void onSelect(ReportInstanceProcessorInfo reportInstanceProcessorInfo, Hub<ReportInstanceProcessorInfo> hub) {
	}

	// can to overwritten to know when a multi-select is made
	public void onSelect(Hub<ReportInstanceProcessorInfo> hub) {
	}
}
