package com.auto.dev.reportercorp.model;

import java.util.logging.Logger;

import com.auto.dev.reportercorp.model.oa.EnvironmentSnapshot;
import com.auto.dev.reportercorp.model.oa.SnapshotReport;
import com.auto.dev.reportercorp.model.oa.SnapshotReportTemplate;
import com.auto.dev.reportercorp.model.search.EnvironmentSnapshotSearchModel;
import com.auto.dev.reportercorp.model.search.SnapshotReportSearchModel;
import com.auto.dev.reportercorp.model.search.SnapshotReportTemplateSearchModel;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubCopy;
import com.viaoa.hub.HubDelegate;
import com.viaoa.hub.HubDetailDelegate;
import com.viaoa.hub.HubSelectDelegate;
import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObjectCallbackDelegate;
import com.viaoa.object.OAObjectModel;

public class SnapshotReportModel extends OAObjectModel {
	private static Logger LOG = Logger.getLogger(SnapshotReportModel.class.getName());

	// Hubs
	protected Hub<SnapshotReport> hub;
	// selected snapshotReports
	protected Hub<SnapshotReport> hubMultiSelect;
	// detail hubs
	protected Hub<EnvironmentSnapshot> hubEnvironmentSnapshot;
	protected Hub<SnapshotReport> hubParentSnapshotReport;
	protected Hub<SnapshotReport> hubSnapshotReports;
	protected Hub<SnapshotReportTemplate> hubSnapshotReportTemplates;

	// ObjectModels
	protected EnvironmentSnapshotModel modelEnvironmentSnapshot;
	protected SnapshotReportModel modelParentSnapshotReport;
	protected SnapshotReportModel modelSnapshotReports;
	protected SnapshotReportTemplateModel modelSnapshotReportTemplates;

	// SearchModels used for references
	protected EnvironmentSnapshotSearchModel modelEnvironmentSnapshotSearch;
	protected SnapshotReportSearchModel modelParentSnapshotReportSearch;
	protected SnapshotReportSearchModel modelSnapshotReportsSearch;
	protected SnapshotReportTemplateSearchModel modelSnapshotReportTemplatesSearch;

	public SnapshotReportModel() {
		setDisplayName("Snapshot Report");
		setPluralDisplayName("Snapshot Reports");
	}

	public SnapshotReportModel(Hub<SnapshotReport> hubSnapshotReport) {
		this();
		if (hubSnapshotReport != null) {
			HubDelegate.setObjectClass(hubSnapshotReport, SnapshotReport.class);
		}
		this.hub = hubSnapshotReport;
	}

	public SnapshotReportModel(SnapshotReport snapshotReport) {
		this();
		getHub().add(snapshotReport);
		getHub().setPos(0);
	}

	public Hub<SnapshotReport> getOriginalHub() {
		return getHub();
	}

	public Hub<EnvironmentSnapshot> getEnvironmentSnapshotHub() {
		if (hubEnvironmentSnapshot != null) {
			return hubEnvironmentSnapshot;
		}
		hubEnvironmentSnapshot = getHub().getDetailHub(SnapshotReport.P_EnvironmentSnapshot);
		return hubEnvironmentSnapshot;
	}

	public Hub<SnapshotReport> getParentSnapshotReportHub() {
		if (hubParentSnapshotReport != null) {
			return hubParentSnapshotReport;
		}
		// this is the owner, use detailHub
		hubParentSnapshotReport = getHub().getDetailHub(SnapshotReport.P_ParentSnapshotReport);
		return hubParentSnapshotReport;
	}

	public Hub<SnapshotReport> getSnapshotReports() {
		if (hubSnapshotReports == null) {
			hubSnapshotReports = getHub().getDetailHub(SnapshotReport.P_SnapshotReports);
		}
		return hubSnapshotReports;
	}

	public Hub<SnapshotReportTemplate> getSnapshotReportTemplates() {
		if (hubSnapshotReportTemplates == null) {
			hubSnapshotReportTemplates = getHub().getDetailHub(SnapshotReport.P_SnapshotReportTemplates);
		}
		return hubSnapshotReportTemplates;
	}

	public SnapshotReport getSnapshotReport() {
		return getHub().getAO();
	}

	public Hub<SnapshotReport> getHub() {
		if (hub == null) {
			hub = new Hub<SnapshotReport>(SnapshotReport.class);
		}
		return hub;
	}

	public Hub<SnapshotReport> getMultiSelectHub() {
		if (hubMultiSelect == null) {
			hubMultiSelect = new Hub<SnapshotReport>(SnapshotReport.class);
		}
		return hubMultiSelect;
	}

	public EnvironmentSnapshotModel getEnvironmentSnapshotModel() {
		if (modelEnvironmentSnapshot != null) {
			return modelEnvironmentSnapshot;
		}
		modelEnvironmentSnapshot = new EnvironmentSnapshotModel(getEnvironmentSnapshotHub());
		modelEnvironmentSnapshot.setDisplayName("Environment Snapshot");
		modelEnvironmentSnapshot.setPluralDisplayName("Environment Snapshots");
		modelEnvironmentSnapshot.setForJfc(getForJfc());
		modelEnvironmentSnapshot.setAllowNew(false);
		modelEnvironmentSnapshot.setAllowSave(true);
		modelEnvironmentSnapshot.setAllowAdd(false);
		modelEnvironmentSnapshot.setAllowRemove(true);
		modelEnvironmentSnapshot.setAllowClear(true);
		modelEnvironmentSnapshot.setAllowDelete(false);
		modelEnvironmentSnapshot.setAllowSearch(true);
		modelEnvironmentSnapshot.setAllowHubSearch(false);
		modelEnvironmentSnapshot.setAllowGotoEdit(true);
		modelEnvironmentSnapshot.setViewOnly(true);
		// call SnapshotReport.environmentSnapshotModelCallback(EnvironmentSnapshotModel) to be able to customize this model
		OAObjectCallbackDelegate.onObjectCallbackModel(	SnapshotReport.class, SnapshotReport.P_EnvironmentSnapshot,
														modelEnvironmentSnapshot);

		return modelEnvironmentSnapshot;
	}

	public SnapshotReportModel getParentSnapshotReportModel() {
		if (modelParentSnapshotReport != null) {
			return modelParentSnapshotReport;
		}
		modelParentSnapshotReport = new SnapshotReportModel(getParentSnapshotReportHub());
		modelParentSnapshotReport.setDisplayName("Parent Snapshot Report");
		modelParentSnapshotReport.setPluralDisplayName("Snapshot Reports");
		modelParentSnapshotReport.setForJfc(getForJfc());
		modelParentSnapshotReport.setAllowNew(false);
		modelParentSnapshotReport.setAllowSave(true);
		modelParentSnapshotReport.setAllowAdd(false);
		modelParentSnapshotReport.setAllowRemove(false);
		modelParentSnapshotReport.setAllowClear(false);
		modelParentSnapshotReport.setAllowDelete(false);
		modelParentSnapshotReport.setAllowSearch(false);
		modelParentSnapshotReport.setAllowHubSearch(true);
		modelParentSnapshotReport.setAllowGotoEdit(true);
		OALinkInfo li = HubDetailDelegate.getLinkInfoFromDetailToMaster(getOriginalHub());
		modelParentSnapshotReport.setCreateUI(li == null || !SnapshotReport.P_ParentSnapshotReport.equalsIgnoreCase(li.getName()));
		modelParentSnapshotReport.setViewOnly(getViewOnly());
		// call SnapshotReport.parentSnapshotReportModelCallback(SnapshotReportModel) to be able to customize this model
		OAObjectCallbackDelegate.onObjectCallbackModel(	SnapshotReport.class, SnapshotReport.P_ParentSnapshotReport,
														modelParentSnapshotReport);

		return modelParentSnapshotReport;
	}

	public SnapshotReportModel getSnapshotReportsModel() {
		if (modelSnapshotReports != null) {
			return modelSnapshotReports;
		}
		modelSnapshotReports = new SnapshotReportModel(getSnapshotReports());
		modelSnapshotReports.setDisplayName("Snapshot Report");
		modelSnapshotReports.setPluralDisplayName("Snapshot Reports");
		if (HubDetailDelegate.getIsFromSameMasterHub(getOriginalHub(), getSnapshotReports())) {
			modelSnapshotReports.setCreateUI(false);
		}
		if (!HubDetailDelegate.getIsValidRecursive(getSnapshotReports())) {
			modelSnapshotReports.setCreateUI(false);
		}
		modelSnapshotReports.setForJfc(getForJfc());
		modelSnapshotReports.setAllowNew(true);
		modelSnapshotReports.setAllowSave(true);
		modelSnapshotReports.setAllowAdd(false);
		modelSnapshotReports.setAllowMove(true);
		modelSnapshotReports.setAllowRemove(false);
		modelSnapshotReports.setAllowDelete(true);
		modelSnapshotReports.setAllowRefresh(false);
		modelSnapshotReports.setAllowSearch(false);
		modelSnapshotReports.setAllowHubSearch(true);
		modelSnapshotReports.setAllowGotoEdit(true);
		modelSnapshotReports.setViewOnly(getViewOnly());
		modelSnapshotReports.setAllowTableFilter(false);
		modelSnapshotReports.setAllowTableSorting(false);
		modelSnapshotReports.setAllowMultiSelect(false);
		modelSnapshotReports.setAllowCopy(false);
		modelSnapshotReports.setAllowCut(false);
		modelSnapshotReports.setAllowPaste(false);
		// call SnapshotReport.snapshotReportsModelCallback(SnapshotReportModel) to be able to customize this model
		OAObjectCallbackDelegate.onObjectCallbackModel(SnapshotReport.class, SnapshotReport.P_SnapshotReports, modelSnapshotReports);

		return modelSnapshotReports;
	}

	public SnapshotReportTemplateModel getSnapshotReportTemplatesModel() {
		if (modelSnapshotReportTemplates != null) {
			return modelSnapshotReportTemplates;
		}
		modelSnapshotReportTemplates = new SnapshotReportTemplateModel(getSnapshotReportTemplates());
		modelSnapshotReportTemplates.setDisplayName("Snapshot Report Template");
		modelSnapshotReportTemplates.setPluralDisplayName("Snapshot Report Templates");
		if (HubDetailDelegate.getIsFromSameMasterHub(getOriginalHub(), getSnapshotReportTemplates())) {
			modelSnapshotReportTemplates.setCreateUI(false);
		}
		modelSnapshotReportTemplates.setForJfc(getForJfc());
		modelSnapshotReportTemplates.setAllowNew(true);
		modelSnapshotReportTemplates.setAllowSave(true);
		modelSnapshotReportTemplates.setAllowAdd(false);
		modelSnapshotReportTemplates.setAllowMove(false);
		modelSnapshotReportTemplates.setAllowRemove(false);
		modelSnapshotReportTemplates.setAllowDelete(true);
		modelSnapshotReportTemplates.setAllowRefresh(false);
		modelSnapshotReportTemplates.setAllowSearch(false);
		modelSnapshotReportTemplates.setAllowHubSearch(true);
		modelSnapshotReportTemplates.setAllowGotoEdit(true);
		modelSnapshotReportTemplates.setViewOnly(getViewOnly());
		modelSnapshotReportTemplates.setAllowTableFilter(true);
		modelSnapshotReportTemplates.setAllowTableSorting(true);
		modelSnapshotReportTemplates.setAllowMultiSelect(false);
		modelSnapshotReportTemplates.setAllowCopy(false);
		modelSnapshotReportTemplates.setAllowCut(false);
		modelSnapshotReportTemplates.setAllowPaste(false);
		// call SnapshotReport.snapshotReportTemplatesModelCallback(SnapshotReportTemplateModel) to be able to customize this model
		OAObjectCallbackDelegate.onObjectCallbackModel(	SnapshotReport.class, SnapshotReport.P_SnapshotReportTemplates,
														modelSnapshotReportTemplates);

		return modelSnapshotReportTemplates;
	}

	public EnvironmentSnapshotSearchModel getEnvironmentSnapshotSearchModel() {
		if (modelEnvironmentSnapshotSearch != null) {
			return modelEnvironmentSnapshotSearch;
		}
		modelEnvironmentSnapshotSearch = new EnvironmentSnapshotSearchModel();
		HubSelectDelegate.adoptWhereHub(modelEnvironmentSnapshotSearch.getHub(), SnapshotReport.P_EnvironmentSnapshot, getHub());
		return modelEnvironmentSnapshotSearch;
	}

	public SnapshotReportSearchModel getParentSnapshotReportSearchModel() {
		if (modelParentSnapshotReportSearch != null) {
			return modelParentSnapshotReportSearch;
		}
		modelParentSnapshotReportSearch = new SnapshotReportSearchModel();
		HubSelectDelegate.adoptWhereHub(modelParentSnapshotReportSearch.getHub(), SnapshotReport.P_ParentSnapshotReport, getHub());
		return modelParentSnapshotReportSearch;
	}

	public SnapshotReportSearchModel getSnapshotReportsSearchModel() {
		if (modelSnapshotReportsSearch != null) {
			return modelSnapshotReportsSearch;
		}
		modelSnapshotReportsSearch = new SnapshotReportSearchModel();
		return modelSnapshotReportsSearch;
	}

	public SnapshotReportTemplateSearchModel getSnapshotReportTemplatesSearchModel() {
		if (modelSnapshotReportTemplatesSearch != null) {
			return modelSnapshotReportTemplatesSearch;
		}
		modelSnapshotReportTemplatesSearch = new SnapshotReportTemplateSearchModel();
		return modelSnapshotReportTemplatesSearch;
	}

	public HubCopy<SnapshotReport> createHubCopy() {
		Hub<SnapshotReport> hubSnapshotReportx = new Hub<>(SnapshotReport.class);
		HubCopy<SnapshotReport> hc = new HubCopy<>(getHub(), hubSnapshotReportx, true);
		return hc;
	}

	public SnapshotReportModel createCopy() {
		SnapshotReportModel mod = new SnapshotReportModel(createHubCopy().getHub());
		return mod;
	}
}
