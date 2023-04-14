package com.auto.dev.reportercorp.model;

import java.util.logging.Logger;

import com.auto.dev.reportercorp.model.oa.SnapshotReport;
import com.auto.dev.reportercorp.model.oa.SnapshotReportTemplate;
import com.auto.dev.reportercorp.model.oa.SnapshotReportVersion;
import com.auto.dev.reportercorp.model.search.SnapshotReportSearchModel;
import com.auto.dev.reportercorp.model.search.SnapshotReportVersionSearchModel;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubCopy;
import com.viaoa.hub.HubDelegate;
import com.viaoa.hub.HubDetailDelegate;
import com.viaoa.hub.HubSelectDelegate;
import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObjectCallbackDelegate;
import com.viaoa.object.OAObjectModel;

public class SnapshotReportTemplateModel extends OAObjectModel {
	private static Logger LOG = Logger.getLogger(SnapshotReportTemplateModel.class.getName());

	// Hubs
	protected Hub<SnapshotReportTemplate> hub;
	// selected snapshotReportTemplates
	protected Hub<SnapshotReportTemplate> hubMultiSelect;
	// detail hubs
	protected Hub<SnapshotReport> hubSnapshotReport;
	protected Hub<SnapshotReportVersion> hubSnapshotReportVersions;

	// ObjectModels
	protected SnapshotReportModel modelSnapshotReport;
	protected SnapshotReportVersionModel modelSnapshotReportVersions;

	// SearchModels used for references
	protected SnapshotReportSearchModel modelSnapshotReportSearch;
	protected SnapshotReportVersionSearchModel modelSnapshotReportVersionsSearch;

	public SnapshotReportTemplateModel() {
		setDisplayName("Snapshot Report Template");
		setPluralDisplayName("Snapshot Report Templates");
	}

	public SnapshotReportTemplateModel(Hub<SnapshotReportTemplate> hubSnapshotReportTemplate) {
		this();
		if (hubSnapshotReportTemplate != null) {
			HubDelegate.setObjectClass(hubSnapshotReportTemplate, SnapshotReportTemplate.class);
		}
		this.hub = hubSnapshotReportTemplate;
	}

	public SnapshotReportTemplateModel(SnapshotReportTemplate snapshotReportTemplate) {
		this();
		getHub().add(snapshotReportTemplate);
		getHub().setPos(0);
	}

	public Hub<SnapshotReportTemplate> getOriginalHub() {
		return getHub();
	}

	public Hub<SnapshotReport> getSnapshotReportHub() {
		if (hubSnapshotReport != null) {
			return hubSnapshotReport;
		}
		// this is the owner, use detailHub
		hubSnapshotReport = getHub().getDetailHub(SnapshotReportTemplate.P_SnapshotReport);
		return hubSnapshotReport;
	}

	public Hub<SnapshotReportVersion> getSnapshotReportVersions() {
		if (hubSnapshotReportVersions == null) {
			hubSnapshotReportVersions = getHub().getDetailHub(SnapshotReportTemplate.P_SnapshotReportVersions);
		}
		return hubSnapshotReportVersions;
	}

	public SnapshotReportTemplate getSnapshotReportTemplate() {
		return getHub().getAO();
	}

	public Hub<SnapshotReportTemplate> getHub() {
		if (hub == null) {
			hub = new Hub<SnapshotReportTemplate>(SnapshotReportTemplate.class);
		}
		return hub;
	}

	public Hub<SnapshotReportTemplate> getMultiSelectHub() {
		if (hubMultiSelect == null) {
			hubMultiSelect = new Hub<SnapshotReportTemplate>(SnapshotReportTemplate.class);
		}
		return hubMultiSelect;
	}

	public SnapshotReportModel getSnapshotReportModel() {
		if (modelSnapshotReport != null) {
			return modelSnapshotReport;
		}
		modelSnapshotReport = new SnapshotReportModel(getSnapshotReportHub());
		modelSnapshotReport.setDisplayName("Snapshot Report");
		modelSnapshotReport.setPluralDisplayName("Snapshot Reports");
		modelSnapshotReport.setForJfc(getForJfc());
		modelSnapshotReport.setAllowNew(false);
		modelSnapshotReport.setAllowSave(true);
		modelSnapshotReport.setAllowAdd(false);
		modelSnapshotReport.setAllowRemove(false);
		modelSnapshotReport.setAllowClear(false);
		modelSnapshotReport.setAllowDelete(false);
		modelSnapshotReport.setAllowSearch(false);
		modelSnapshotReport.setAllowHubSearch(true);
		modelSnapshotReport.setAllowGotoEdit(true);
		OALinkInfo li = HubDetailDelegate.getLinkInfoFromDetailToMaster(getOriginalHub());
		modelSnapshotReport.setCreateUI(li == null || !SnapshotReportTemplate.P_SnapshotReport.equalsIgnoreCase(li.getName()));
		modelSnapshotReport.setViewOnly(getViewOnly());
		// call SnapshotReportTemplate.snapshotReportModelCallback(SnapshotReportModel) to be able to customize this model
		OAObjectCallbackDelegate.onObjectCallbackModel(	SnapshotReportTemplate.class, SnapshotReportTemplate.P_SnapshotReport,
														modelSnapshotReport);

		return modelSnapshotReport;
	}

	public SnapshotReportVersionModel getSnapshotReportVersionsModel() {
		if (modelSnapshotReportVersions != null) {
			return modelSnapshotReportVersions;
		}
		modelSnapshotReportVersions = new SnapshotReportVersionModel(getSnapshotReportVersions());
		modelSnapshotReportVersions.setDisplayName("Snapshot Report Version");
		modelSnapshotReportVersions.setPluralDisplayName("Snapshot Report Versions");
		if (HubDetailDelegate.getIsFromSameMasterHub(getOriginalHub(), getSnapshotReportVersions())) {
			modelSnapshotReportVersions.setCreateUI(false);
		}
		modelSnapshotReportVersions.setForJfc(getForJfc());
		modelSnapshotReportVersions.setAllowNew(true);
		modelSnapshotReportVersions.setAllowSave(true);
		modelSnapshotReportVersions.setAllowAdd(false);
		modelSnapshotReportVersions.setAllowMove(false);
		modelSnapshotReportVersions.setAllowRemove(false);
		modelSnapshotReportVersions.setAllowDelete(true);
		modelSnapshotReportVersions.setAllowRefresh(false);
		modelSnapshotReportVersions.setAllowSearch(false);
		modelSnapshotReportVersions.setAllowHubSearch(true);
		modelSnapshotReportVersions.setAllowGotoEdit(true);
		modelSnapshotReportVersions.setViewOnly(getViewOnly());
		modelSnapshotReportVersions.setAllowTableFilter(true);
		modelSnapshotReportVersions.setAllowTableSorting(true);
		modelSnapshotReportVersions.setAllowMultiSelect(false);
		modelSnapshotReportVersions.setAllowCopy(false);
		modelSnapshotReportVersions.setAllowCut(false);
		modelSnapshotReportVersions.setAllowPaste(false);
		// call SnapshotReportTemplate.snapshotReportVersionsModelCallback(SnapshotReportVersionModel) to be able to customize this model
		OAObjectCallbackDelegate.onObjectCallbackModel(	SnapshotReportTemplate.class, SnapshotReportTemplate.P_SnapshotReportVersions,
														modelSnapshotReportVersions);

		return modelSnapshotReportVersions;
	}

	public SnapshotReportSearchModel getSnapshotReportSearchModel() {
		if (modelSnapshotReportSearch != null) {
			return modelSnapshotReportSearch;
		}
		modelSnapshotReportSearch = new SnapshotReportSearchModel();
		HubSelectDelegate.adoptWhereHub(modelSnapshotReportSearch.getHub(), SnapshotReportTemplate.P_SnapshotReport, getHub());
		return modelSnapshotReportSearch;
	}

	public SnapshotReportVersionSearchModel getSnapshotReportVersionsSearchModel() {
		if (modelSnapshotReportVersionsSearch != null) {
			return modelSnapshotReportVersionsSearch;
		}
		modelSnapshotReportVersionsSearch = new SnapshotReportVersionSearchModel();
		return modelSnapshotReportVersionsSearch;
	}

	public HubCopy<SnapshotReportTemplate> createHubCopy() {
		Hub<SnapshotReportTemplate> hubSnapshotReportTemplatex = new Hub<>(SnapshotReportTemplate.class);
		HubCopy<SnapshotReportTemplate> hc = new HubCopy<>(getHub(), hubSnapshotReportTemplatex, true);
		return hc;
	}

	public SnapshotReportTemplateModel createCopy() {
		SnapshotReportTemplateModel mod = new SnapshotReportTemplateModel(createHubCopy().getHub());
		return mod;
	}
}
