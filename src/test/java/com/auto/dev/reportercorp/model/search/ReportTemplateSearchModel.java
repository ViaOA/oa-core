package com.auto.dev.reportercorp.model.search;

import java.util.logging.Logger;

import com.auto.dev.reportercorp.delegate.ModelDelegate;
import com.auto.dev.reportercorp.model.ReportModel;
import com.auto.dev.reportercorp.model.oa.Report;
import com.auto.dev.reportercorp.model.oa.ReportTemplate;
import com.auto.dev.reportercorp.model.oa.search.ReportTemplateSearch;
import com.viaoa.datasource.OASelect;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubCopy;
import com.viaoa.hub.HubEvent;
import com.viaoa.hub.HubListener;
import com.viaoa.hub.HubListenerAdapter;
import com.viaoa.object.OAFinder;

public class ReportTemplateSearchModel {
	private static Logger LOG = Logger.getLogger(ReportTemplateSearchModel.class.getName());

	protected Hub<ReportTemplate> hub; // search results
	protected Hub<ReportTemplate> hubMultiSelect;
	protected Hub<ReportTemplate> hubSearchFrom; // hub (optional) to search from
	protected Hub<ReportTemplateSearch> hubReportTemplateSearch; // search data, size=1, AO
	// references used in search
	protected Hub<Report> hubReport;

	// finder used to find objects in a path
	protected OAFinder<?, ReportTemplate> finder;

	// ObjectModels
	protected ReportModel modelReport;

	// SearchModels
	protected ReportSearchModel modelReportSearch;

	// object used for search data
	protected ReportTemplateSearch reportTemplateSearch;

	public ReportTemplateSearchModel() {
	}

	public ReportTemplateSearchModel(Hub<ReportTemplate> hub) {
		this.hub = hub;
	}

	// hub used for search results
	public Hub<ReportTemplate> getHub() {
		if (hub == null) {
			hub = new Hub<ReportTemplate>(ReportTemplate.class);
		}
		return hub;
	}

	// hub used to search within
	private HubListener hlSearchFromHub;

	public Hub<ReportTemplate> getSearchFromHub() {
		return hubSearchFrom;
	}

	public void setSearchFromHub(Hub<ReportTemplate> hub) {
		if (this.hlSearchFromHub != null) {
			hubSearchFrom.removeListener(hlSearchFromHub);
			hlSearchFromHub = null;
		}

		hubSearchFrom = hub;
		if (hubSearchFrom != null) {
			hlSearchFromHub = new HubListenerAdapter() {
				@Override
				public void onNewList(HubEvent e) {
					ReportTemplateSearchModel.this.getHub().clear();
				}
			};
			hubSearchFrom.addHubListener(hlSearchFromHub);
		}
	}

	public void close() {
		setSearchFromHub(null);
	}

	public Hub<ReportTemplate> getMultiSelectHub() {
		if (hubMultiSelect == null) {
			hubMultiSelect = new Hub<>(ReportTemplate.class);
		}
		return hubMultiSelect;
	}

	public OAFinder<?, ReportTemplate> getFinder() {
		return finder;
	}

	public void setFinder(OAFinder<?, ReportTemplate> finder) {
		this.finder = finder;
	}

	// object used to input query data, to be used by searchHub
	public ReportTemplateSearch getReportTemplateSearch() {
		if (reportTemplateSearch != null) {
			return reportTemplateSearch;
		}
		reportTemplateSearch = new ReportTemplateSearch();
		return reportTemplateSearch;
	}

	// hub for search object - used to bind with UI components for entering search data
	public Hub<ReportTemplateSearch> getReportTemplateSearchHub() {
		if (hubReportTemplateSearch == null) {
			hubReportTemplateSearch = new Hub<ReportTemplateSearch>(ReportTemplateSearch.class);
			hubReportTemplateSearch.add(getReportTemplateSearch());
			hubReportTemplateSearch.setPos(0);
		}
		return hubReportTemplateSearch;
	}

	public Hub<Report> getReportHub() {
		if (hubReport != null) {
			return hubReport;
		}
		hubReport = new Hub<>(Report.class);
		Hub<Report> hub = ModelDelegate.getReports();
		HubCopy<Report> hc = new HubCopy<>(hub, hubReport, false);
		hubReport.setLinkHub(getReportTemplateSearchHub(), ReportTemplateSearch.P_Report);
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
			getReportTemplateSearch().setReportSearch(modelReportSearch.getReportSearch());
		}
		return modelReportSearch;
	}

	public void beforeInput() {
		// hook that is called before search input starts
	}

	// uses ReportTemplateSearch to build query, and populate Hub 
	public void performSearch() {
		OASelect<ReportTemplate> sel = getReportTemplateSearch().getSelect();
		sel.setSearchHub(getSearchFromHub());
		sel.setFinder(getFinder());
		getHub().select(sel);
	}

	// can to overwritten to know when a selection is made
	public void onSelect(ReportTemplate reportTemplate, Hub<ReportTemplate> hub) {
	}

	// can to overwritten to know when a multi-select is made
	public void onSelect(Hub<ReportTemplate> hub) {
	}
}
