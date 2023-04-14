package com.auto.dev.reportercorp.model.search;

import java.util.logging.Logger;

import com.auto.dev.reportercorp.model.SnapshotReportModel;
import com.auto.dev.reportercorp.model.oa.SnapshotReport;
import com.auto.dev.reportercorp.model.oa.SnapshotReportVersion;
import com.auto.dev.reportercorp.model.oa.search.SnapshotReportVersionSearch;
import com.viaoa.datasource.OASelect;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubEvent;
import com.viaoa.hub.HubListener;
import com.viaoa.hub.HubListenerAdapter;
import com.viaoa.object.OAFinder;

public class SnapshotReportVersionSearchModel {
	private static Logger LOG = Logger.getLogger(SnapshotReportVersionSearchModel.class.getName());

	protected Hub<SnapshotReportVersion> hub; // search results
	protected Hub<SnapshotReportVersion> hubMultiSelect;
	protected Hub<SnapshotReportVersion> hubSearchFrom; // hub (optional) to search from
	protected Hub<SnapshotReportVersionSearch> hubSnapshotReportVersionSearch; // search data, size=1, AO
	// references used in search
	protected Hub<SnapshotReport> hubReport;

	// finder used to find objects in a path
	protected OAFinder<?, SnapshotReportVersion> finder;

	// ObjectModels
	protected SnapshotReportModel modelReport;

	// SearchModels
	protected SnapshotReportSearchModel modelReportSearch;

	// object used for search data
	protected SnapshotReportVersionSearch snapshotReportVersionSearch;

	public SnapshotReportVersionSearchModel() {
	}

	public SnapshotReportVersionSearchModel(Hub<SnapshotReportVersion> hub) {
		this.hub = hub;
	}

	// hub used for search results
	public Hub<SnapshotReportVersion> getHub() {
		if (hub == null) {
			hub = new Hub<SnapshotReportVersion>(SnapshotReportVersion.class);
		}
		return hub;
	}

	// hub used to search within
	private HubListener hlSearchFromHub;

	public Hub<SnapshotReportVersion> getSearchFromHub() {
		return hubSearchFrom;
	}

	public void setSearchFromHub(Hub<SnapshotReportVersion> hub) {
		if (this.hlSearchFromHub != null) {
			hubSearchFrom.removeListener(hlSearchFromHub);
			hlSearchFromHub = null;
		}

		hubSearchFrom = hub;
		if (hubSearchFrom != null) {
			hlSearchFromHub = new HubListenerAdapter() {
				@Override
				public void onNewList(HubEvent e) {
					SnapshotReportVersionSearchModel.this.getHub().clear();
				}
			};
			hubSearchFrom.addHubListener(hlSearchFromHub);
		}
	}

	public void close() {
		setSearchFromHub(null);
	}

	public Hub<SnapshotReportVersion> getMultiSelectHub() {
		if (hubMultiSelect == null) {
			hubMultiSelect = new Hub<>(SnapshotReportVersion.class);
		}
		return hubMultiSelect;
	}

	public OAFinder<?, SnapshotReportVersion> getFinder() {
		return finder;
	}

	public void setFinder(OAFinder<?, SnapshotReportVersion> finder) {
		this.finder = finder;
	}

	// object used to input query data, to be used by searchHub
	public SnapshotReportVersionSearch getSnapshotReportVersionSearch() {
		if (snapshotReportVersionSearch != null) {
			return snapshotReportVersionSearch;
		}
		snapshotReportVersionSearch = new SnapshotReportVersionSearch();
		return snapshotReportVersionSearch;
	}

	// hub for search object - used to bind with UI components for entering search data
	public Hub<SnapshotReportVersionSearch> getSnapshotReportVersionSearchHub() {
		if (hubSnapshotReportVersionSearch == null) {
			hubSnapshotReportVersionSearch = new Hub<SnapshotReportVersionSearch>(SnapshotReportVersionSearch.class);
			hubSnapshotReportVersionSearch.add(getSnapshotReportVersionSearch());
			hubSnapshotReportVersionSearch.setPos(0);
		}
		return hubSnapshotReportVersionSearch;
	}

	public Hub<SnapshotReport> getReportHub() {
		if (hubReport != null) {
			return hubReport;
		}
		hubReport = getSnapshotReportVersionSearchHub().getDetailHub(SnapshotReportVersionSearch.P_Report);
		return hubReport;
	}

	public SnapshotReportModel getReportModel() {
		if (modelReport != null) {
			return modelReport;
		}
		modelReport = new SnapshotReportModel(getReportHub());
		modelReport.setDisplayName("Snapshot Report");
		modelReport.setPluralDisplayName("Snapshot Reports");
		modelReport.setAllowNew(false);
		modelReport.setAllowSave(true);
		modelReport.setAllowAdd(false);
		modelReport.setAllowRemove(false);
		modelReport.setAllowClear(true);
		modelReport.setAllowDelete(false);
		modelReport.setAllowSearch(true);
		modelReport.setAllowHubSearch(false);
		modelReport.setAllowGotoEdit(true);
		return modelReport;
	}

	public SnapshotReportSearchModel getReportSearchModel() {
		if (modelReportSearch == null) {
			modelReportSearch = new SnapshotReportSearchModel();
			getSnapshotReportVersionSearch().setReportSearch(modelReportSearch.getSnapshotReportSearch());
		}
		return modelReportSearch;
	}

	public void beforeInput() {
		// hook that is called before search input starts
	}

	// uses SnapshotReportVersionSearch to build query, and populate Hub 
	public void performSearch() {
		OASelect<SnapshotReportVersion> sel = getSnapshotReportVersionSearch().getSelect();
		sel.setSearchHub(getSearchFromHub());
		sel.setFinder(getFinder());
		getHub().select(sel);
	}

	// can to overwritten to know when a selection is made
	public void onSelect(SnapshotReportVersion snapshotReportVersion, Hub<SnapshotReportVersion> hub) {
	}

	// can to overwritten to know when a multi-select is made
	public void onSelect(Hub<SnapshotReportVersion> hub) {
	}
}
