package com.auto.dev.reportercorp.model.search;

import java.util.logging.Logger;

import com.auto.dev.reportercorp.model.SnapshotReportModel;
import com.auto.dev.reportercorp.model.oa.SnapshotReport;
import com.auto.dev.reportercorp.model.oa.SnapshotReportTemplate;
import com.auto.dev.reportercorp.model.oa.search.SnapshotReportTemplateSearch;
import com.viaoa.datasource.OASelect;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubEvent;
import com.viaoa.hub.HubListener;
import com.viaoa.hub.HubListenerAdapter;
import com.viaoa.object.OAFinder;

public class SnapshotReportTemplateSearchModel {
	private static Logger LOG = Logger.getLogger(SnapshotReportTemplateSearchModel.class.getName());

	protected Hub<SnapshotReportTemplate> hub; // search results
	protected Hub<SnapshotReportTemplate> hubMultiSelect;
	protected Hub<SnapshotReportTemplate> hubSearchFrom; // hub (optional) to search from
	protected Hub<SnapshotReportTemplateSearch> hubSnapshotReportTemplateSearch; // search data, size=1, AO
	// references used in search
	protected Hub<SnapshotReport> hubReport;

	// finder used to find objects in a path
	protected OAFinder<?, SnapshotReportTemplate> finder;

	// ObjectModels
	protected SnapshotReportModel modelReport;

	// SearchModels
	protected SnapshotReportSearchModel modelReportSearch;

	// object used for search data
	protected SnapshotReportTemplateSearch snapshotReportTemplateSearch;

	public SnapshotReportTemplateSearchModel() {
	}

	public SnapshotReportTemplateSearchModel(Hub<SnapshotReportTemplate> hub) {
		this.hub = hub;
	}

	// hub used for search results
	public Hub<SnapshotReportTemplate> getHub() {
		if (hub == null) {
			hub = new Hub<SnapshotReportTemplate>(SnapshotReportTemplate.class);
		}
		return hub;
	}

	// hub used to search within
	private HubListener hlSearchFromHub;

	public Hub<SnapshotReportTemplate> getSearchFromHub() {
		return hubSearchFrom;
	}

	public void setSearchFromHub(Hub<SnapshotReportTemplate> hub) {
		if (this.hlSearchFromHub != null) {
			hubSearchFrom.removeListener(hlSearchFromHub);
			hlSearchFromHub = null;
		}

		hubSearchFrom = hub;
		if (hubSearchFrom != null) {
			hlSearchFromHub = new HubListenerAdapter() {
				@Override
				public void onNewList(HubEvent e) {
					SnapshotReportTemplateSearchModel.this.getHub().clear();
				}
			};
			hubSearchFrom.addHubListener(hlSearchFromHub);
		}
	}

	public void close() {
		setSearchFromHub(null);
	}

	public Hub<SnapshotReportTemplate> getMultiSelectHub() {
		if (hubMultiSelect == null) {
			hubMultiSelect = new Hub<>(SnapshotReportTemplate.class);
		}
		return hubMultiSelect;
	}

	public OAFinder<?, SnapshotReportTemplate> getFinder() {
		return finder;
	}

	public void setFinder(OAFinder<?, SnapshotReportTemplate> finder) {
		this.finder = finder;
	}

	// object used to input query data, to be used by searchHub
	public SnapshotReportTemplateSearch getSnapshotReportTemplateSearch() {
		if (snapshotReportTemplateSearch != null) {
			return snapshotReportTemplateSearch;
		}
		snapshotReportTemplateSearch = new SnapshotReportTemplateSearch();
		return snapshotReportTemplateSearch;
	}

	// hub for search object - used to bind with UI components for entering search data
	public Hub<SnapshotReportTemplateSearch> getSnapshotReportTemplateSearchHub() {
		if (hubSnapshotReportTemplateSearch == null) {
			hubSnapshotReportTemplateSearch = new Hub<SnapshotReportTemplateSearch>(SnapshotReportTemplateSearch.class);
			hubSnapshotReportTemplateSearch.add(getSnapshotReportTemplateSearch());
			hubSnapshotReportTemplateSearch.setPos(0);
		}
		return hubSnapshotReportTemplateSearch;
	}

	public Hub<SnapshotReport> getReportHub() {
		if (hubReport != null) {
			return hubReport;
		}
		hubReport = getSnapshotReportTemplateSearchHub().getDetailHub(SnapshotReportTemplateSearch.P_Report);
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
			getSnapshotReportTemplateSearch().setReportSearch(modelReportSearch.getSnapshotReportSearch());
		}
		return modelReportSearch;
	}

	public void beforeInput() {
		// hook that is called before search input starts
	}

	// uses SnapshotReportTemplateSearch to build query, and populate Hub 
	public void performSearch() {
		OASelect<SnapshotReportTemplate> sel = getSnapshotReportTemplateSearch().getSelect();
		sel.setSearchHub(getSearchFromHub());
		sel.setFinder(getFinder());
		getHub().select(sel);
	}

	// can to overwritten to know when a selection is made
	public void onSelect(SnapshotReportTemplate snapshotReportTemplate, Hub<SnapshotReportTemplate> hub) {
	}

	// can to overwritten to know when a multi-select is made
	public void onSelect(Hub<SnapshotReportTemplate> hub) {
	}
}
