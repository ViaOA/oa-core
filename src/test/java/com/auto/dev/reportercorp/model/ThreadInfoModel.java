package com.auto.dev.reportercorp.model;

import java.util.logging.Logger;

import com.auto.dev.reportercorp.delegate.ModelDelegate;
import com.auto.dev.reportercorp.model.oa.ReportInstanceProcess;
import com.auto.dev.reportercorp.model.oa.ReporterCorp;
import com.auto.dev.reportercorp.model.oa.StatusInfo;
import com.auto.dev.reportercorp.model.oa.ThreadInfo;
import com.auto.dev.reportercorp.model.oa.propertypath.ReportInstanceProcessPP;
import com.auto.dev.reportercorp.model.oa.propertypath.ThreadInfoPP;
import com.auto.dev.reportercorp.model.search.ReportInstanceProcessSearchModel;
import com.auto.dev.reportercorp.model.search.ReporterCorpSearchModel;
import com.viaoa.filter.OAEqualPathFilter;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubCombined;
import com.viaoa.hub.HubCopy;
import com.viaoa.hub.HubDelegate;
import com.viaoa.hub.HubDetailDelegate;
import com.viaoa.hub.HubSelectDelegate;
import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObjectCallbackDelegate;
import com.viaoa.object.OAObjectModel;
import com.viaoa.util.OAFilter;

public class ThreadInfoModel extends OAObjectModel {
	private static Logger LOG = Logger.getLogger(ThreadInfoModel.class.getName());

	// Hubs
	protected Hub<ThreadInfo> hub;
	// selected threadInfos
	protected Hub<ThreadInfo> hubMultiSelect;
	// detail hubs
	protected Hub<ReporterCorp> hubReporterCorp;
	protected Hub<StatusInfo> hubStatusInfo;
	protected Hub<ReportInstanceProcess> hubReportInstanceProcesses;

	// AddHubs used for references
	protected Hub<ReporterCorp> hubReporterCorpSelectFrom;

	// ObjectModels
	protected ReporterCorpModel modelReporterCorp;
	protected StatusInfoModel modelStatusInfo;
	protected ReportInstanceProcessModel modelReportInstanceProcesses;

	// selectFrom
	protected ReporterCorpModel modelReporterCorpSelectFrom;

	// SearchModels used for references
	protected ReporterCorpSearchModel modelReporterCorpSearch;
	protected ReportInstanceProcessSearchModel modelReportInstanceProcessesSearch;

	public ThreadInfoModel() {
		setDisplayName("Thread Info");
		setPluralDisplayName("Thread Infos");
	}

	public ThreadInfoModel(Hub<ThreadInfo> hubThreadInfo) {
		this();
		if (hubThreadInfo != null) {
			HubDelegate.setObjectClass(hubThreadInfo, ThreadInfo.class);
		}
		this.hub = hubThreadInfo;
	}

	public ThreadInfoModel(ThreadInfo threadInfo) {
		this();
		getHub().add(threadInfo);
		getHub().setPos(0);
	}

	public Hub<ThreadInfo> getOriginalHub() {
		return getHub();
	}

	public Hub<ReporterCorp> getReporterCorpHub() {
		if (hubReporterCorp != null) {
			return hubReporterCorp;
		}
		// this is the owner, use detailHub
		hubReporterCorp = getHub().getDetailHub(ThreadInfo.P_ReporterCorp);
		return hubReporterCorp;
	}

	public Hub<StatusInfo> getStatusInfoHub() {
		if (hubStatusInfo != null) {
			return hubStatusInfo;
		}
		hubStatusInfo = getHub().getDetailHub(ThreadInfo.P_StatusInfo);
		return hubStatusInfo;
	}

	public Hub<ReportInstanceProcess> getReportInstanceProcesses() {
		if (hubReportInstanceProcesses == null) {
			hubReportInstanceProcesses = getHub().getDetailHub(ThreadInfo.P_ReportInstanceProcesses);
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
		hubReporterCorpSelectFrom.setLinkHub(getHub(), ThreadInfo.P_ReporterCorp);
		return hubReporterCorpSelectFrom;
	}

	public ThreadInfo getThreadInfo() {
		return getHub().getAO();
	}

	public Hub<ThreadInfo> getHub() {
		if (hub == null) {
			hub = new Hub<ThreadInfo>(ThreadInfo.class);
		}
		return hub;
	}

	public Hub<ThreadInfo> getMultiSelectHub() {
		if (hubMultiSelect == null) {
			hubMultiSelect = new Hub<ThreadInfo>(ThreadInfo.class);
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
		modelReporterCorp.setCreateUI(li == null || !ThreadInfo.P_ReporterCorp.equalsIgnoreCase(li.getName()));
		modelReporterCorp.setViewOnly(getViewOnly());
		// call ThreadInfo.reporterCorpModelCallback(ReporterCorpModel) to be able to customize this model
		OAObjectCallbackDelegate.onObjectCallbackModel(ThreadInfo.class, ThreadInfo.P_ReporterCorp, modelReporterCorp);

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
		// call ThreadInfo.statusInfoModelCallback(StatusInfoModel) to be able to customize this model
		OAObjectCallbackDelegate.onObjectCallbackModel(ThreadInfo.class, ThreadInfo.P_StatusInfo, modelStatusInfo);

		return modelStatusInfo;
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
		modelReportInstanceProcesses.setAllowNew(false);
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
		// call ThreadInfo.reportInstanceProcessesModelCallback(ReportInstanceProcessModel) to be able to customize this model
		OAObjectCallbackDelegate.onObjectCallbackModel(	ThreadInfo.class, ThreadInfo.P_ReportInstanceProcesses,
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
		HubSelectDelegate.adoptWhereHub(modelReporterCorpSearch.getHub(), ThreadInfo.P_ReporterCorp, getHub());
		return modelReporterCorpSearch;
	}

	public ReportInstanceProcessSearchModel getReportInstanceProcessesSearchModel() {
		if (modelReportInstanceProcessesSearch != null) {
			return modelReportInstanceProcessesSearch;
		}
		modelReportInstanceProcessesSearch = new ReportInstanceProcessSearchModel();
		OAFilter filter = new OAEqualPathFilter(ThreadInfoModel.this.getHub(),
				ReportInstanceProcessPP.reportInstanceProcessorInfo().reporterCorp().pp, ThreadInfoPP.reporterCorp().pp);
		modelReportInstanceProcessesSearch.getReportInstanceProcessSearch().setExtraWhereFilter(filter);
		return modelReportInstanceProcessesSearch;
	}

	public HubCopy<ThreadInfo> createHubCopy() {
		Hub<ThreadInfo> hubThreadInfox = new Hub<>(ThreadInfo.class);
		HubCopy<ThreadInfo> hc = new HubCopy<>(getHub(), hubThreadInfox, true);
		return hc;
	}

	public ThreadInfoModel createCopy() {
		ThreadInfoModel mod = new ThreadInfoModel(createHubCopy().getHub());
		return mod;
	}
}
