package com.auto.dev.reportercorp.model;

import java.util.logging.Logger;

import com.auto.dev.reportercorp.model.oa.ReportTemplate;
import com.auto.dev.reportercorp.model.oa.ReportVersion;
import com.auto.dev.reportercorp.model.oa.propertypath.ReportVersionPP;
import com.auto.dev.reportercorp.model.search.ReportTemplateSearchModel;
import com.auto.dev.reportercorp.model.search.ReportVersionSearchModel;
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

public class ReportVersionModel extends OAObjectModel {
	private static Logger LOG = Logger.getLogger(ReportVersionModel.class.getName());

	// Hubs
	protected Hub<ReportVersion> hub;
	// selected reportVersions
	protected Hub<ReportVersion> hubMultiSelect;
	// detail hubs
	protected Hub<ReportVersion> hubParentReportVersion;
	protected Hub<ReportTemplate> hubReportTemplate;
	protected Hub<ReportVersion> hubSubReportVersions;

	// selectFrom
	protected Hub<ReportVersion> hubSubReportVersionsSelectFrom;

	// ObjectModels
	protected ReportVersionModel modelParentReportVersion;
	protected ReportTemplateModel modelReportTemplate;
	protected ReportVersionModel modelSubReportVersions;

	// selectFrom
	protected ReportVersionModel modelSubReportVersionsSelectFrom;

	// SearchModels used for references
	protected ReportVersionSearchModel modelParentReportVersionSearch;
	protected ReportTemplateSearchModel modelReportTemplateSearch;
	protected ReportVersionSearchModel modelSubReportVersionsSearch;

	public ReportVersionModel() {
		setDisplayName("Report Version");
		setPluralDisplayName("Report Versions");
	}

	public ReportVersionModel(Hub<ReportVersion> hubReportVersion) {
		this();
		if (hubReportVersion != null) {
			HubDelegate.setObjectClass(hubReportVersion, ReportVersion.class);
		}
		this.hub = hubReportVersion;
	}

	public ReportVersionModel(ReportVersion reportVersion) {
		this();
		getHub().add(reportVersion);
		getHub().setPos(0);
	}

	public Hub<ReportVersion> getOriginalHub() {
		return getHub();
	}

	public Hub<ReportVersion> getParentReportVersionHub() {
		if (hubParentReportVersion != null) {
			return hubParentReportVersion;
		}
		hubParentReportVersion = getHub().getDetailHub(ReportVersion.P_ParentReportVersion);
		return hubParentReportVersion;
	}

	public Hub<ReportTemplate> getReportTemplateHub() {
		if (hubReportTemplate != null) {
			return hubReportTemplate;
		}
		// this is the owner, use detailHub
		hubReportTemplate = getHub().getDetailHub(ReportVersion.P_ReportTemplate);
		return hubReportTemplate;
	}

	public Hub<ReportVersion> getSubReportVersions() {
		if (hubSubReportVersions == null) {
			hubSubReportVersions = getHub().getDetailHub(ReportVersion.P_SubReportVersions);
		}
		return hubSubReportVersions;
	}

	public Hub<ReportVersion> getSubReportVersionsSelectFromHub() {
		if (hubSubReportVersionsSelectFrom != null) {
			return hubSubReportVersionsSelectFrom;
		}
		hubSubReportVersionsSelectFrom = new Hub(ReportVersion.class);
		new HubMerger(getHub(), hubSubReportVersionsSelectFrom,
				ReportVersionPP.reportTemplate().report().subReports().reportTemplates().reportVersions().pp);
		return hubSubReportVersionsSelectFrom;
	}

	public ReportVersion getReportVersion() {
		return getHub().getAO();
	}

	public Hub<ReportVersion> getHub() {
		if (hub == null) {
			hub = new Hub<ReportVersion>(ReportVersion.class);
		}
		return hub;
	}

	public Hub<ReportVersion> getMultiSelectHub() {
		if (hubMultiSelect == null) {
			hubMultiSelect = new Hub<ReportVersion>(ReportVersion.class);
		}
		return hubMultiSelect;
	}

	public ReportVersionModel getParentReportVersionModel() {
		if (modelParentReportVersion != null) {
			return modelParentReportVersion;
		}
		modelParentReportVersion = new ReportVersionModel(getParentReportVersionHub());
		modelParentReportVersion.setDisplayName("Parent Report Version");
		modelParentReportVersion.setPluralDisplayName("Report Versions");
		modelParentReportVersion.setForJfc(getForJfc());
		modelParentReportVersion.setAllowNew(false);
		modelParentReportVersion.setAllowSave(true);
		modelParentReportVersion.setAllowAdd(false);
		modelParentReportVersion.setAllowRemove(false);
		modelParentReportVersion.setAllowClear(false);
		modelParentReportVersion.setAllowDelete(false);
		modelParentReportVersion.setAllowSearch(false);
		modelParentReportVersion.setAllowHubSearch(true);
		modelParentReportVersion.setAllowGotoEdit(true);
		modelParentReportVersion.setViewOnly(true);
		// call ReportVersion.parentReportVersionModelCallback(ReportVersionModel) to be able to customize this model
		OAObjectCallbackDelegate.onObjectCallbackModel(ReportVersion.class, ReportVersion.P_ParentReportVersion, modelParentReportVersion);

		return modelParentReportVersion;
	}

	public ReportTemplateModel getReportTemplateModel() {
		if (modelReportTemplate != null) {
			return modelReportTemplate;
		}
		modelReportTemplate = new ReportTemplateModel(getReportTemplateHub());
		modelReportTemplate.setDisplayName("Report Template");
		modelReportTemplate.setPluralDisplayName("Report Templates");
		modelReportTemplate.setForJfc(getForJfc());
		modelReportTemplate.setAllowNew(false);
		modelReportTemplate.setAllowSave(true);
		modelReportTemplate.setAllowAdd(false);
		modelReportTemplate.setAllowRemove(false);
		modelReportTemplate.setAllowClear(false);
		modelReportTemplate.setAllowDelete(false);
		modelReportTemplate.setAllowSearch(false);
		modelReportTemplate.setAllowHubSearch(true);
		modelReportTemplate.setAllowGotoEdit(true);
		OALinkInfo li = HubDetailDelegate.getLinkInfoFromDetailToMaster(getOriginalHub());
		modelReportTemplate.setCreateUI(li == null || !ReportVersion.P_ReportTemplate.equalsIgnoreCase(li.getName()));
		modelReportTemplate.setViewOnly(getViewOnly());
		// call ReportVersion.reportTemplateModelCallback(ReportTemplateModel) to be able to customize this model
		OAObjectCallbackDelegate.onObjectCallbackModel(ReportVersion.class, ReportVersion.P_ReportTemplate, modelReportTemplate);

		return modelReportTemplate;
	}

	public ReportVersionModel getSubReportVersionsModel() {
		if (modelSubReportVersions != null) {
			return modelSubReportVersions;
		}
		modelSubReportVersions = new ReportVersionModel(getSubReportVersions());
		modelSubReportVersions.setDisplayName("Report Version");
		modelSubReportVersions.setPluralDisplayName("Sub Report Versions");
		if (HubDetailDelegate.getIsFromSameMasterHub(getOriginalHub(), getSubReportVersions())) {
			modelSubReportVersions.setCreateUI(false);
		}
		if (!HubDetailDelegate.getIsValidRecursive(getSubReportVersions())) {
			modelSubReportVersions.setCreateUI(false);
		}
		modelSubReportVersions.setForJfc(getForJfc());
		modelSubReportVersions.setAllowNew(false);
		modelSubReportVersions.setAllowSave(true);
		modelSubReportVersions.setAllowAdd(true);
		modelSubReportVersions.setAllowMove(false);
		modelSubReportVersions.setAllowRemove(true);
		modelSubReportVersions.setAllowDelete(false);
		modelSubReportVersions.setAllowRefresh(true);
		modelSubReportVersions.setAllowSearch(false);
		modelSubReportVersions.setAllowHubSearch(true);
		modelSubReportVersions.setAllowGotoEdit(true);
		modelSubReportVersions.setViewOnly(getViewOnly());
		modelSubReportVersions.setAllowTableFilter(true);
		modelSubReportVersions.setAllowTableSorting(true);
		modelSubReportVersions.setAllowMultiSelect(false);
		modelSubReportVersions.setAllowCopy(false);
		modelSubReportVersions.setAllowCut(false);
		modelSubReportVersions.setAllowPaste(false);
		// call ReportVersion.subReportVersionsModelCallback(ReportVersionModel) to be able to customize this model
		OAObjectCallbackDelegate.onObjectCallbackModel(ReportVersion.class, ReportVersion.P_SubReportVersions, modelSubReportVersions);

		return modelSubReportVersions;
	}

	public ReportVersionModel getSubReportVersionsSelectFromModel() {
		if (modelSubReportVersionsSelectFrom != null) {
			return modelSubReportVersionsSelectFrom;
		}
		modelSubReportVersionsSelectFrom = new ReportVersionModel(getSubReportVersionsSelectFromHub());
		modelSubReportVersionsSelectFrom.setDisplayName("Report Version");
		modelSubReportVersionsSelectFrom.setPluralDisplayName("Report Versions");
		modelSubReportVersionsSelectFrom.setForJfc(getForJfc());
		modelSubReportVersionsSelectFrom.setAllowNew(false);
		modelSubReportVersionsSelectFrom.setAllowSave(true);
		modelSubReportVersionsSelectFrom.setAllowAdd(false);
		modelSubReportVersionsSelectFrom.setAllowMove(false);
		modelSubReportVersionsSelectFrom.setAllowRemove(false);
		modelSubReportVersionsSelectFrom.setAllowDelete(false);
		modelSubReportVersionsSelectFrom.setAllowRefresh(true);
		modelSubReportVersionsSelectFrom.setAllowSearch(true);
		modelSubReportVersionsSelectFrom.setAllowHubSearch(true);
		modelSubReportVersionsSelectFrom.setAllowGotoEdit(true);
		modelSubReportVersionsSelectFrom.setViewOnly(getViewOnly());
		modelSubReportVersionsSelectFrom.setAllowNew(false);
		modelSubReportVersionsSelectFrom.setAllowTableFilter(true);
		modelSubReportVersionsSelectFrom.setAllowTableSorting(true);
		modelSubReportVersionsSelectFrom.setAllowCut(false);
		modelSubReportVersionsSelectFrom.setAllowCopy(false);
		modelSubReportVersionsSelectFrom.setAllowPaste(false);
		modelSubReportVersionsSelectFrom.setAllowMultiSelect(true);
		new HubMakeCopy(getSubReportVersions(), modelSubReportVersionsSelectFrom.getMultiSelectHub());
		return modelSubReportVersionsSelectFrom;
	}

	public ReportVersionSearchModel getParentReportVersionSearchModel() {
		if (modelParentReportVersionSearch != null) {
			return modelParentReportVersionSearch;
		}
		modelParentReportVersionSearch = new ReportVersionSearchModel();
		HubSelectDelegate.adoptWhereHub(modelParentReportVersionSearch.getHub(), ReportVersion.P_ParentReportVersion, getHub());
		return modelParentReportVersionSearch;
	}

	public ReportTemplateSearchModel getReportTemplateSearchModel() {
		if (modelReportTemplateSearch != null) {
			return modelReportTemplateSearch;
		}
		modelReportTemplateSearch = new ReportTemplateSearchModel();
		HubSelectDelegate.adoptWhereHub(modelReportTemplateSearch.getHub(), ReportVersion.P_ReportTemplate, getHub());
		return modelReportTemplateSearch;
	}

	public ReportVersionSearchModel getSubReportVersionsSearchModel() {
		if (modelSubReportVersionsSearch != null) {
			return modelSubReportVersionsSearch;
		}
		modelSubReportVersionsSearch = new ReportVersionSearchModel();
		OAFilter filter = new OAInFilter(ReportVersionModel.this.getHub(),
				ReportVersionPP.reportTemplate().report().subReports().reportTemplates().reportVersions().pp);
		modelSubReportVersionsSearch.getReportVersionSearch().setExtraWhereFilter(filter);
		return modelSubReportVersionsSearch;
	}

	public HubCopy<ReportVersion> createHubCopy() {
		Hub<ReportVersion> hubReportVersionx = new Hub<>(ReportVersion.class);
		HubCopy<ReportVersion> hc = new HubCopy<>(getHub(), hubReportVersionx, true);
		return hc;
	}

	public ReportVersionModel createCopy() {
		ReportVersionModel mod = new ReportVersionModel(createHubCopy().getHub());
		return mod;
	}
}
