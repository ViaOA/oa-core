package com.auto.dev.reportercorp.model.search;

import java.util.logging.Logger;

import com.auto.dev.reportercorp.model.oa.ReportInstanceData;
import com.auto.dev.reportercorp.model.oa.search.ReportInstanceDataSearch;
import com.viaoa.datasource.OASelect;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubEvent;
import com.viaoa.hub.HubListener;
import com.viaoa.hub.HubListenerAdapter;
import com.viaoa.object.OAFinder;

public class ReportInstanceDataSearchModel {
	private static Logger LOG = Logger.getLogger(ReportInstanceDataSearchModel.class.getName());

	protected Hub<ReportInstanceData> hub; // search results
	protected Hub<ReportInstanceData> hubMultiSelect;
	protected Hub<ReportInstanceData> hubSearchFrom; // hub (optional) to search from
	protected Hub<ReportInstanceDataSearch> hubReportInstanceDataSearch; // search data, size=1, AO

	// finder used to find objects in a path
	protected OAFinder<?, ReportInstanceData> finder;

	// object used for search data
	protected ReportInstanceDataSearch reportInstanceDataSearch;

	public ReportInstanceDataSearchModel() {
	}

	public ReportInstanceDataSearchModel(Hub<ReportInstanceData> hub) {
		this.hub = hub;
	}

	// hub used for search results
	public Hub<ReportInstanceData> getHub() {
		if (hub == null) {
			hub = new Hub<ReportInstanceData>(ReportInstanceData.class);
		}
		return hub;
	}

	// hub used to search within
	private HubListener hlSearchFromHub;

	public Hub<ReportInstanceData> getSearchFromHub() {
		return hubSearchFrom;
	}

	public void setSearchFromHub(Hub<ReportInstanceData> hub) {
		if (this.hlSearchFromHub != null) {
			hubSearchFrom.removeListener(hlSearchFromHub);
			hlSearchFromHub = null;
		}

		hubSearchFrom = hub;
		if (hubSearchFrom != null) {
			hlSearchFromHub = new HubListenerAdapter() {
				@Override
				public void onNewList(HubEvent e) {
					ReportInstanceDataSearchModel.this.getHub().clear();
				}
			};
			hubSearchFrom.addHubListener(hlSearchFromHub);
		}
	}

	public void close() {
		setSearchFromHub(null);
	}

	public Hub<ReportInstanceData> getMultiSelectHub() {
		if (hubMultiSelect == null) {
			hubMultiSelect = new Hub<>(ReportInstanceData.class);
		}
		return hubMultiSelect;
	}

	public OAFinder<?, ReportInstanceData> getFinder() {
		return finder;
	}

	public void setFinder(OAFinder<?, ReportInstanceData> finder) {
		this.finder = finder;
	}

	// object used to input query data, to be used by searchHub
	public ReportInstanceDataSearch getReportInstanceDataSearch() {
		if (reportInstanceDataSearch != null) {
			return reportInstanceDataSearch;
		}
		reportInstanceDataSearch = new ReportInstanceDataSearch();
		return reportInstanceDataSearch;
	}

	// hub for search object - used to bind with UI components for entering search data
	public Hub<ReportInstanceDataSearch> getReportInstanceDataSearchHub() {
		if (hubReportInstanceDataSearch == null) {
			hubReportInstanceDataSearch = new Hub<ReportInstanceDataSearch>(ReportInstanceDataSearch.class);
			hubReportInstanceDataSearch.add(getReportInstanceDataSearch());
			hubReportInstanceDataSearch.setPos(0);
		}
		return hubReportInstanceDataSearch;
	}

	public void beforeInput() {
		// hook that is called before search input starts
	}

	// uses ReportInstanceDataSearch to build query, and populate Hub 
	public void performSearch() {
		OASelect<ReportInstanceData> sel = getReportInstanceDataSearch().getSelect();
		sel.setSearchHub(getSearchFromHub());
		sel.setFinder(getFinder());
		getHub().select(sel);
	}

	// can to overwritten to know when a selection is made
	public void onSelect(ReportInstanceData reportInstanceData, Hub<ReportInstanceData> hub) {
	}

	// can to overwritten to know when a multi-select is made
	public void onSelect(Hub<ReportInstanceData> hub) {
	}
}
