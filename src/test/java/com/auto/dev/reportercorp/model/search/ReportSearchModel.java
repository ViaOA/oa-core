package com.auto.dev.reportercorp.model.search;

import java.util.logging.Logger;

import com.auto.dev.reportercorp.model.oa.Report;
import com.auto.dev.reportercorp.model.oa.search.ReportSearch;
import com.viaoa.datasource.OASelect;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubEvent;
import com.viaoa.hub.HubListener;
import com.viaoa.hub.HubListenerAdapter;
import com.viaoa.object.OAFinder;

public class ReportSearchModel {
	private static Logger LOG = Logger.getLogger(ReportSearchModel.class.getName());

	protected Hub<Report> hub; // search results
	protected Hub<Report> hubMultiSelect;
	protected Hub<Report> hubSearchFrom; // hub (optional) to search from
	protected Hub<ReportSearch> hubReportSearch; // search data, size=1, AO

	// finder used to find objects in a path
	protected OAFinder<?, Report> finder;

	// object used for search data
	protected ReportSearch reportSearch;

	public ReportSearchModel() {
	}

	public ReportSearchModel(Hub<Report> hub) {
		this.hub = hub;
	}

	// hub used for search results
	public Hub<Report> getHub() {
		if (hub == null) {
			hub = new Hub<Report>(Report.class);
		}
		return hub;
	}

	// hub used to search within
	private HubListener hlSearchFromHub;

	public Hub<Report> getSearchFromHub() {
		return hubSearchFrom;
	}

	public void setSearchFromHub(Hub<Report> hub) {
		if (this.hlSearchFromHub != null) {
			hubSearchFrom.removeListener(hlSearchFromHub);
			hlSearchFromHub = null;
		}

		hubSearchFrom = hub;
		if (hubSearchFrom != null) {
			hlSearchFromHub = new HubListenerAdapter() {
				@Override
				public void onNewList(HubEvent e) {
					ReportSearchModel.this.getHub().clear();
				}
			};
			hubSearchFrom.addHubListener(hlSearchFromHub);
		}
	}

	public void close() {
		setSearchFromHub(null);
	}

	public Hub<Report> getMultiSelectHub() {
		if (hubMultiSelect == null) {
			hubMultiSelect = new Hub<>(Report.class);
		}
		return hubMultiSelect;
	}

	public OAFinder<?, Report> getFinder() {
		return finder;
	}

	public void setFinder(OAFinder<?, Report> finder) {
		this.finder = finder;
	}

	// object used to input query data, to be used by searchHub
	public ReportSearch getReportSearch() {
		if (reportSearch != null) {
			return reportSearch;
		}
		reportSearch = new ReportSearch();
		return reportSearch;
	}

	// hub for search object - used to bind with UI components for entering search data
	public Hub<ReportSearch> getReportSearchHub() {
		if (hubReportSearch == null) {
			hubReportSearch = new Hub<ReportSearch>(ReportSearch.class);
			hubReportSearch.add(getReportSearch());
			hubReportSearch.setPos(0);
		}
		return hubReportSearch;
	}

	public void beforeInput() {
		// hook that is called before search input starts
	}

	// uses ReportSearch to build query, and populate Hub 
	public void performSearch() {
		OASelect<Report> sel = getReportSearch().getSelect();
		sel.setSearchHub(getSearchFromHub());
		sel.setFinder(getFinder());
		getHub().select(sel);
	}

	// can to overwritten to know when a selection is made
	public void onSelect(Report report, Hub<Report> hub) {
	}

	// can to overwritten to know when a multi-select is made
	public void onSelect(Hub<Report> hub) {
	}
}
