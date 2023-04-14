package com.auto.dev.reportercorp.model.search;

import java.util.logging.Logger;

import com.auto.dev.reportercorp.delegate.ModelDelegate;
import com.auto.dev.reportercorp.model.ReportModel;
import com.auto.dev.reportercorp.model.ReportVersionModel;
import com.auto.dev.reportercorp.model.oa.Report;
import com.auto.dev.reportercorp.model.oa.ReportInstance;
import com.auto.dev.reportercorp.model.oa.ReportVersion;
import com.auto.dev.reportercorp.model.oa.search.ReportInstanceSearch;
import com.viaoa.datasource.OASelect;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubCopy;
import com.viaoa.hub.HubEvent;
import com.viaoa.hub.HubListener;
import com.viaoa.hub.HubListenerAdapter;
import com.viaoa.object.OAFinder;

public class ReportInstanceSearchModel {
	private static Logger LOG = Logger.getLogger(ReportInstanceSearchModel.class.getName());

	protected Hub<ReportInstance> hub; // search results
	protected Hub<ReportInstance> hubMultiSelect;
	protected Hub<ReportInstance> hubSearchFrom; // hub (optional) to search from
	protected Hub<ReportInstanceSearch> hubReportInstanceSearch; // search data, size=1, AO
	// references used in search
	protected Hub<Report> hubReport;
	protected Hub<ReportVersion> hubReportVersion;

	// finder used to find objects in a path
	protected OAFinder<?, ReportInstance> finder;

	// ObjectModels
	protected ReportModel modelReport;
	protected ReportVersionModel modelReportVersion;

	// SearchModels
	protected ReportSearchModel modelReportSearch;
	protected ReportVersionSearchModel modelReportVersionSearch;

	// object used for search data
	protected ReportInstanceSearch reportInstanceSearch;

	public ReportInstanceSearchModel() {
	}

	public ReportInstanceSearchModel(Hub<ReportInstance> hub) {
		this.hub = hub;
	}

	// hub used for search results
	public Hub<ReportInstance> getHub() {
		if (hub == null) {
			hub = new Hub<ReportInstance>(ReportInstance.class);
		}
		return hub;
	}

	// hub used to search within
	private HubListener hlSearchFromHub;

	public Hub<ReportInstance> getSearchFromHub() {
		return hubSearchFrom;
	}

	public void setSearchFromHub(Hub<ReportInstance> hub) {
		if (this.hlSearchFromHub != null) {
			hubSearchFrom.removeListener(hlSearchFromHub);
			hlSearchFromHub = null;
		}

		hubSearchFrom = hub;
		if (hubSearchFrom != null) {
			hlSearchFromHub = new HubListenerAdapter() {
				@Override
				public void onNewList(HubEvent e) {
					ReportInstanceSearchModel.this.getHub().clear();
				}
			};
			hubSearchFrom.addHubListener(hlSearchFromHub);
		}
	}

	public void close() {
		setSearchFromHub(null);
	}

	public Hub<ReportInstance> getMultiSelectHub() {
		if (hubMultiSelect == null) {
			hubMultiSelect = new Hub<>(ReportInstance.class);
		}
		return hubMultiSelect;
	}

	public OAFinder<?, ReportInstance> getFinder() {
		return finder;
	}

	public void setFinder(OAFinder<?, ReportInstance> finder) {
		this.finder = finder;
	}

	// object used to input query data, to be used by searchHub
	public ReportInstanceSearch getReportInstanceSearch() {
		if (reportInstanceSearch != null) {
			return reportInstanceSearch;
		}
		reportInstanceSearch = new ReportInstanceSearch();
		return reportInstanceSearch;
	}

	// hub for search object - used to bind with UI components for entering search data
	public Hub<ReportInstanceSearch> getReportInstanceSearchHub() {
		if (hubReportInstanceSearch == null) {
			hubReportInstanceSearch = new Hub<ReportInstanceSearch>(ReportInstanceSearch.class);
			hubReportInstanceSearch.add(getReportInstanceSearch());
			hubReportInstanceSearch.setPos(0);
		}
		return hubReportInstanceSearch;
	}

	public Hub<Report> getReportHub() {
		if (hubReport != null) {
			return hubReport;
		}
		hubReport = new Hub<>(Report.class);
		Hub<Report> hub = ModelDelegate.getReports();
		HubCopy<Report> hc = new HubCopy<>(hub, hubReport, false);
		hubReport.setLinkHub(getReportInstanceSearchHub(), ReportInstanceSearch.P_Report);
		return hubReport;
	}

	public Hub<ReportVersion> getReportVersionHub() {
		if (hubReportVersion != null) {
			return hubReportVersion;
		}
		hubReportVersion = getReportInstanceSearchHub().getDetailHub(ReportInstanceSearch.P_ReportVersion);
		return hubReportVersion;
	}

	public ReportModel getReportModel() {
		if (modelReport != null) {
			return modelReport;
		}
		modelReport = new ReportModel(getReportHub());
		modelReport.setDisplayName("Report");
		modelReport.setPluralDisplayName("Reports");
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

	public ReportVersionModel getReportVersionModel() {
		if (modelReportVersion != null) {
			return modelReportVersion;
		}
		modelReportVersion = new ReportVersionModel(getReportVersionHub());
		modelReportVersion.setDisplayName("Report Version");
		modelReportVersion.setPluralDisplayName("Report Versions");
		modelReportVersion.setAllowNew(false);
		modelReportVersion.setAllowSave(true);
		modelReportVersion.setAllowAdd(false);
		modelReportVersion.setAllowRemove(false);
		modelReportVersion.setAllowClear(true);
		modelReportVersion.setAllowDelete(false);
		modelReportVersion.setAllowSearch(true);
		modelReportVersion.setAllowHubSearch(false);
		modelReportVersion.setAllowGotoEdit(true);
		modelReportVersion.setViewOnly(true);
		return modelReportVersion;
	}

	public ReportSearchModel getReportSearchModel() {
		if (modelReportSearch == null) {
			modelReportSearch = new ReportSearchModel();
			getReportInstanceSearch().setReportSearch(modelReportSearch.getReportSearch());
		}
		return modelReportSearch;
	}

	public ReportVersionSearchModel getReportVersionSearchModel() {
		if (modelReportVersionSearch == null) {
			modelReportVersionSearch = new ReportVersionSearchModel();
			getReportInstanceSearch().setReportVersionSearch(modelReportVersionSearch.getReportVersionSearch());
		}
		return modelReportVersionSearch;
	}

	public void beforeInput() {
		// hook that is called before search input starts
	}

	// uses ReportInstanceSearch to build query, and populate Hub 
	public void performSearch() {
		OASelect<ReportInstance> sel = getReportInstanceSearch().getSelect();
		sel.setSearchHub(getSearchFromHub());
		sel.setFinder(getFinder());
		getHub().select(sel);
	}

	// can to overwritten to know when a selection is made
	public void onSelect(ReportInstance reportInstance, Hub<ReportInstance> hub) {
	}

	// can to overwritten to know when a multi-select is made
	public void onSelect(Hub<ReportInstance> hub) {
	}
}
