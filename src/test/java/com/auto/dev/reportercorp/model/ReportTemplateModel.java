package com.auto.dev.reportercorp.model;

import java.util.logging.Logger;

import com.auto.dev.reportercorp.delegate.ModelDelegate;
import com.auto.dev.reportercorp.model.filter.ReportTemplateNeedsTemplateFilterModel;
import com.auto.dev.reportercorp.model.oa.Report;
import com.auto.dev.reportercorp.model.oa.ReportInstance;
import com.auto.dev.reportercorp.model.oa.ReportTemplate;
import com.auto.dev.reportercorp.model.oa.ReportVersion;
import com.auto.dev.reportercorp.model.oa.propertypath.ReportInstancePP;
import com.auto.dev.reportercorp.model.oa.propertypath.ReportTemplatePP;
import com.auto.dev.reportercorp.model.search.ReportInstanceSearchModel;
import com.auto.dev.reportercorp.model.search.ReportSearchModel;
import com.auto.dev.reportercorp.model.search.ReportVersionSearchModel;
import com.viaoa.filter.OAEqualPathFilter;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubCombined;
import com.viaoa.hub.HubCopy;
import com.viaoa.hub.HubDelegate;
import com.viaoa.hub.HubDetailDelegate;
import com.viaoa.hub.HubSelectDelegate;
import com.viaoa.hub.HubShareAO;
import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObjectCallbackDelegate;
import com.viaoa.object.OAObjectModel;
import com.viaoa.util.OAFilter;

public class ReportTemplateModel extends OAObjectModel {
	private static Logger LOG = Logger.getLogger(ReportTemplateModel.class.getName());

	/* overview
	  originalHub   - save the original hub
	  <- unfilteredHub - points one of the above hubs
	  needsTemplateFilteredHub;
	  <- hub - points to unfiltered or filtered hub
	*/

	// Hubs
	protected Hub<ReportTemplate> hubOriginal;

	// base hub that points to one of: hubOriginal
	protected Hub<ReportTemplate> hubUnfiltered;
	protected Hub<ReportTemplate> hubNeedsTemplateFilteredHub;
	// main hub that points to hubUnfiltered, hubNeedsTemplateFilteredHub
	protected Hub<ReportTemplate> hub;
	// selected reportTemplates
	protected Hub<ReportTemplate> hubMultiSelect;
	// detail hubs
	protected Hub<Report> hubReport;
	protected Hub<ReportVersion> hubReportVersions;
	protected Hub<ReportInstance> hubSearchReportInstances;

	// AddHubs used for references
	protected Hub<Report> hubReportSelectFrom;

	// ObjectModels
	protected ReportModel modelReport;
	protected ReportVersionModel modelReportVersions;
	protected ReportInstanceModel modelSearchReportInstances;

	// selectFrom
	protected ReportModel modelReportSelectFrom;

	// SearchModels used for references
	protected ReportSearchModel modelReportSearch;
	protected ReportVersionSearchModel modelReportVersionsSearch;
	protected ReportInstanceSearchModel modelSearchReportInstancesSearch;

	// FilterModels
	protected ReportTemplateNeedsTemplateFilterModel modelReportTemplateNeedsTemplateFilter;

	public ReportTemplateModel() {
		setDisplayName("Report Template");
		setPluralDisplayName("Report Templates");
	}

	public ReportTemplateModel(Hub<ReportTemplate> hubReportTemplate) {
		this();
		if (hubReportTemplate != null) {
			HubDelegate.setObjectClass(hubReportTemplate, ReportTemplate.class);
		}
		this.hubOriginal = hubReportTemplate;
	}

	public ReportTemplateModel(ReportTemplate reportTemplate) {
		this();
		getHub().add(reportTemplate);
		getHub().setPos(0);
	}

	public void useUnfilteredHub() {
		getHub().setSharedHub(getUnfilteredHub(), true);
	}

	public void useNeedsTemplateFilteredHub() {
		getHub().setSharedHub(getNeedsTemplateFilteredHub(), true);
	}

	public Hub<ReportTemplate> getOriginalHub() {
		if (hubOriginal == null) {
			hubOriginal = new Hub<ReportTemplate>(ReportTemplate.class);
		}
		return hubOriginal;
	}

	public Hub<Report> getReportHub() {
		if (hubReport != null) {
			return hubReport;
		}
		// this is the owner, use detailHub
		hubReport = getHub().getDetailHub(ReportTemplate.P_Report);
		return hubReport;
	}

	public Hub<ReportVersion> getReportVersions() {
		if (hubReportVersions == null) {
			hubReportVersions = getHub().getDetailHub(ReportTemplate.P_ReportVersions);
		}
		return hubReportVersions;
	}

	public Hub<ReportInstance> getSearchReportInstances() {
		// used by getSearchReportInstancesSearchModel() for searches
		if (hubSearchReportInstances != null) {
			return hubSearchReportInstances;
		}
		hubSearchReportInstances = new Hub<ReportInstance>(ReportInstance.class);
		return hubSearchReportInstances;
	}

	public Hub<Report> getReportSelectFromHub() {
		if (hubReportSelectFrom != null) {
			return hubReportSelectFrom;
		}
		hubReportSelectFrom = new Hub<Report>(Report.class);
		Hub<Report> hubReportSelectFrom1 = ModelDelegate.getReports().createSharedHub();
		HubCombined<Report> hubCombined = new HubCombined(hubReportSelectFrom, hubReportSelectFrom1, getReportHub());
		hubReportSelectFrom.setLinkHub(getHub(), ReportTemplate.P_Report);
		return hubReportSelectFrom;
	}

	public Hub<ReportTemplate> getUnfilteredHub() {
		if (hubUnfiltered == null) {
			hubUnfiltered = new Hub<ReportTemplate>(ReportTemplate.class);
			hubUnfiltered.setSharedHub(getOriginalHub(), true);
		}
		return hubUnfiltered;
	}

	public Hub<ReportTemplate> getNeedsTemplateFilteredHub() {
		if (hubNeedsTemplateFilteredHub == null) {
			hubNeedsTemplateFilteredHub = new Hub<ReportTemplate>(ReportTemplate.class);
		}
		return hubNeedsTemplateFilteredHub;
	}

	public ReportTemplate getReportTemplate() {
		return getHub().getAO();
	}

	// points to filtered or unfiltered hub
	public Hub<ReportTemplate> getHub() {
		if (hub == null) {
			hub = new Hub<ReportTemplate>(ReportTemplate.class);
			hub.setSharedHub(getUnfilteredHub(), true);
		}
		return hub;
	}

	public Hub<ReportTemplate> getMultiSelectHub() {
		if (hubMultiSelect == null) {
			hubMultiSelect = new Hub<ReportTemplate>(ReportTemplate.class);
		}
		return hubMultiSelect;
	}

	public ReportModel getReportModel() {
		if (modelReport != null) {
			return modelReport;
		}
		modelReport = new ReportModel(getReportHub());
		modelReport.setDisplayName("Report");
		modelReport.setPluralDisplayName("Reports");
		modelReport.setForJfc(getForJfc());
		modelReport.setAllowNew(false);
		modelReport.setAllowSave(true);
		modelReport.setAllowAdd(false);
		modelReport.setAllowRemove(false);
		modelReport.setAllowClear(false);
		modelReport.setAllowDelete(false);
		modelReport.setAllowSearch(false);
		modelReport.setAllowHubSearch(true);
		modelReport.setAllowGotoEdit(true);
		OALinkInfo li = HubDetailDelegate.getLinkInfoFromDetailToMaster(getOriginalHub());
		modelReport.setCreateUI(li == null || !ReportTemplate.P_Report.equalsIgnoreCase(li.getName()));
		modelReport.setViewOnly(getViewOnly());
		// call ReportTemplate.reportModelCallback(ReportModel) to be able to customize this model
		OAObjectCallbackDelegate.onObjectCallbackModel(ReportTemplate.class, ReportTemplate.P_Report, modelReport);

		return modelReport;
	}

	public ReportVersionModel getReportVersionsModel() {
		if (modelReportVersions != null) {
			return modelReportVersions;
		}
		modelReportVersions = new ReportVersionModel(getReportVersions());
		modelReportVersions.setDisplayName("Report Version");
		modelReportVersions.setPluralDisplayName("Report Versions");
		if (HubDetailDelegate.getIsFromSameMasterHub(getOriginalHub(), getReportVersions())) {
			modelReportVersions.setCreateUI(false);
		}
		modelReportVersions.setForJfc(getForJfc());
		modelReportVersions.setAllowNew(true);
		modelReportVersions.setAllowSave(true);
		modelReportVersions.setAllowAdd(false);
		modelReportVersions.setAllowMove(false);
		modelReportVersions.setAllowRemove(false);
		modelReportVersions.setAllowDelete(true);
		modelReportVersions.setAllowRefresh(true);
		modelReportVersions.setAllowSearch(false);
		modelReportVersions.setAllowHubSearch(true);
		modelReportVersions.setAllowGotoEdit(true);
		modelReportVersions.setViewOnly(getViewOnly());
		modelReportVersions.setAllowTableFilter(true);
		modelReportVersions.setAllowTableSorting(true);
		modelReportVersions.setAllowMultiSelect(false);
		modelReportVersions.setAllowCopy(false);
		modelReportVersions.setAllowCut(false);
		modelReportVersions.setAllowPaste(false);
		// call ReportTemplate.reportVersionsModelCallback(ReportVersionModel) to be able to customize this model
		OAObjectCallbackDelegate.onObjectCallbackModel(ReportTemplate.class, ReportTemplate.P_ReportVersions, modelReportVersions);

		return modelReportVersions;
	}

	public ReportInstanceModel getSearchReportInstancesModel() {
		if (modelSearchReportInstances != null) {
			return modelSearchReportInstances;
		}
		modelSearchReportInstances = new ReportInstanceModel(getSearchReportInstances());
		modelSearchReportInstances.setDisplayName("Report Instance");
		modelSearchReportInstances.setPluralDisplayName("Search Report Instances");
		modelSearchReportInstances.setForJfc(getForJfc());
		modelSearchReportInstances.setAllowNew(false);
		modelSearchReportInstances.setAllowSave(true);
		modelSearchReportInstances.setAllowAdd(false);
		modelSearchReportInstances.setAllowMove(false);
		modelSearchReportInstances.setAllowRemove(false);
		modelSearchReportInstances.setAllowDelete(true);
		modelSearchReportInstances.setAllowRefresh(true);
		modelSearchReportInstances.setAllowSearch(true);
		modelSearchReportInstances.setAllowHubSearch(false);
		modelSearchReportInstances.setAllowGotoEdit(true);
		modelSearchReportInstances.setViewOnly(getViewOnly());
		modelSearchReportInstances.setAllowTableFilter(true);
		modelSearchReportInstances.setAllowTableSorting(true);
		modelSearchReportInstances.setAllowMultiSelect(false);
		modelSearchReportInstances.setAllowCopy(false);
		modelSearchReportInstances.setAllowCut(false);
		modelSearchReportInstances.setAllowPaste(false);
		// call ReportTemplate.searchReportInstancesModelCallback(ReportInstanceModel) to be able to customize this model
		OAObjectCallbackDelegate.onObjectCallbackModel(ReportTemplate.class, "SearchReportInstances", modelSearchReportInstances);

		return modelSearchReportInstances;
	}

	public ReportModel getReportSelectFromModel() {
		if (modelReportSelectFrom != null) {
			return modelReportSelectFrom;
		}
		modelReportSelectFrom = new ReportModel(getReportSelectFromHub());
		modelReportSelectFrom.setDisplayName("Report");
		modelReportSelectFrom.setPluralDisplayName("Reports");
		modelReportSelectFrom.setForJfc(getForJfc());
		modelReportSelectFrom.setAllowNew(false);
		modelReportSelectFrom.setAllowSave(true);
		modelReportSelectFrom.setAllowAdd(false);
		modelReportSelectFrom.setAllowMove(false);
		modelReportSelectFrom.setAllowRemove(false);
		modelReportSelectFrom.setAllowDelete(false);
		modelReportSelectFrom.setAllowSearch(false);
		modelReportSelectFrom.setAllowHubSearch(true);
		modelReportSelectFrom.setAllowGotoEdit(true);
		modelReportSelectFrom.setViewOnly(getViewOnly());
		modelReportSelectFrom.setAllowNew(false);
		modelReportSelectFrom.setAllowTableFilter(true);
		modelReportSelectFrom.setAllowTableSorting(true);
		modelReportSelectFrom.setAllowCut(false);
		modelReportSelectFrom.setAllowCopy(false);
		modelReportSelectFrom.setAllowPaste(false);
		modelReportSelectFrom.setAllowMultiSelect(false);
		return modelReportSelectFrom;
	}

	public ReportSearchModel getReportSearchModel() {
		if (modelReportSearch != null) {
			return modelReportSearch;
		}
		modelReportSearch = new ReportSearchModel();
		HubSelectDelegate.adoptWhereHub(modelReportSearch.getHub(), ReportTemplate.P_Report, getHub());
		return modelReportSearch;
	}

	public ReportVersionSearchModel getReportVersionsSearchModel() {
		if (modelReportVersionsSearch != null) {
			return modelReportVersionsSearch;
		}
		modelReportVersionsSearch = new ReportVersionSearchModel();
		return modelReportVersionsSearch;
	}

	public ReportInstanceSearchModel getSearchReportInstancesSearchModel() {
		if (modelSearchReportInstancesSearch != null) {
			return modelSearchReportInstancesSearch;
		}
		modelSearchReportInstancesSearch = new ReportInstanceSearchModel(getSearchReportInstances()); // use hub for the search results
		OAFilter filter = new OAEqualPathFilter(ReportTemplateModel.this.getHub(), ReportTemplatePP.pp(),
				ReportInstancePP.reportVersion().reportTemplate().pp);
		modelSearchReportInstancesSearch.getReportInstanceSearch().setExtraWhereFilter(filter);
		return modelSearchReportInstancesSearch;
	}

	public ReportTemplateNeedsTemplateFilterModel getReportTemplateNeedsTemplateFilterModel() {
		if (modelReportTemplateNeedsTemplateFilter == null) {
			modelReportTemplateNeedsTemplateFilter = new ReportTemplateNeedsTemplateFilterModel(getUnfilteredHub(),
					getNeedsTemplateFilteredHub());
			new HubShareAO(getUnfilteredHub(), getNeedsTemplateFilteredHub());
		}
		return modelReportTemplateNeedsTemplateFilter;
	}

	public HubCopy<ReportTemplate> createHubCopy() {
		Hub<ReportTemplate> hubReportTemplatex = new Hub<>(ReportTemplate.class);
		HubCopy<ReportTemplate> hc = new HubCopy<>(getHub(), hubReportTemplatex, true);
		return hc;
	}

	public ReportTemplateModel createCopy() {
		ReportTemplateModel mod = new ReportTemplateModel(createHubCopy().getHub());
		return mod;
	}
}
