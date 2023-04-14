package com.auto.dev.reportercorp.model.search;

import java.util.logging.Logger;

import com.auto.dev.reportercorp.model.oa.ReportInfo;
import com.auto.dev.reportercorp.model.oa.search.ReportInfoSearch;
import com.viaoa.datasource.OASelect;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubEvent;
import com.viaoa.hub.HubListener;
import com.viaoa.hub.HubListenerAdapter;
import com.viaoa.object.OAFinder;

public class ReportInfoSearchModel {
	private static Logger LOG = Logger.getLogger(ReportInfoSearchModel.class.getName());

	protected Hub<ReportInfo> hub; // search results
	protected Hub<ReportInfo> hubMultiSelect;
	protected Hub<ReportInfo> hubSearchFrom; // hub (optional) to search from
	protected Hub<ReportInfoSearch> hubReportInfoSearch; // search data, size=1, AO

	// finder used to find objects in a path
	protected OAFinder<?, ReportInfo> finder;

	// object used for search data
	protected ReportInfoSearch reportInfoSearch;

	public ReportInfoSearchModel() {
	}

	public ReportInfoSearchModel(Hub<ReportInfo> hub) {
		this.hub = hub;
	}

	// hub used for search results
	public Hub<ReportInfo> getHub() {
		if (hub == null) {
			hub = new Hub<ReportInfo>(ReportInfo.class);
		}
		return hub;
	}

	// hub used to search within
	private HubListener hlSearchFromHub;

	public Hub<ReportInfo> getSearchFromHub() {
		return hubSearchFrom;
	}

	public void setSearchFromHub(Hub<ReportInfo> hub) {
		if (this.hlSearchFromHub != null) {
			hubSearchFrom.removeListener(hlSearchFromHub);
			hlSearchFromHub = null;
		}

		hubSearchFrom = hub;
		if (hubSearchFrom != null) {
			hlSearchFromHub = new HubListenerAdapter() {
				@Override
				public void onNewList(HubEvent e) {
					ReportInfoSearchModel.this.getHub().clear();
				}
			};
			hubSearchFrom.addHubListener(hlSearchFromHub);
		}
	}

	public void close() {
		setSearchFromHub(null);
	}

	public Hub<ReportInfo> getMultiSelectHub() {
		if (hubMultiSelect == null) {
			hubMultiSelect = new Hub<>(ReportInfo.class);
		}
		return hubMultiSelect;
	}

	public OAFinder<?, ReportInfo> getFinder() {
		return finder;
	}

	public void setFinder(OAFinder<?, ReportInfo> finder) {
		this.finder = finder;
	}

	// object used to input query data, to be used by searchHub
	public ReportInfoSearch getReportInfoSearch() {
		if (reportInfoSearch != null) {
			return reportInfoSearch;
		}
		reportInfoSearch = new ReportInfoSearch();
		return reportInfoSearch;
	}

	// hub for search object - used to bind with UI components for entering search data
	public Hub<ReportInfoSearch> getReportInfoSearchHub() {
		if (hubReportInfoSearch == null) {
			hubReportInfoSearch = new Hub<ReportInfoSearch>(ReportInfoSearch.class);
			hubReportInfoSearch.add(getReportInfoSearch());
			hubReportInfoSearch.setPos(0);
		}
		return hubReportInfoSearch;
	}

	public void beforeInput() {
		// hook that is called before search input starts
	}

	// uses ReportInfoSearch to build query, and populate Hub 
	public void performSearch() {
		OASelect<ReportInfo> sel = getReportInfoSearch().getSelect();
		sel.setSearchHub(getSearchFromHub());
		sel.setFinder(getFinder());
		getHub().select(sel);
	}

	// can to overwritten to know when a selection is made
	public void onSelect(ReportInfo reportInfo, Hub<ReportInfo> hub) {
	}

	// can to overwritten to know when a multi-select is made
	public void onSelect(Hub<ReportInfo> hub) {
	}
}
