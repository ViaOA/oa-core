package com.auto.dev.reportercorp.model;

import java.util.logging.Logger;

import com.auto.dev.reportercorp.delegate.ModelDelegate;
import com.auto.dev.reportercorp.model.oa.ReportInfo;
import com.auto.dev.reportercorp.model.oa.ReportInstanceProcess;
import com.auto.dev.reportercorp.model.oa.ReportInstanceProcessorInfo;
import com.auto.dev.reportercorp.model.oa.ReporterCorp;
import com.auto.dev.reportercorp.model.oa.StatusInfo;
import com.auto.dev.reportercorp.model.search.ReportInstanceProcessSearchModel;
import com.auto.dev.reportercorp.model.search.ReporterCorpSearchModel;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubCombined;
import com.viaoa.hub.HubCopy;
import com.viaoa.hub.HubDelegate;
import com.viaoa.hub.HubDetailDelegate;
import com.viaoa.hub.HubSelectDelegate;
import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObjectCallbackDelegate;
import com.viaoa.object.OAObjectModel;

public class ReportInstanceProcessorInfoModel extends OAObjectModel {
	private static Logger LOG = Logger.getLogger(ReportInstanceProcessorInfoModel.class.getName());

	// Hubs
	protected Hub<ReportInstanceProcessorInfo> hub;
	// selected reportInstanceProcessorInfos
	protected Hub<ReportInstanceProcessorInfo> hubMultiSelect;
	// detail hubs
	protected Hub<ReporterCorp> hubReporterCorp;
	protected Hub<StatusInfo> hubStatusInfo;
	protected Hub<ReportInfo> hubReportInfos;
	protected Hub<ReportInstanceProcess> hubReportInstanceProcesses;

	// AddHubs used for references
	protected Hub<ReporterCorp> hubReporterCorpSelectFrom;

	// ObjectModels
	protected ReporterCorpModel modelReporterCorp;
	protected StatusInfoModel modelStatusInfo;
	protected ReportInfoModel modelReportInfos;
	protected ReportInstanceProcessModel modelReportInstanceProcesses;

	// selectFrom
	protected ReporterCorpModel modelReporterCorpSelectFrom;

	// SearchModels used for references
	protected ReporterCorpSearchModel modelReporterCorpSearch;
	protected ReportInstanceProcessSearchModel modelReportInstanceProcessesSearch;

	public ReportInstanceProcessorInfoModel() {
		setDisplayName("Report Instance Processor Info");
		setPluralDisplayName("Report Instance Processor Infos");
	}

	public ReportInstanceProcessorInfoModel(Hub<ReportInstanceProcessorInfo> hubReportInstanceProcessorInfo) {
		this();
		if (hubReportInstanceProcessorInfo != null) {
			HubDelegate.setObjectClass(hubReportInstanceProcessorInfo, ReportInstanceProcessorInfo.class);
		}
		this.hub = hubReportInstanceProcessorInfo;
	}

	public ReportInstanceProcessorInfoModel(ReportInstanceProcessorInfo reportInstanceProcessorInfo) {
		this();
		getHub().add(reportInstanceProcessorInfo);
		getHub().setPos(0);
	}

	public Hub<ReportInstanceProcessorInfo> getOriginalHub() {
		return getHub();
	}

	public Hub<ReporterCorp> getReporterCorpHub() {
		if (hubReporterCorp != null) {
			return hubReporterCorp;
		}
		// this is the owner, use detailHub
		hubReporterCorp = getHub().getDetailHub(ReportInstanceProcessorInfo.P_ReporterCorp);
		return hubReporterCorp;
	}

	public Hub<StatusInfo> getStatusInfoHub() {
		if (hubStatusInfo != null) {
			return hubStatusInfo;
		}
		hubStatusInfo = getHub().getDetailHub(ReportInstanceProcessorInfo.P_StatusInfo);
		return hubStatusInfo;
	}

	public Hub<ReportInfo> getReportInfos() {
		if (hubReportInfos == null) {
			hubReportInfos = getHub().getDetailHub(ReportInstanceProcessorInfo.P_ReportInfos);
		}
		return hubReportInfos;
	}

	public Hub<ReportInstanceProcess> getReportInstanceProcesses() {
		if (hubReportInstanceProcesses == null) {
			hubReportInstanceProcesses = getHub().getDetailHub(ReportInstanceProcessorInfo.P_ReportInstanceProcesses);
		}
		return hubReportInstanceProcesses;
	}

	public Hub<ReporterCorp> getReporterCorpSelectFromHub() {
		if (hubReporterCorpSelectFrom != null) {
			return hubReporterCorpSelectFrom;
		}
		hubReporterCorpSelectFrom = new Hub<ReporterCorp>(ReporterCorp.class);
		Hub<ReporterCorp> hubReporterCorpSelectFrom1 = ModelDelegate.getReporterCorps().createSharedHub();
		HubCombined<ReporterCorp> hubCombined = new HubCombined(hubReporterCorpSelectFrom, hubReporterCorpSelectFrom1,
				getReporterCorpHub());
		hubReporterCorpSelectFrom.setLinkHub(getHub(), ReportInstanceProcessorInfo.P_ReporterCorp);
		return hubReporterCorpSelectFrom;
	}

	public ReportInstanceProcessorInfo getReportInstanceProcessorInfo() {
		return getHub().getAO();
	}

	public Hub<ReportInstanceProcessorInfo> getHub() {
		if (hub == null) {
			hub = new Hub<ReportInstanceProcessorInfo>(ReportInstanceProcessorInfo.class);
		}
		return hub;
	}

	public Hub<ReportInstanceProcessorInfo> getMultiSelectHub() {
		if (hubMultiSelect == null) {
			hubMultiSelect = new Hub<ReportInstanceProcessorInfo>(ReportInstanceProcessorInfo.class);
		}
		return hubMultiSelect;
	}

	public ReporterCorpModel getReporterCorpModel() {
		if (modelReporterCorp != null) {
			return modelReporterCorp;
		}
		modelReporterCorp = new ReporterCorpModel(getReporterCorpHub());
		modelReporterCorp.setDisplayName("Reporter Corp");
		modelReporterCorp.setPluralDisplayName("Reporter Corps");
		modelReporterCorp.setForJfc(getForJfc());
		modelReporterCorp.setAllowNew(false);
		modelReporterCorp.setAllowSave(true);
		modelReporterCorp.setAllowAdd(false);
		modelReporterCorp.setAllowRemove(false);
		modelReporterCorp.setAllowClear(false);
		modelReporterCorp.setAllowDelete(false);
		modelReporterCorp.setAllowSearch(false);
		modelReporterCorp.setAllowHubSearch(true);
		modelReporterCorp.setAllowGotoEdit(true);
		OALinkInfo li = HubDetailDelegate.getLinkInfoFromDetailToMaster(getOriginalHub());
		modelReporterCorp.setCreateUI(li == null || !ReportInstanceProcessorInfo.P_ReporterCorp.equalsIgnoreCase(li.getName()));
		modelReporterCorp.setViewOnly(getViewOnly());
		// call ReportInstanceProcessorInfo.reporterCorpModelCallback(ReporterCorpModel) to be able to customize this model
		OAObjectCallbackDelegate.onObjectCallbackModel(	ReportInstanceProcessorInfo.class, ReportInstanceProcessorInfo.P_ReporterCorp,
														modelReporterCorp);

		return modelReporterCorp;
	}

	public StatusInfoModel getStatusInfoModel() {
		if (modelStatusInfo != null) {
			return modelStatusInfo;
		}
		modelStatusInfo = new StatusInfoModel(getStatusInfoHub());
		modelStatusInfo.setDisplayName("Status Info");
		modelStatusInfo.setPluralDisplayName("Status Infos");
		modelStatusInfo.setForJfc(getForJfc());
		modelStatusInfo.setAllowNew(false);
		modelStatusInfo.setAllowSave(true);
		modelStatusInfo.setAllowAdd(false);
		modelStatusInfo.setAllowRemove(false);
		modelStatusInfo.setAllowClear(false);
		modelStatusInfo.setAllowDelete(false);
		modelStatusInfo.setAllowSearch(false);
		modelStatusInfo.setAllowHubSearch(false);
		modelStatusInfo.setAllowGotoEdit(true);
		modelStatusInfo.setViewOnly(getViewOnly());
		// call ReportInstanceProcessorInfo.statusInfoModelCallback(StatusInfoModel) to be able to customize this model
		OAObjectCallbackDelegate.onObjectCallbackModel(	ReportInstanceProcessorInfo.class, ReportInstanceProcessorInfo.P_StatusInfo,
														modelStatusInfo);

		return modelStatusInfo;
	}

	public ReportInfoModel getReportInfosModel() {
		if (modelReportInfos != null) {
			return modelReportInfos;
		}
		modelReportInfos = new ReportInfoModel(getReportInfos());
		modelReportInfos.setDisplayName("Report Info");
		modelReportInfos.setPluralDisplayName("Report Infos");
		if (HubDetailDelegate.getIsFromSameMasterHub(getOriginalHub(), getReportInfos())) {
			modelReportInfos.setCreateUI(false);
		}
		modelReportInfos.setForJfc(getForJfc());
		modelReportInfos.setAllowNew(true);
		modelReportInfos.setAllowSave(true);
		modelReportInfos.setAllowAdd(false);
		modelReportInfos.setAllowMove(false);
		modelReportInfos.setAllowRemove(false);
		modelReportInfos.setAllowDelete(true);
		modelReportInfos.setAllowRefresh(false);
		modelReportInfos.setAllowSearch(false);
		modelReportInfos.setAllowHubSearch(false);
		modelReportInfos.setAllowGotoEdit(true);
		modelReportInfos.setViewOnly(getViewOnly());
		modelReportInfos.setAllowTableFilter(true);
		modelReportInfos.setAllowTableSorting(true);
		modelReportInfos.setAllowMultiSelect(false);
		modelReportInfos.setAllowCopy(false);
		modelReportInfos.setAllowCut(false);
		modelReportInfos.setAllowPaste(false);
		// call ReportInstanceProcessorInfo.reportInfosModelCallback(ReportInfoModel) to be able to customize this model
		OAObjectCallbackDelegate.onObjectCallbackModel(	ReportInstanceProcessorInfo.class, ReportInstanceProcessorInfo.P_ReportInfos,
														modelReportInfos);

		return modelReportInfos;
	}

	public ReportInstanceProcessModel getReportInstanceProcessesModel() {
		if (modelReportInstanceProcesses != null) {
			return modelReportInstanceProcesses;
		}
		modelReportInstanceProcesses = new ReportInstanceProcessModel(getReportInstanceProcesses());
		modelReportInstanceProcesses.setDisplayName("Report Instance Process");
		modelReportInstanceProcesses.setPluralDisplayName("Report Instance Processes");
		if (HubDetailDelegate.getIsFromSameMasterHub(getOriginalHub(), getReportInstanceProcesses())) {
			modelReportInstanceProcesses.setCreateUI(false);
		}
		modelReportInstanceProcesses.setForJfc(getForJfc());
		modelReportInstanceProcesses.setAllowNew(true);
		modelReportInstanceProcesses.setAllowSave(true);
		modelReportInstanceProcesses.setAllowAdd(false);
		modelReportInstanceProcesses.setAllowMove(false);
		modelReportInstanceProcesses.setAllowRemove(false);
		modelReportInstanceProcesses.setAllowDelete(true);
		modelReportInstanceProcesses.setAllowRefresh(false);
		modelReportInstanceProcesses.setAllowSearch(false);
		modelReportInstanceProcesses.setAllowHubSearch(true);
		modelReportInstanceProcesses.setAllowGotoEdit(true);
		modelReportInstanceProcesses.setViewOnly(getViewOnly());
		modelReportInstanceProcesses.setAllowTableFilter(true);
		modelReportInstanceProcesses.setAllowTableSorting(true);
		modelReportInstanceProcesses.setAllowMultiSelect(false);
		modelReportInstanceProcesses.setAllowCopy(false);
		modelReportInstanceProcesses.setAllowCut(false);
		modelReportInstanceProcesses.setAllowPaste(false);
		// call ReportInstanceProcessorInfo.reportInstanceProcessesModelCallback(ReportInstanceProcessModel) to be able to customize this model
		OAObjectCallbackDelegate.onObjectCallbackModel(	ReportInstanceProcessorInfo.class,
														ReportInstanceProcessorInfo.P_ReportInstanceProcesses,
														modelReportInstanceProcesses);

		return modelReportInstanceProcesses;
	}

	public ReporterCorpModel getReporterCorpSelectFromModel() {
		if (modelReporterCorpSelectFrom != null) {
			return modelReporterCorpSelectFrom;
		}
		modelReporterCorpSelectFrom = new ReporterCorpModel(getReporterCorpSelectFromHub());
		modelReporterCorpSelectFrom.setDisplayName("Reporter Corp");
		modelReporterCorpSelectFrom.setPluralDisplayName("Reporter Corps");
		modelReporterCorpSelectFrom.setForJfc(getForJfc());
		modelReporterCorpSelectFrom.setAllowNew(false);
		modelReporterCorpSelectFrom.setAllowSave(true);
		modelReporterCorpSelectFrom.setAllowAdd(false);
		modelReporterCorpSelectFrom.setAllowMove(false);
		modelReporterCorpSelectFrom.setAllowRemove(false);
		modelReporterCorpSelectFrom.setAllowDelete(false);
		modelReporterCorpSelectFrom.setAllowSearch(false);
		modelReporterCorpSelectFrom.setAllowHubSearch(true);
		modelReporterCorpSelectFrom.setAllowGotoEdit(true);
		modelReporterCorpSelectFrom.setViewOnly(getViewOnly());
		modelReporterCorpSelectFrom.setAllowNew(false);
		modelReporterCorpSelectFrom.setAllowTableFilter(true);
		modelReporterCorpSelectFrom.setAllowTableSorting(true);
		modelReporterCorpSelectFrom.setAllowCut(false);
		modelReporterCorpSelectFrom.setAllowCopy(false);
		modelReporterCorpSelectFrom.setAllowPaste(false);
		modelReporterCorpSelectFrom.setAllowMultiSelect(false);
		return modelReporterCorpSelectFrom;
	}

	public ReporterCorpSearchModel getReporterCorpSearchModel() {
		if (modelReporterCorpSearch != null) {
			return modelReporterCorpSearch;
		}
		modelReporterCorpSearch = new ReporterCorpSearchModel();
		HubSelectDelegate.adoptWhereHub(modelReporterCorpSearch.getHub(), ReportInstanceProcessorInfo.P_ReporterCorp, getHub());
		return modelReporterCorpSearch;
	}

	public ReportInstanceProcessSearchModel getReportInstanceProcessesSearchModel() {
		if (modelReportInstanceProcessesSearch != null) {
			return modelReportInstanceProcessesSearch;
		}
		modelReportInstanceProcessesSearch = new ReportInstanceProcessSearchModel();
		return modelReportInstanceProcessesSearch;
	}

	public HubCopy<ReportInstanceProcessorInfo> createHubCopy() {
		Hub<ReportInstanceProcessorInfo> hubReportInstanceProcessorInfox = new Hub<>(ReportInstanceProcessorInfo.class);
		HubCopy<ReportInstanceProcessorInfo> hc = new HubCopy<>(getHub(), hubReportInstanceProcessorInfox, true);
		return hc;
	}

	public ReportInstanceProcessorInfoModel createCopy() {
		ReportInstanceProcessorInfoModel mod = new ReportInstanceProcessorInfoModel(createHubCopy().getHub());
		return mod;
	}
}
