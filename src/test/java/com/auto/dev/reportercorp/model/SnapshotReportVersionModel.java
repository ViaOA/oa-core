package com.auto.dev.reportercorp.model;

import java.util.logging.Logger;

import com.auto.dev.reportercorp.model.oa.SnapshotReportTemplate;
import com.auto.dev.reportercorp.model.oa.SnapshotReportVersion;
import com.auto.dev.reportercorp.model.oa.propertypath.SnapshotReportVersionPP;
import com.auto.dev.reportercorp.model.search.SnapshotReportTemplateSearchModel;
import com.auto.dev.reportercorp.model.search.SnapshotReportVersionSearchModel;
import com.viaoa.filter.OAInFilter;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubCopy;
import com.viaoa.hub.HubDelegate;
import com.viaoa.hub.HubDetailDelegate;
import com.viaoa.hub.HubMakeCopy;
import com.viaoa.hub.HubMerger;
import com.viaoa.hub.HubSelectDelegate;
import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObjectCallbackDelegate;
import com.viaoa.object.OAObjectModel;
import com.viaoa.util.OAFilter;

public class SnapshotReportVersionModel extends OAObjectModel {
	private static Logger LOG = Logger.getLogger(SnapshotReportVersionModel.class.getName());

	// Hubs
	protected Hub<SnapshotReportVersion> hub;
	// selected snapshotReportVersions
	protected Hub<SnapshotReportVersion> hubMultiSelect;
	// detail hubs
	protected Hub<SnapshotReportVersion> hubParentSnapshotReportVersion;
	protected Hub<SnapshotReportTemplate> hubSnapshotReportTemplate;
	protected Hub<SnapshotReportVersion> hubSnapshotReportVersions;

	// selectFrom
	protected Hub<SnapshotReportVersion> hubSnapshotReportVersionsSelectFrom;

	// ObjectModels
	protected SnapshotReportVersionModel modelParentSnapshotReportVersion;
	protected SnapshotReportTemplateModel modelSnapshotReportTemplate;
	protected SnapshotReportVersionModel modelSnapshotReportVersions;

	// selectFrom
	protected SnapshotReportVersionModel modelSnapshotReportVersionsSelectFrom;

	// SearchModels used for references
	protected SnapshotReportVersionSearchModel modelParentSnapshotReportVersionSearch;
	protected SnapshotReportTemplateSearchModel modelSnapshotReportTemplateSearch;
	protected SnapshotReportVersionSearchModel modelSnapshotReportVersionsSearch;

	public SnapshotReportVersionModel() {
		setDisplayName("Snapshot Report Version");
		setPluralDisplayName("Snapshot Report Versions");
	}

	public SnapshotReportVersionModel(Hub<SnapshotReportVersion> hubSnapshotReportVersion) {
		this();
		if (hubSnapshotReportVersion != null) {
			HubDelegate.setObjectClass(hubSnapshotReportVersion, SnapshotReportVersion.class);
		}
		this.hub = hubSnapshotReportVersion;
	}

	public SnapshotReportVersionModel(SnapshotReportVersion snapshotReportVersion) {
		this();
		getHub().add(snapshotReportVersion);
		getHub().setPos(0);
	}

	public Hub<SnapshotReportVersion> getOriginalHub() {
		return getHub();
	}

	public Hub<SnapshotReportVersion> getParentSnapshotReportVersionHub() {
		if (hubParentSnapshotReportVersion != null) {
			return hubParentSnapshotReportVersion;
		}
		hubParentSnapshotReportVersion = getHub().getDetailHub(SnapshotReportVersion.P_ParentSnapshotReportVersion);
		return hubParentSnapshotReportVersion;
	}

	public Hub<SnapshotReportTemplate> getSnapshotReportTemplateHub() {
		if (hubSnapshotReportTemplate != null) {
			return hubSnapshotReportTemplate;
		}
		// this is the owner, use detailHub
		hubSnapshotReportTemplate = getHub().getDetailHub(SnapshotReportVersion.P_SnapshotReportTemplate);
		return hubSnapshotReportTemplate;
	}

	public Hub<SnapshotReportVersion> getSnapshotReportVersions() {
		if (hubSnapshotReportVersions == null) {
			hubSnapshotReportVersions = getHub().getDetailHub(SnapshotReportVersion.P_SnapshotReportVersions);
		}
		return hubSnapshotReportVersions;
	}

	public Hub<SnapshotReportVersion> getSnapshotReportVersionsSelectFromHub() {
		if (hubSnapshotReportVersionsSelectFrom != null) {
			return hubSnapshotReportVersionsSelectFrom;
		}
		hubSnapshotReportVersionsSelectFrom = new Hub(SnapshotReportVersion.class);
		new HubMerger(getHub(), hubSnapshotReportVersionsSelectFrom, SnapshotReportVersionPP.snapshotReportTemplate().snapshotReport()
				.snapshotReports().snapshotReportTemplates().snapshotReportVersions().pp);
		return hubSnapshotReportVersionsSelectFrom;
	}

	public SnapshotReportVersion getSnapshotReportVersion() {
		return getHub().getAO();
	}

	public Hub<SnapshotReportVersion> getHub() {
		if (hub == null) {
			hub = new Hub<SnapshotReportVersion>(SnapshotReportVersion.class);
		}
		return hub;
	}

	public Hub<SnapshotReportVersion> getMultiSelectHub() {
		if (hubMultiSelect == null) {
			hubMultiSelect = new Hub<SnapshotReportVersion>(SnapshotReportVersion.class);
		}
		return hubMultiSelect;
	}

	public SnapshotReportVersionModel getParentSnapshotReportVersionModel() {
		if (modelParentSnapshotReportVersion != null) {
			return modelParentSnapshotReportVersion;
		}
		modelParentSnapshotReportVersion = new SnapshotReportVersionModel(getParentSnapshotReportVersionHub());
		modelParentSnapshotReportVersion.setDisplayName("Parent Snapshot Report Version");
		modelParentSnapshotReportVersion.setPluralDisplayName("Snapshot Report Versions");
		modelParentSnapshotReportVersion.setForJfc(getForJfc());
		modelParentSnapshotReportVersion.setAllowNew(false);
		modelParentSnapshotReportVersion.setAllowSave(true);
		modelParentSnapshotReportVersion.setAllowAdd(false);
		modelParentSnapshotReportVersion.setAllowRemove(false);
		modelParentSnapshotReportVersion.setAllowClear(false);
		modelParentSnapshotReportVersion.setAllowDelete(false);
		modelParentSnapshotReportVersion.setAllowSearch(false);
		modelParentSnapshotReportVersion.setAllowHubSearch(true);
		modelParentSnapshotReportVersion.setAllowGotoEdit(true);
		modelParentSnapshotReportVersion.setViewOnly(true);
		// call SnapshotReportVersion.parentSnapshotReportVersionModelCallback(SnapshotReportVersionModel) to be able to customize this model
		OAObjectCallbackDelegate.onObjectCallbackModel(	SnapshotReportVersion.class, SnapshotReportVersion.P_ParentSnapshotReportVersion,
														modelParentSnapshotReportVersion);

		return modelParentSnapshotReportVersion;
	}

	public SnapshotReportTemplateModel getSnapshotReportTemplateModel() {
		if (modelSnapshotReportTemplate != null) {
			return modelSnapshotReportTemplate;
		}
		modelSnapshotReportTemplate = new SnapshotReportTemplateModel(getSnapshotReportTemplateHub());
		modelSnapshotReportTemplate.setDisplayName("Snapshot Report Template");
		modelSnapshotReportTemplate.setPluralDisplayName("Snapshot Report Templates");
		modelSnapshotReportTemplate.setForJfc(getForJfc());
		modelSnapshotReportTemplate.setAllowNew(false);
		modelSnapshotReportTemplate.setAllowSave(true);
		modelSnapshotReportTemplate.setAllowAdd(false);
		modelSnapshotReportTemplate.setAllowRemove(false);
		modelSnapshotReportTemplate.setAllowClear(false);
		modelSnapshotReportTemplate.setAllowDelete(false);
		modelSnapshotReportTemplate.setAllowSearch(false);
		modelSnapshotReportTemplate.setAllowHubSearch(true);
		modelSnapshotReportTemplate.setAllowGotoEdit(true);
		OALinkInfo li = HubDetailDelegate.getLinkInfoFromDetailToMaster(getOriginalHub());
		modelSnapshotReportTemplate
				.setCreateUI(li == null || !SnapshotReportVersion.P_SnapshotReportTemplate.equalsIgnoreCase(li.getName()));
		modelSnapshotReportTemplate.setViewOnly(getViewOnly());
		// call SnapshotReportVersion.snapshotReportTemplateModelCallback(SnapshotReportTemplateModel) to be able to customize this model
		OAObjectCallbackDelegate.onObjectCallbackModel(	SnapshotReportVersion.class, SnapshotReportVersion.P_SnapshotReportTemplate,
														modelSnapshotReportTemplate);

		return modelSnapshotReportTemplate;
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
		if (!HubDetailDelegate.getIsValidRecursive(getSnapshotReportVersions())) {
			modelSnapshotReportVersions.setCreateUI(false);
		}
		modelSnapshotReportVersions.setForJfc(getForJfc());
		modelSnapshotReportVersions.setAllowNew(false);
		modelSnapshotReportVersions.setAllowSave(true);
		modelSnapshotReportVersions.setAllowAdd(true);
		modelSnapshotReportVersions.setAllowMove(false);
		modelSnapshotReportVersions.setAllowRemove(true);
		modelSnapshotReportVersions.setAllowDelete(false);
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
		// call SnapshotReportVersion.snapshotReportVersionsModelCallback(SnapshotReportVersionModel) to be able to customize this model
		OAObjectCallbackDelegate.onObjectCallbackModel(	SnapshotReportVersion.class, SnapshotReportVersion.P_SnapshotReportVersions,
														modelSnapshotReportVersions);

		return modelSnapshotReportVersions;
	}

	public SnapshotReportVersionModel getSnapshotReportVersionsSelectFromModel() {
		if (modelSnapshotReportVersionsSelectFrom != null) {
			return modelSnapshotReportVersionsSelectFrom;
		}
		modelSnapshotReportVersionsSelectFrom = new SnapshotReportVersionModel(getSnapshotReportVersionsSelectFromHub());
		modelSnapshotReportVersionsSelectFrom.setDisplayName("Snapshot Report Version");
		modelSnapshotReportVersionsSelectFrom.setPluralDisplayName("Snapshot Report Versions");
		modelSnapshotReportVersionsSelectFrom.setForJfc(getForJfc());
		modelSnapshotReportVersionsSelectFrom.setAllowNew(false);
		modelSnapshotReportVersionsSelectFrom.setAllowSave(true);
		modelSnapshotReportVersionsSelectFrom.setAllowAdd(false);
		modelSnapshotReportVersionsSelectFrom.setAllowMove(false);
		modelSnapshotReportVersionsSelectFrom.setAllowRemove(false);
		modelSnapshotReportVersionsSelectFrom.setAllowDelete(false);
		modelSnapshotReportVersionsSelectFrom.setAllowRefresh(false);
		modelSnapshotReportVersionsSelectFrom.setAllowSearch(true);
		modelSnapshotReportVersionsSelectFrom.setAllowHubSearch(true);
		modelSnapshotReportVersionsSelectFrom.setAllowGotoEdit(true);
		modelSnapshotReportVersionsSelectFrom.setViewOnly(getViewOnly());
		modelSnapshotReportVersionsSelectFrom.setAllowNew(false);
		modelSnapshotReportVersionsSelectFrom.setAllowTableFilter(true);
		modelSnapshotReportVersionsSelectFrom.setAllowTableSorting(true);
		modelSnapshotReportVersionsSelectFrom.setAllowCut(false);
		modelSnapshotReportVersionsSelectFrom.setAllowCopy(false);
		modelSnapshotReportVersionsSelectFrom.setAllowPaste(false);
		modelSnapshotReportVersionsSelectFrom.setAllowMultiSelect(true);
		new HubMakeCopy(getSnapshotReportVersions(), modelSnapshotReportVersionsSelectFrom.getMultiSelectHub());
		return modelSnapshotReportVersionsSelectFrom;
	}

	public SnapshotReportVersionSearchModel getParentSnapshotReportVersionSearchModel() {
		if (modelParentSnapshotReportVersionSearch != null) {
			return modelParentSnapshotReportVersionSearch;
		}
		modelParentSnapshotReportVersionSearch = new SnapshotReportVersionSearchModel();
		HubSelectDelegate.adoptWhereHub(modelParentSnapshotReportVersionSearch.getHub(),
										SnapshotReportVersion.P_ParentSnapshotReportVersion, getHub());
		return modelParentSnapshotReportVersionSearch;
	}

	public SnapshotReportTemplateSearchModel getSnapshotReportTemplateSearchModel() {
		if (modelSnapshotReportTemplateSearch != null) {
			return modelSnapshotReportTemplateSearch;
		}
		modelSnapshotReportTemplateSearch = new SnapshotReportTemplateSearchModel();
		HubSelectDelegate.adoptWhereHub(modelSnapshotReportTemplateSearch.getHub(), SnapshotReportVersion.P_SnapshotReportTemplate,
										getHub());
		return modelSnapshotReportTemplateSearch;
	}

	public SnapshotReportVersionSearchModel getSnapshotReportVersionsSearchModel() {
		if (modelSnapshotReportVersionsSearch != null) {
			return modelSnapshotReportVersionsSearch;
		}
		modelSnapshotReportVersionsSearch = new SnapshotReportVersionSearchModel();
		OAFilter filter = new OAInFilter(SnapshotReportVersionModel.this.getHub(), SnapshotReportVersionPP.snapshotReportTemplate()
				.snapshotReport().snapshotReports().snapshotReportTemplates().snapshotReportVersions().pp);
		modelSnapshotReportVersionsSearch.getSnapshotReportVersionSearch().setExtraWhereFilter(filter);
		return modelSnapshotReportVersionsSearch;
	}

	public HubCopy<SnapshotReportVersion> createHubCopy() {
		Hub<SnapshotReportVersion> hubSnapshotReportVersionx = new Hub<>(SnapshotReportVersion.class);
		HubCopy<SnapshotReportVersion> hc = new HubCopy<>(getHub(), hubSnapshotReportVersionx, true);
		return hc;
	}

	public SnapshotReportVersionModel createCopy() {
		SnapshotReportVersionModel mod = new SnapshotReportVersionModel(createHubCopy().getHub());
		return mod;
	}
}
