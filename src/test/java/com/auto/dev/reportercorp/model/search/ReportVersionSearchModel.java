package com.auto.dev.reportercorp.model.search;

import java.util.logging.Logger;

import com.auto.dev.reportercorp.delegate.ModelDelegate;
import com.auto.dev.reportercorp.model.ReportModel;
import com.auto.dev.reportercorp.model.oa.Report;
import com.auto.dev.reportercorp.model.oa.ReportVersion;
import com.auto.dev.reportercorp.model.oa.search.ReportVersionSearch;
import com.viaoa.datasource.OASelect;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubCopy;
import com.viaoa.hub.HubEvent;
import com.viaoa.hub.HubListener;
import com.viaoa.hub.HubListenerAdapter;
import com.viaoa.object.OAFinder;

public class ReportVersionSearchModel {
	private static Logger LOG = Logger.getLogger(ReportVersionSearchModel.class.getName());

	protected Hub<ReportVersion> hub; // search results
	protected Hub<ReportVersion> hubMultiSelect;
	protected Hub<ReportVersion> hubSearchFrom; // hub (optional) to search from
	protected Hub<ReportVersionSearch> hubReportVersionSearch; // search data, size=1, AO
	// references used in search
	protected Hub<Report> hubReport;

	// finder used to find objects in a path
	protected OAFinder<?, ReportVersion> finder;

	// ObjectModels
	protected ReportModel modelReport;

	// SearchModels
	protected ReportSearchModel modelReportSearch;

	// object used for search data
	protected ReportVersionSearch reportVersionSearch;

	public ReportVersionSearchModel() {
	}

	public ReportVersionSearchModel(Hub<ReportVersion> hub) {
		this.hub = hub;
	}

	// hub used for search results
	public Hub<ReportVersion> getHub() {
		if (hub == null) {
			hub = new Hub<ReportVersion>(ReportVersion.class);
		}
		return hub;
	}

	// hub used to search within
	private HubListener hlSearchFromHub;

	public Hub<ReportVersion> getSearchFromHub() {
		return hubSearchFrom;
	}

	public void setSearchFromHub(Hub<ReportVersion> hub) {
		if (this.hlSearchFromHub != null) {
			hubSearchFrom.removeListener(hlSearchFromHub);
			hlSearchFromHub = null;
		}

		hubSearchFrom = hub;
		if (hubSearchFrom != null) {
			hlSearchFromHub = new HubListenerAdapter() {
				@Override
				public void onNewList(HubEvent e) {
					ReportVersionSearchModel.this.getHub().clear();
				}
			};
			hubSearchFrom.addHubListener(hlSearchFromHub);
		}
	}

	public void close() {
		setSearchFromHub(null);
	}

	public Hub<ReportVersion> getMultiSelectHub() {
		if (hubMultiSelect == null) {
			hubMultiSelect = new Hub<>(ReportVersion.class);
		}
		return hubMultiSelect;
	}

	public OAFinder<?, ReportVersion> getFinder() {
		return finder;
	}

	public void setFinder(OAFinder<?, ReportVersion> finder) {
		this.finder = finder;
	}

	// object used to input query data, to be used by searchHub
	public ReportVersionSearch getReportVersionSearch() {
		if (reportVersionSearch != null) {
			return reportVersionSearch;
		}
		reportVersionSearch = new ReportVersionSearch();
		return reportVersionSearch;
	}

	// hub for search object - used to bind with UI components for entering search data
	public Hub<ReportVersionSearch> getReportVersionSearchHub() {
		if (hubReportVersionSearch == null) {
			hubReportVersionSearch = new Hub<ReportVersionSearch>(ReportVersionSearch.class);
			hubReportVersionSearch.add(getReportVersionSearch());
			hubReportVersionSearch.setPos(0);
		}
		return hubReportVersionSearch;
	}

	public Hub<Report> getReportHub() {
		if (hubReport != null) {
			return hubReport;
		}
		hubReport = new Hub<>(Report.class);
		Hub<Report> hub = ModelDelegate.getReports();
		HubCopy<Report> hc = new HubCopy<>(hub, hubReport, false);
		hubReport.setLinkHub(getReportVersionSearchHub(), ReportVersionSearch.P_Report);
		return hubReport;
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

	public ReportSearchModel getReportSearchModel() {
		if (modelReportSearch == null) {
			modelReportSearch = new ReportSearchModel();
			getReportVersionSearch().setReportSearch(modelReportSearch.getReportSearch());
		}
		return modelReportSearch;
	}

	public void beforeInput() {
		// hook that is called before search input starts
	}

	// uses ReportVersionSearch to build query, and populate Hub 
	public void performSearch() {
		OASelect<ReportVersion> sel = getReportVersionSearch().getSelect();
		sel.setSearchHub(getSearchFromHub());
		sel.setFinder(getFinder());
		getHub().select(sel);
	}

	// can to overwritten to know when a selection is made
	public void onSelect(ReportVersion reportVersion, Hub<ReportVersion> hub) {
	}

	// can to overwritten to know when a multi-select is made
	public void onSelect(Hub<ReportVersion> hub) {
	}
}
