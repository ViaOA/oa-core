package com.auto.dev.reportercorp.model;

import java.util.logging.Logger;

import com.auto.dev.reportercorp.delegate.ModelDelegate;
import com.auto.dev.reportercorp.model.filter.ReportMasterOnlyFilterModel;
import com.auto.dev.reportercorp.model.filter.ReportNeedsTemplateFilterModel;
import com.auto.dev.reportercorp.model.oa.Report;
import com.auto.dev.reportercorp.model.oa.ReportInstance;
import com.auto.dev.reportercorp.model.oa.ReportTemplate;
import com.auto.dev.reportercorp.model.oa.propertypath.ReportInstancePP;
import com.auto.dev.reportercorp.model.oa.propertypath.ReportPP;
import com.auto.dev.reportercorp.model.search.ReportInstanceSearchModel;
import com.auto.dev.reportercorp.model.search.ReportSearchModel;
import com.auto.dev.reportercorp.model.search.ReportTemplateSearchModel;
import com.viaoa.filter.OAEqualPathFilter;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubCombined;
import com.viaoa.hub.HubCopy;
import com.viaoa.hub.HubDelegate;
import com.viaoa.hub.HubDetailDelegate;
import com.viaoa.hub.HubMakeCopy;
import com.viaoa.hub.HubSelectDelegate;
import com.viaoa.hub.HubShareAO;
import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObjectCallbackDelegate;
import com.viaoa.object.OAObjectModel;
import com.viaoa.util.OAFilter;

public class ReportModel extends OAObjectModel {
	private static Logger LOG = Logger.getLogger(ReportModel.class.getName());

	/* overview
	  originalHub   - save the original hub
	  <- unfilteredHub - points one of the above hubs
	  masterOnlyFilteredHub;
	  needsTemplateFilteredHub;
	  <- hub - points to unfiltered or filtered hub
	*/

	// Hubs
	protected Hub<Report> hubOriginal;

	// base hub that points to one of: hubOriginal
	protected Hub<Report> hubUnfiltered;
	protected Hub<Report> hubMasterOnlyFilteredHub;
	protected Hub<Report> hubNeedsTemplateFilteredHub;
	// main hub that points to hubUnfiltered, hubMasterOnlyFilteredHub, hubNeedsTemplateFilteredHub
	protected Hub<Report> hub;
	// selected reports
	protected Hub<Report> hubMultiSelect;
	// detail hubs
	protected Hub<Report> hubParentReport;
	protected Hub<ReportTemplate> hubReportTemplates;
	protected Hub<ReportInstance> hubSearchReportInstances;
	protected Hub<Report> hubSubReports;

	// AddHubs used for references
	protected Hub<Report> hubParentReportSelectFrom;
	protected Hub<Report> hubSubReportsSelectFrom;

	// ObjectModels
	protected ReportModel modelParentReport;
	protected ReportTemplateModel modelReportTemplates;
	protected ReportInstanceModel modelSearchReportInstances;
	protected ReportModel modelSubReports;

	// selectFrom
	protected ReportModel modelParentReportSelectFrom;
	protected ReportModel modelSubReportsSelectFrom;

	// SearchModels used for references
	protected ReportSearchModel modelParentReportSearch;
	protected ReportTemplateSearchModel modelReportTemplatesSearch;
	protected ReportInstanceSearchModel modelSearchReportInstancesSearch;
	protected ReportSearchModel modelSubReportsSearch;

	// FilterModels
	protected ReportMasterOnlyFilterModel modelReportMasterOnlyFilter;
	protected ReportNeedsTemplateFilterModel modelReportNeedsTemplateFilter;

	public ReportModel() {
		setDisplayName("Report");
		setPluralDisplayName("Reports");
	}

	public ReportModel(Hub<Report> hubReport) {
		this();
		if (hubReport != null) {
			HubDelegate.setObjectClass(hubReport, Report.class);
		}
		this.hubOriginal = hubReport;
	}

	public ReportModel(Report report) {
		this();
		getHub().add(report);
		getHub().setPos(0);
	}

	public void useUnfilteredHub() {
		getHub().setSharedHub(getUnfilteredHub(), true);
	}

	public void useMasterOnlyFilteredHub() {
		getHub().setSharedHub(getMasterOnlyFilteredHub(), true);
	}

	public void useNeedsTemplateFilteredHub() {
		getHub().setSharedHub(getNeedsTemplateFilteredHub(), true);
	}

	public Hub<Report> getOriginalHub() {
		if (hubOriginal == null) {
			hubOriginal = new Hub<Report>(Report.class);
		}
		return hubOriginal;
	}

	public Hub<Report> getParentReportHub() {
		if (hubParentReport != null) {
			return hubParentReport;
		}
		// this is the owner, use detailHub
		hubParentReport = getHub().getDetailHub(Report.P_ParentReport);
		return hubParentReport;
	}

	public Hub<ReportTemplate> getReportTemplates() {
		if (hubReportTemplates == null) {
			hubReportTemplates = getHub().getDetailHub(Report.P_ReportTemplates);
		}
		return hubReportTemplates;
	}

	public Hub<ReportInstance> getSearchReportInstances() {
		// used by getSearchReportInstancesSearchModel() for searches
		if (hubSearchReportInstances != null) {
			return hubSearchReportInstances;
		}
		hubSearchReportInstances = new Hub<ReportInstance>(ReportInstance.class);
		return hubSearchReportInstances;
	}

	public Hub<Report> getSubReports() {
		if (hubSubReports == null) {
			hubSubReports = getHub().getDetailHub(Report.P_SubReports);
		}
		return hubSubReports;
	}

	public Hub<Report> getParentReportSelectFromHub() {
		if (hubParentReportSelectFrom != null) {
			return hubParentReportSelectFrom;
		}
		hubParentReportSelectFrom = new Hub<Report>(Report.class);
		Hub<Report> hubParentReportSelectFrom1 = ModelDelegate.getReports().createSharedHub();
		HubCombined<Report> hubCombined = new HubCombined(hubParentReportSelectFrom, hubParentReportSelectFrom1, getParentReportHub());
		hubParentReportSelectFrom.setLinkHub(getHub(), Report.P_ParentReport);
		return hubParentReportSelectFrom;
	}

	public Hub<Report> getSubReportsSelectFromHub() {
		if (hubSubReportsSelectFrom != null) {
			return hubSubReportsSelectFrom;
		}
		hubSubReportsSelectFrom = ModelDelegate.getReports().createSharedHub();
		return hubSubReportsSelectFrom;
	}

	public Hub<Report> getUnfilteredHub() {
		if (hubUnfiltered == null) {
			hubUnfiltered = new Hub<Report>(Report.class);
			hubUnfiltered.setSharedHub(getOriginalHub(), true);
		}
		return hubUnfiltered;
	}

	public Hub<Report> getMasterOnlyFilteredHub() {
		if (hubMasterOnlyFilteredHub == null) {
			hubMasterOnlyFilteredHub = new Hub<Report>(Report.class);
		}
		return hubMasterOnlyFilteredHub;
	}

	public Hub<Report> getNeedsTemplateFilteredHub() {
		if (hubNeedsTemplateFilteredHub == null) {
			hubNeedsTemplateFilteredHub = new Hub<Report>(Report.class);
		}
		return hubNeedsTemplateFilteredHub;
	}

	public Report getReport() {
		return getHub().getAO();
	}

	// points to filtered or unfiltered hub
	public Hub<Report> getHub() {
		if (hub == null) {
			hub = new Hub<Report>(Report.class);
			hub.setSharedHub(getUnfilteredHub(), true);
		}
		return hub;
	}

	public Hub<Report> getMultiSelectHub() {
		if (hubMultiSelect == null) {
			hubMultiSelect = new Hub<Report>(Report.class);
		}
		return hubMultiSelect;
	}

	public ReportModel getParentReportModel() {
		if (modelParentReport != null) {
			return modelParentReport;
		}
		modelParentReport = new ReportModel(getParentReportHub());
		modelParentReport.setDisplayName("Parent Report");
		modelParentReport.setPluralDisplayName("Reports");
		modelParentReport.setForJfc(getForJfc());
		modelParentReport.setAllowNew(false);
		modelParentReport.setAllowSave(true);
		modelParentReport.setAllowAdd(false);
		modelParentReport.setAllowRemove(false);
		modelParentReport.setAllowClear(false);
		modelParentReport.setAllowDelete(false);
		modelParentReport.setAllowSearch(false);
		modelParentReport.setAllowHubSearch(true);
		modelParentReport.setAllowGotoEdit(true);
		OALinkInfo li = HubDetailDelegate.getLinkInfoFromDetailToMaster(getOriginalHub());
		modelParentReport.setCreateUI(li == null || !Report.P_ParentReport.equalsIgnoreCase(li.getName()));
		modelParentReport.setViewOnly(getViewOnly());
		// call Report.parentReportModelCallback(ReportModel) to be able to customize this model
		OAObjectCallbackDelegate.onObjectCallbackModel(Report.class, Report.P_ParentReport, modelParentReport);

		return modelParentReport;
	}

	public ReportTemplateModel getReportTemplatesModel() {
		if (modelReportTemplates != null) {
			return modelReportTemplates;
		}
		modelReportTemplates = new ReportTemplateModel(getReportTemplates());
		modelReportTemplates.setDisplayName("Report Template");
		modelReportTemplates.setPluralDisplayName("Report Templates");
		if (HubDetailDelegate.getIsFromSameMasterHub(getOriginalHub(), getReportTemplates())) {
			modelReportTemplates.setCreateUI(false);
		}
		modelReportTemplates.setForJfc(getForJfc());
		modelReportTemplates.setAllowNew(true);
		modelReportTemplates.setAllowSave(true);
		modelReportTemplates.setAllowAdd(false);
		modelReportTemplates.setAllowMove(false);
		modelReportTemplates.setAllowRemove(false);
		modelReportTemplates.setAllowDelete(true);
		modelReportTemplates.setAllowRefresh(true);
		modelReportTemplates.setAllowSearch(false);
		modelReportTemplates.setAllowHubSearch(true);
		modelReportTemplates.setAllowGotoEdit(true);
		modelReportTemplates.setViewOnly(getViewOnly());
		modelReportTemplates.setAllowTableFilter(true);
		modelReportTemplates.setAllowTableSorting(true);
		modelReportTemplates.setAllowMultiSelect(false);
		modelReportTemplates.setAllowCopy(false);
		modelReportTemplates.setAllowCut(false);
		modelReportTemplates.setAllowPaste(false);
		// call Report.reportTemplatesModelCallback(ReportTemplateModel) to be able to customize this model
		OAObjectCallbackDelegate.onObjectCallbackModel(Report.class, Report.P_ReportTemplates, modelReportTemplates);

		return modelReportTemplates;
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
		// call Report.searchReportInstancesModelCallback(ReportInstanceModel) to be able to customize this model
		OAObjectCallbackDelegate.onObjectCallbackModel(Report.class, "SearchReportInstances", modelSearchReportInstances);

		return modelSearchReportInstances;
	}

	public ReportModel getSubReportsModel() {
		if (modelSubReports != null) {
			return modelSubReports;
		}
		modelSubReports = new ReportModel(getSubReports());
		modelSubReports.setDisplayName("Report");
		modelSubReports.setPluralDisplayName("Sub Reports");
		if (HubDetailDelegate.getIsFromSameMasterHub(getOriginalHub(), getSubReports())) {
			modelSubReports.setCreateUI(false);
		}
		if (!HubDetailDelegate.getIsValidRecursive(getSubReports())) {
			modelSubReports.setCreateUI(false);
		}
		modelSubReports.setForJfc(getForJfc());
		modelSubReports.setAllowNew(true);
		modelSubReports.setAllowSave(true);
		modelSubReports.setAllowAdd(false);
		modelSubReports.setAllowMove(true);
		modelSubReports.setAllowRemove(false);
		modelSubReports.setAllowDelete(true);
		modelSubReports.setAllowRefresh(true);
		modelSubReports.setAllowSearch(false);
		modelSubReports.setAllowHubSearch(true);
		modelSubReports.setAllowGotoEdit(true);
		modelSubReports.setViewOnly(getViewOnly());
		modelSubReports.setAllowTableFilter(false);
		modelSubReports.setAllowTableSorting(false);
		modelSubReports.setAllowMultiSelect(false);
		modelSubReports.setAllowCopy(false);
		modelSubReports.setAllowCut(false);
		modelSubReports.setAllowPaste(false);
		// call Report.subReportsModelCallback(ReportModel) to be able to customize this model
		OAObjectCallbackDelegate.onObjectCallbackModel(Report.class, Report.P_SubReports, modelSubReports);

		return modelSubReports;
	}

	public ReportModel getParentReportSelectFromModel() {
		if (modelParentReportSelectFrom != null) {
			return modelParentReportSelectFrom;
		}
		modelParentReportSelectFrom = new ReportModel(getParentReportSelectFromHub());
		modelParentReportSelectFrom.setDisplayName("Report");
		modelParentReportSelectFrom.setPluralDisplayName("Reports");
		modelParentReportSelectFrom.setForJfc(getForJfc());
		modelParentReportSelectFrom.setAllowNew(false);
		modelParentReportSelectFrom.setAllowSave(true);
		modelParentReportSelectFrom.setAllowAdd(false);
		modelParentReportSelectFrom.setAllowMove(false);
		modelParentReportSelectFrom.setAllowRemove(false);
		modelParentReportSelectFrom.setAllowDelete(false);
		modelParentReportSelectFrom.setAllowSearch(false);
		modelParentReportSelectFrom.setAllowHubSearch(true);
		modelParentReportSelectFrom.setAllowGotoEdit(true);
		modelParentReportSelectFrom.setViewOnly(getViewOnly());
		modelParentReportSelectFrom.setAllowNew(false);
		modelParentReportSelectFrom.setAllowTableFilter(true);
		modelParentReportSelectFrom.setAllowTableSorting(true);
		modelParentReportSelectFrom.setAllowCut(false);
		modelParentReportSelectFrom.setAllowCopy(false);
		modelParentReportSelectFrom.setAllowPaste(false);
		modelParentReportSelectFrom.setAllowMultiSelect(false);
		return modelParentReportSelectFrom;
	}

	public ReportModel getSubReportsSelectFromModel() {
		if (modelSubReportsSelectFrom != null) {
			return modelSubReportsSelectFrom;
		}
		modelSubReportsSelectFrom = new ReportModel(getSubReportsSelectFromHub());
		modelSubReportsSelectFrom.setDisplayName("Report");
		modelSubReportsSelectFrom.setPluralDisplayName("Reports");
		modelSubReportsSelectFrom.setForJfc(getForJfc());
		modelSubReportsSelectFrom.setAllowNew(false);
		modelSubReportsSelectFrom.setAllowSave(true);
		modelSubReportsSelectFrom.setAllowAdd(false);
		modelSubReportsSelectFrom.setAllowMove(false);
		modelSubReportsSelectFrom.setAllowRemove(false);
		modelSubReportsSelectFrom.setAllowDelete(false);
		modelSubReportsSelectFrom.setAllowRefresh(true);
		modelSubReportsSelectFrom.setAllowSearch(false);
		modelSubReportsSelectFrom.setAllowHubSearch(true);
		modelSubReportsSelectFrom.setAllowGotoEdit(true);
		modelSubReportsSelectFrom.setViewOnly(getViewOnly());
		modelSubReportsSelectFrom.setAllowNew(false);
		modelSubReportsSelectFrom.setAllowTableFilter(true);
		modelSubReportsSelectFrom.setAllowTableSorting(true);
		modelSubReportsSelectFrom.setAllowCut(false);
		modelSubReportsSelectFrom.setAllowCopy(false);
		modelSubReportsSelectFrom.setAllowPaste(false);
		modelSubReportsSelectFrom.setAllowMultiSelect(true);
		new HubMakeCopy(getSubReports(), modelSubReportsSelectFrom.getMultiSelectHub());
		return modelSubReportsSelectFrom;
	}

	public ReportSearchModel getParentReportSearchModel() {
		if (modelParentReportSearch != null) {
			return modelParentReportSearch;
		}
		modelParentReportSearch = new ReportSearchModel();
		HubSelectDelegate.adoptWhereHub(modelParentReportSearch.getHub(), Report.P_ParentReport, getHub());
		return modelParentReportSearch;
	}

	public ReportTemplateSearchModel getReportTemplatesSearchModel() {
		if (modelReportTemplatesSearch != null) {
			return modelReportTemplatesSearch;
		}
		modelReportTemplatesSearch = new ReportTemplateSearchModel();
		return modelReportTemplatesSearch;
	}

	public ReportInstanceSearchModel getSearchReportInstancesSearchModel() {
		if (modelSearchReportInstancesSearch != null) {
			return modelSearchReportInstancesSearch;
		}
		modelSearchReportInstancesSearch = new ReportInstanceSearchModel(getSearchReportInstances()); // use hub for the search results
		OAFilter filter = new OAEqualPathFilter(ReportModel.this.getHub(), ReportPP.pp(),
				ReportInstancePP.reportVersion().reportTemplate().report().pp);
		modelSearchReportInstancesSearch.getReportInstanceSearch().setExtraWhereFilter(filter);
		return modelSearchReportInstancesSearch;
	}

	public ReportSearchModel getSubReportsSearchModel() {
		if (modelSubReportsSearch != null) {
			return modelSubReportsSearch;
		}
		modelSubReportsSearch = new ReportSearchModel();
		return modelSubReportsSearch;
	}

	public ReportMasterOnlyFilterModel getReportMasterOnlyFilterModel() {
		if (modelReportMasterOnlyFilter == null) {
			modelReportMasterOnlyFilter = new ReportMasterOnlyFilterModel(getUnfilteredHub(), getMasterOnlyFilteredHub());
			new HubShareAO(getUnfilteredHub(), getMasterOnlyFilteredHub());
		}
		return modelReportMasterOnlyFilter;
	}

	public ReportNeedsTemplateFilterModel getReportNeedsTemplateFilterModel() {
		if (modelReportNeedsTemplateFilter == null) {
			modelReportNeedsTemplateFilter = new ReportNeedsTemplateFilterModel(getUnfilteredHub(), getNeedsTemplateFilteredHub());
			new HubShareAO(getUnfilteredHub(), getNeedsTemplateFilteredHub());
		}
		return modelReportNeedsTemplateFilter;
	}

	public HubCopy<Report> createHubCopy() {
		Hub<Report> hubReportx = new Hub<>(Report.class);
		HubCopy<Report> hc = new HubCopy<>(getHub(), hubReportx, true);
		return hc;
	}

	public ReportModel createCopy() {
		ReportModel mod = new ReportModel(createHubCopy().getHub());
		return mod;
	}
}
