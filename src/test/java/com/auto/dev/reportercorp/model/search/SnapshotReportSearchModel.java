package com.auto.dev.reportercorp.model.search;

import java.util.logging.Logger;

import com.auto.dev.reportercorp.model.oa.SnapshotReport;
import com.auto.dev.reportercorp.model.oa.search.SnapshotReportSearch;
import com.viaoa.datasource.OASelect;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubEvent;
import com.viaoa.hub.HubListener;
import com.viaoa.hub.HubListenerAdapter;
import com.viaoa.object.OAFinder;

public class SnapshotReportSearchModel {
	private static Logger LOG = Logger.getLogger(SnapshotReportSearchModel.class.getName());

	protected Hub<SnapshotReport> hub; // search results
	protected Hub<SnapshotReport> hubMultiSelect;
	protected Hub<SnapshotReport> hubSearchFrom; // hub (optional) to search from
	protected Hub<SnapshotReportSearch> hubSnapshotReportSearch; // search data, size=1, AO

	// finder used to find objects in a path
	protected OAFinder<?, SnapshotReport> finder;

	// object used for search data
	protected SnapshotReportSearch snapshotReportSearch;

	public SnapshotReportSearchModel() {
	}

	public SnapshotReportSearchModel(Hub<SnapshotReport> hub) {
		this.hub = hub;
	}

	// hub used for search results
	public Hub<SnapshotReport> getHub() {
		if (hub == null) {
			hub = new Hub<SnapshotReport>(SnapshotReport.class);
		}
		return hub;
	}

	// hub used to search within
	private HubListener hlSearchFromHub;

	public Hub<SnapshotReport> getSearchFromHub() {
		return hubSearchFrom;
	}

	public void setSearchFromHub(Hub<SnapshotReport> hub) {
		if (this.hlSearchFromHub != null) {
			hubSearchFrom.removeListener(hlSearchFromHub);
			hlSearchFromHub = null;
		}

		hubSearchFrom = hub;
		if (hubSearchFrom != null) {
			hlSearchFromHub = new HubListenerAdapter() {
				@Override
				public void onNewList(HubEvent e) {
					SnapshotReportSearchModel.this.getHub().clear();
				}
			};
			hubSearchFrom.addHubListener(hlSearchFromHub);
		}
	}

	public void close() {
		setSearchFromHub(null);
	}

	public Hub<SnapshotReport> getMultiSelectHub() {
		if (hubMultiSelect == null) {
			hubMultiSelect = new Hub<>(SnapshotReport.class);
		}
		return hubMultiSelect;
	}

	public OAFinder<?, SnapshotReport> getFinder() {
		return finder;
	}

	public void setFinder(OAFinder<?, SnapshotReport> finder) {
		this.finder = finder;
	}

	// object used to input query data, to be used by searchHub
	public SnapshotReportSearch getSnapshotReportSearch() {
		if (snapshotReportSearch != null) {
			return snapshotReportSearch;
		}
		snapshotReportSearch = new SnapshotReportSearch();
		return snapshotReportSearch;
	}

	// hub for search object - used to bind with UI components for entering search data
	public Hub<SnapshotReportSearch> getSnapshotReportSearchHub() {
		if (hubSnapshotReportSearch == null) {
			hubSnapshotReportSearch = new Hub<SnapshotReportSearch>(SnapshotReportSearch.class);
			hubSnapshotReportSearch.add(getSnapshotReportSearch());
			hubSnapshotReportSearch.setPos(0);
		}
		return hubSnapshotReportSearch;
	}

	public void beforeInput() {
		// hook that is called before search input starts
	}

	// uses SnapshotReportSearch to build query, and populate Hub 
	public void performSearch() {
		OASelect<SnapshotReport> sel = getSnapshotReportSearch().getSelect();
		sel.setSearchHub(getSearchFromHub());
		sel.setFinder(getFinder());
		getHub().select(sel);
	}

	// can to overwritten to know when a selection is made
	public void onSelect(SnapshotReport snapshotReport, Hub<SnapshotReport> hub) {
	}

	// can to overwritten to know when a multi-select is made
	public void onSelect(Hub<SnapshotReport> hub) {
	}
}
