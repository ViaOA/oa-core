package com.auto.dev.reportercorp.model;

import java.util.logging.Logger;

import com.auto.dev.reportercorp.model.oa.PypeReportMessage;
import com.auto.dev.reportercorp.model.oa.ReportInfo;
import com.auto.dev.reportercorp.model.oa.ReportInstanceProcess;
import com.auto.dev.reportercorp.model.oa.ReportInstanceProcessStep;
import com.auto.dev.reportercorp.model.oa.ReportInstanceProcessorInfo;
import com.auto.dev.reportercorp.model.oa.StatusInfo;
import com.auto.dev.reportercorp.model.oa.StoreInfo;
import com.auto.dev.reportercorp.model.oa.ThreadInfo;
import com.auto.dev.reportercorp.model.oa.propertypath.ReportInfoPP;
import com.auto.dev.reportercorp.model.oa.propertypath.ReportInstanceProcessPP;
import com.auto.dev.reportercorp.model.oa.propertypath.StoreInfoPP;
import com.auto.dev.reportercorp.model.oa.propertypath.ThreadInfoPP;
import com.auto.dev.reportercorp.model.search.PypeReportMessageSearchModel;
import com.auto.dev.reportercorp.model.search.ReportInfoSearchModel;
import com.auto.dev.reportercorp.model.search.StoreInfoSearchModel;
import com.auto.dev.reportercorp.model.search.ThreadInfoSearchModel;
import com.viaoa.filter.OAAndFilter;
import com.viaoa.filter.OAEqualPathFilter;
import com.viaoa.filter.OAInFilter;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubCombined;
import com.viaoa.hub.HubCopy;
import com.viaoa.hub.HubDelegate;
import com.viaoa.hub.HubDetailDelegate;
import com.viaoa.hub.HubMerger;
import com.viaoa.hub.HubSelectDelegate;
import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObjectCallbackDelegate;
import com.viaoa.object.OAObjectModel;
import com.viaoa.util.OAFilter;

public class ReportInstanceProcessModel extends OAObjectModel {
	private static Logger LOG = Logger.getLogger(ReportInstanceProcessModel.class.getName());

	// Hubs
	protected Hub<ReportInstanceProcess> hub;
	// selected reportInstanceProcesses
	protected Hub<ReportInstanceProcess> hubMultiSelect;
	// detail hubs
	protected Hub<PypeReportMessage> hubPypeReportMessage;
	protected Hub<ReportInfo> hubReportInfo;
	protected Hub<ReportInstanceProcessorInfo> hubReportInstanceProcessorInfo;
	protected Hub<StatusInfo> hubStatusInfo;
	protected Hub<StoreInfo> hubStoreInfo;
	protected Hub<ThreadInfo> hubThreadInfo;
	protected Hub<ReportInstanceProcessStep> hubReportInstanceProcessSteps;

	// AddHubs used for references
	protected Hub<ReportInfo> hubReportInfoSelectFrom;
	protected Hub<StoreInfo> hubStoreInfoSelectFrom;
	protected Hub<ThreadInfo> hubThreadInfoSelectFrom;

	// ObjectModels
	protected PypeReportMessageModel modelPypeReportMessage;
	protected ReportInfoModel modelReportInfo;
	protected ReportInstanceProcessorInfoModel modelReportInstanceProcessorInfo;
	protected StatusInfoModel modelStatusInfo;
	protected StoreInfoModel modelStoreInfo;
	protected ThreadInfoModel modelThreadInfo;
	protected ReportInstanceProcessStepModel modelReportInstanceProcessSteps;

	// selectFrom
	protected ReportInfoModel modelReportInfoSelectFrom;
	protected StoreInfoModel modelStoreInfoSelectFrom;
	protected ThreadInfoModel modelThreadInfoSelectFrom;

	// SearchModels used for references
	protected PypeReportMessageSearchModel modelPypeReportMessageSearch;
	protected ReportInfoSearchModel modelReportInfoSearch;
	protected StoreInfoSearchModel modelStoreInfoSearch;
	protected ThreadInfoSearchModel modelThreadInfoSearch;

	public ReportInstanceProcessModel() {
		setDisplayName("Report Instance Process");
		setPluralDisplayName("Report Instance Processes");
	}

	public ReportInstanceProcessModel(Hub<ReportInstanceProcess> hubReportInstanceProcess) {
		this();
		if (hubReportInstanceProcess != null) {
			HubDelegate.setObjectClass(hubReportInstanceProcess, ReportInstanceProcess.class);
		}
		this.hub = hubReportInstanceProcess;
	}

	public ReportInstanceProcessModel(ReportInstanceProcess reportInstanceProcess) {
		this();
		getHub().add(reportInstanceProcess);
		getHub().setPos(0);
	}

	public Hub<ReportInstanceProcess> getOriginalHub() {
		return getHub();
	}

	public Hub<PypeReportMessage> getPypeReportMessageHub() {
		if (hubPypeReportMessage != null) {
			return hubPypeReportMessage;
		}
		hubPypeReportMessage = getHub().getDetailHub(ReportInstanceProcess.P_PypeReportMessage);
		return hubPypeReportMessage;
	}

	public Hub<ReportInfo> getReportInfoHub() {
		if (hubReportInfo != null) {
			return hubReportInfo;
		}
		hubReportInfo = getHub().getDetailHub(ReportInstanceProcess.P_ReportInfo);
		return hubReportInfo;
	}

	public Hub<ReportInstanceProcessorInfo> getReportInstanceProcessorInfoHub() {
		if (hubReportInstanceProcessorInfo != null) {
			return hubReportInstanceProcessorInfo;
		}
		// this is the owner, use detailHub
		hubReportInstanceProcessorInfo = getHub().getDetailHub(ReportInstanceProcess.P_ReportInstanceProcessorInfo);
		return hubReportInstanceProcessorInfo;
	}

	public Hub<StatusInfo> getStatusInfoHub() {
		if (hubStatusInfo != null) {
			return hubStatusInfo;
		}
		hubStatusInfo = getHub().getDetailHub(ReportInstanceProcess.P_StatusInfo);
		return hubStatusInfo;
	}

	public Hub<StoreInfo> getStoreInfoHub() {
		if (hubStoreInfo != null) {
			return hubStoreInfo;
		}
		hubStoreInfo = getHub().getDetailHub(ReportInstanceProcess.P_StoreInfo);
		return hubStoreInfo;
	}

	public Hub<ThreadInfo> getThreadInfoHub() {
		if (hubThreadInfo != null) {
			return hubThreadInfo;
		}
		hubThreadInfo = getHub().getDetailHub(ReportInstanceProcess.P_ThreadInfo);
		return hubThreadInfo;
	}

	public Hub<ReportInstanceProcessStep> getReportInstanceProcessSteps() {
		if (hubReportInstanceProcessSteps == null) {
			hubReportInstanceProcessSteps = getHub().getDetailHub(ReportInstanceProcess.P_ReportInstanceProcessSteps);
		}
		return hubReportInstanceProcessSteps;
	}

	public Hub<ReportInfo> getReportInfoSelectFromHub() {
		if (hubReportInfoSelectFrom != null) {
			return hubReportInfoSelectFrom;
		}
		hubReportInfoSelectFrom = new Hub<ReportInfo>(ReportInfo.class);
		Hub<ReportInfo> hubReportInfoSelectFrom1 = new Hub<ReportInfo>(ReportInfo.class);
		new HubMerger(getHub(), hubReportInfoSelectFrom1, ReportInstanceProcessPP.reportInstanceProcessorInfo().reportInfos().pp, false);
		HubCombined<ReportInfo> hubCombined = new HubCombined(hubReportInfoSelectFrom, hubReportInfoSelectFrom1, getReportInfoHub());
		hubReportInfoSelectFrom.setLinkHub(getHub(), ReportInstanceProcess.P_ReportInfo);
		return hubReportInfoSelectFrom;
	}

	public Hub<StoreInfo> getStoreInfoSelectFromHub() {
		if (hubStoreInfoSelectFrom != null) {
			return hubStoreInfoSelectFrom;
		}
		hubStoreInfoSelectFrom = new Hub<StoreInfo>(StoreInfo.class);
		Hub<StoreInfo> hubStoreInfoSelectFrom1 = new Hub<StoreInfo>(StoreInfo.class);
		new HubMerger(getHub(), hubStoreInfoSelectFrom1,
				ReportInstanceProcessPP.reportInstanceProcessorInfo().reporterCorp().storeInfos().pp, false);
		HubCombined<StoreInfo> hubCombined = new HubCombined(hubStoreInfoSelectFrom, hubStoreInfoSelectFrom1, getStoreInfoHub());
		hubStoreInfoSelectFrom.setLinkHub(getHub(), ReportInstanceProcess.P_StoreInfo);
		return hubStoreInfoSelectFrom;
	}

	public Hub<ThreadInfo> getThreadInfoSelectFromHub() {
		if (hubThreadInfoSelectFrom != null) {
			return hubThreadInfoSelectFrom;
		}
		hubThreadInfoSelectFrom = new Hub<ThreadInfo>(ThreadInfo.class);
		Hub<ThreadInfo> hubThreadInfoSelectFrom1 = new Hub<ThreadInfo>(ThreadInfo.class);
		new HubMerger(getHub(), hubThreadInfoSelectFrom1,
				ReportInstanceProcessPP.reportInstanceProcessorInfo().reporterCorp().threadInfos().pp, false);
		HubCombined<ThreadInfo> hubCombined = new HubCombined(hubThreadInfoSelectFrom, hubThreadInfoSelectFrom1, getThreadInfoHub());
		hubThreadInfoSelectFrom.setLinkHub(getHub(), ReportInstanceProcess.P_ThreadInfo);
		return hubThreadInfoSelectFrom;
	}

	public ReportInstanceProcess getReportInstanceProcess() {
		return getHub().getAO();
	}

	public Hub<ReportInstanceProcess> getHub() {
		if (hub == null) {
			hub = new Hub<ReportInstanceProcess>(ReportInstanceProcess.class);
		}
		return hub;
	}

	public Hub<ReportInstanceProcess> getMultiSelectHub() {
		if (hubMultiSelect == null) {
			hubMultiSelect = new Hub<ReportInstanceProcess>(ReportInstanceProcess.class);
		}
		return hubMultiSelect;
	}

	public PypeReportMessageModel getPypeReportMessageModel() {
		if (modelPypeReportMessage != null) {
			return modelPypeReportMessage;
		}
		modelPypeReportMessage = new PypeReportMessageModel(getPypeReportMessageHub());
		modelPypeReportMessage.setDisplayName("Pype Report Message");
		modelPypeReportMessage.setPluralDisplayName("Pype Report Messages");
		modelPypeReportMessage.setForJfc(getForJfc());
		modelPypeReportMessage.setAllowNew(true);
		modelPypeReportMessage.setAllowSave(true);
		modelPypeReportMessage.setAllowAdd(false);
		modelPypeReportMessage.setAllowRemove(true);
		modelPypeReportMessage.setAllowClear(true);
		modelPypeReportMessage.setAllowDelete(false);
		modelPypeReportMessage.setAllowSearch(true);
		modelPypeReportMessage.setAllowHubSearch(true);
		modelPypeReportMessage.setAllowGotoEdit(true);
		modelPypeReportMessage.setViewOnly(getViewOnly());
		// call ReportInstanceProcess.pypeReportMessageModelCallback(PypeReportMessageModel) to be able to customize this model
		OAObjectCallbackDelegate.onObjectCallbackModel(	ReportInstanceProcess.class, ReportInstanceProcess.P_PypeReportMessage,
														modelPypeReportMessage);

		return modelPypeReportMessage;
	}

	public ReportInfoModel getReportInfoModel() {
		if (modelReportInfo != null) {
			return modelReportInfo;
		}
		modelReportInfo = new ReportInfoModel(getReportInfoHub());
		modelReportInfo.setDisplayName("Report Info");
		modelReportInfo.setPluralDisplayName("Report Infos");
		modelReportInfo.setForJfc(getForJfc());
		modelReportInfo.setAllowNew(false);
		modelReportInfo.setAllowSave(true);
		modelReportInfo.setAllowAdd(false);
		modelReportInfo.setAllowRemove(true);
		modelReportInfo.setAllowClear(true);
		modelReportInfo.setAllowDelete(false);
		modelReportInfo.setAllowSearch(true);
		modelReportInfo.setAllowHubSearch(false);
		modelReportInfo.setAllowGotoEdit(true);
		modelReportInfo.setViewOnly(true);
		// call ReportInstanceProcess.reportInfoModelCallback(ReportInfoModel) to be able to customize this model
		OAObjectCallbackDelegate.onObjectCallbackModel(ReportInstanceProcess.class, ReportInstanceProcess.P_ReportInfo, modelReportInfo);

		return modelReportInfo;
	}

	public ReportInstanceProcessorInfoModel getReportInstanceProcessorInfoModel() {
		if (modelReportInstanceProcessorInfo != null) {
			return modelReportInstanceProcessorInfo;
		}
		modelReportInstanceProcessorInfo = new ReportInstanceProcessorInfoModel(getReportInstanceProcessorInfoHub());
		modelReportInstanceProcessorInfo.setDisplayName("Report Instance Processor Info");
		modelReportInstanceProcessorInfo.setPluralDisplayName("Report Instance Processor Infos");
		modelReportInstanceProcessorInfo.setForJfc(getForJfc());
		modelReportInstanceProcessorInfo.setAllowNew(false);
		modelReportInstanceProcessorInfo.setAllowSave(true);
		modelReportInstanceProcessorInfo.setAllowAdd(false);
		modelReportInstanceProcessorInfo.setAllowRemove(false);
		modelReportInstanceProcessorInfo.setAllowClear(false);
		modelReportInstanceProcessorInfo.setAllowDelete(false);
		modelReportInstanceProcessorInfo.setAllowSearch(false);
		modelReportInstanceProcessorInfo.setAllowHubSearch(false);
		modelReportInstanceProcessorInfo.setAllowGotoEdit(true);
		OALinkInfo li = HubDetailDelegate.getLinkInfoFromDetailToMaster(getOriginalHub());
		modelReportInstanceProcessorInfo
				.setCreateUI(li == null || !ReportInstanceProcess.P_ReportInstanceProcessorInfo.equalsIgnoreCase(li.getName()));
		modelReportInstanceProcessorInfo.setViewOnly(getViewOnly());
		// call ReportInstanceProcess.reportInstanceProcessorInfoModelCallback(ReportInstanceProcessorInfoModel) to be able to customize this model
		OAObjectCallbackDelegate.onObjectCallbackModel(	ReportInstanceProcess.class, ReportInstanceProcess.P_ReportInstanceProcessorInfo,
														modelReportInstanceProcessorInfo);

		return modelReportInstanceProcessorInfo;
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
		// call ReportInstanceProcess.statusInfoModelCallback(StatusInfoModel) to be able to customize this model
		OAObjectCallbackDelegate.onObjectCallbackModel(ReportInstanceProcess.class, ReportInstanceProcess.P_StatusInfo, modelStatusInfo);

		return modelStatusInfo;
	}

	public StoreInfoModel getStoreInfoModel() {
		if (modelStoreInfo != null) {
			return modelStoreInfo;
		}
		modelStoreInfo = new StoreInfoModel(getStoreInfoHub());
		modelStoreInfo.setDisplayName("Store Info");
		modelStoreInfo.setPluralDisplayName("Store Infos");
		modelStoreInfo.setForJfc(getForJfc());
		modelStoreInfo.setAllowNew(false);
		modelStoreInfo.setAllowSave(true);
		modelStoreInfo.setAllowAdd(false);
		modelStoreInfo.setAllowRemove(true);
		modelStoreInfo.setAllowClear(true);
		modelStoreInfo.setAllowDelete(false);
		modelStoreInfo.setAllowSearch(true);
		modelStoreInfo.setAllowHubSearch(false);
		modelStoreInfo.setAllowGotoEdit(true);
		modelStoreInfo.setViewOnly(true);
		// call ReportInstanceProcess.storeInfoModelCallback(StoreInfoModel) to be able to customize this model
		OAObjectCallbackDelegate.onObjectCallbackModel(ReportInstanceProcess.class, ReportInstanceProcess.P_StoreInfo, modelStoreInfo);

		return modelStoreInfo;
	}

	public ThreadInfoModel getThreadInfoModel() {
		if (modelThreadInfo != null) {
			return modelThreadInfo;
		}
		modelThreadInfo = new ThreadInfoModel(getThreadInfoHub());
		modelThreadInfo.setDisplayName("Thread Info");
		modelThreadInfo.setPluralDisplayName("Thread Infos");
		modelThreadInfo.setForJfc(getForJfc());
		modelThreadInfo.setAllowNew(false);
		modelThreadInfo.setAllowSave(true);
		modelThreadInfo.setAllowAdd(false);
		modelThreadInfo.setAllowRemove(true);
		modelThreadInfo.setAllowClear(true);
		modelThreadInfo.setAllowDelete(false);
		modelThreadInfo.setAllowSearch(true);
		modelThreadInfo.setAllowHubSearch(false);
		modelThreadInfo.setAllowGotoEdit(true);
		modelThreadInfo.setViewOnly(true);
		// call ReportInstanceProcess.threadInfoModelCallback(ThreadInfoModel) to be able to customize this model
		OAObjectCallbackDelegate.onObjectCallbackModel(ReportInstanceProcess.class, ReportInstanceProcess.P_ThreadInfo, modelThreadInfo);

		return modelThreadInfo;
	}

	public ReportInstanceProcessStepModel getReportInstanceProcessStepsModel() {
		if (modelReportInstanceProcessSteps != null) {
			return modelReportInstanceProcessSteps;
		}
		modelReportInstanceProcessSteps = new ReportInstanceProcessStepModel(getReportInstanceProcessSteps());
		modelReportInstanceProcessSteps.setDisplayName("Report Instance Process Step");
		modelReportInstanceProcessSteps.setPluralDisplayName("Report Instance Process Steps");
		if (HubDetailDelegate.getIsFromSameMasterHub(getOriginalHub(), getReportInstanceProcessSteps())) {
			modelReportInstanceProcessSteps.setCreateUI(false);
		}
		modelReportInstanceProcessSteps.setForJfc(getForJfc());
		modelReportInstanceProcessSteps.setAllowNew(false);
		modelReportInstanceProcessSteps.setAllowSave(true);
		modelReportInstanceProcessSteps.setAllowAdd(false);
		modelReportInstanceProcessSteps.setAllowMove(false);
		modelReportInstanceProcessSteps.setAllowRemove(false);
		modelReportInstanceProcessSteps.setAllowDelete(false);
		modelReportInstanceProcessSteps.setAllowRefresh(false);
		modelReportInstanceProcessSteps.setAllowSearch(false);
		modelReportInstanceProcessSteps.setAllowHubSearch(false);
		modelReportInstanceProcessSteps.setAllowGotoEdit(true);
		modelReportInstanceProcessSteps.setViewOnly(getViewOnly());
		modelReportInstanceProcessSteps.setAllowTableFilter(true);
		modelReportInstanceProcessSteps.setAllowTableSorting(true);
		modelReportInstanceProcessSteps.setAllowMultiSelect(false);
		modelReportInstanceProcessSteps.setAllowCopy(false);
		modelReportInstanceProcessSteps.setAllowCut(false);
		modelReportInstanceProcessSteps.setAllowPaste(false);
		// call ReportInstanceProcess.reportInstanceProcessStepsModelCallback(ReportInstanceProcessStepModel) to be able to customize this model
		OAObjectCallbackDelegate.onObjectCallbackModel(	ReportInstanceProcess.class, ReportInstanceProcess.P_ReportInstanceProcessSteps,
														modelReportInstanceProcessSteps);

		return modelReportInstanceProcessSteps;
	}

	public ReportInfoModel getReportInfoSelectFromModel() {
		if (modelReportInfoSelectFrom != null) {
			return modelReportInfoSelectFrom;
		}
		modelReportInfoSelectFrom = new ReportInfoModel(getReportInfoSelectFromHub());
		modelReportInfoSelectFrom.setDisplayName("Report Info");
		modelReportInfoSelectFrom.setPluralDisplayName("Report Infos");
		modelReportInfoSelectFrom.setForJfc(getForJfc());
		modelReportInfoSelectFrom.setAllowNew(false);
		modelReportInfoSelectFrom.setAllowSave(true);
		modelReportInfoSelectFrom.setAllowAdd(false);
		modelReportInfoSelectFrom.setAllowMove(false);
		modelReportInfoSelectFrom.setAllowRemove(false);
		modelReportInfoSelectFrom.setAllowDelete(false);
		modelReportInfoSelectFrom.setAllowSearch(true);
		modelReportInfoSelectFrom.setAllowHubSearch(true);
		modelReportInfoSelectFrom.setAllowGotoEdit(true);
		modelReportInfoSelectFrom.setViewOnly(getViewOnly());
		modelReportInfoSelectFrom.setAllowNew(false);
		modelReportInfoSelectFrom.setAllowTableFilter(true);
		modelReportInfoSelectFrom.setAllowTableSorting(true);
		modelReportInfoSelectFrom.setAllowCut(false);
		modelReportInfoSelectFrom.setAllowCopy(false);
		modelReportInfoSelectFrom.setAllowPaste(false);
		modelReportInfoSelectFrom.setAllowMultiSelect(false);
		return modelReportInfoSelectFrom;
	}

	public StoreInfoModel getStoreInfoSelectFromModel() {
		if (modelStoreInfoSelectFrom != null) {
			return modelStoreInfoSelectFrom;
		}
		modelStoreInfoSelectFrom = new StoreInfoModel(getStoreInfoSelectFromHub());
		modelStoreInfoSelectFrom.setDisplayName("Store Info");
		modelStoreInfoSelectFrom.setPluralDisplayName("Store Infos");
		modelStoreInfoSelectFrom.setForJfc(getForJfc());
		modelStoreInfoSelectFrom.setAllowNew(false);
		modelStoreInfoSelectFrom.setAllowSave(true);
		modelStoreInfoSelectFrom.setAllowAdd(false);
		modelStoreInfoSelectFrom.setAllowMove(false);
		modelStoreInfoSelectFrom.setAllowRemove(false);
		modelStoreInfoSelectFrom.setAllowDelete(false);
		modelStoreInfoSelectFrom.setAllowSearch(true);
		modelStoreInfoSelectFrom.setAllowHubSearch(true);
		modelStoreInfoSelectFrom.setAllowGotoEdit(true);
		modelStoreInfoSelectFrom.setViewOnly(getViewOnly());
		modelStoreInfoSelectFrom.setAllowNew(false);
		modelStoreInfoSelectFrom.setAllowTableFilter(true);
		modelStoreInfoSelectFrom.setAllowTableSorting(true);
		modelStoreInfoSelectFrom.setAllowCut(false);
		modelStoreInfoSelectFrom.setAllowCopy(false);
		modelStoreInfoSelectFrom.setAllowPaste(false);
		modelStoreInfoSelectFrom.setAllowMultiSelect(false);
		return modelStoreInfoSelectFrom;
	}

	public ThreadInfoModel getThreadInfoSelectFromModel() {
		if (modelThreadInfoSelectFrom != null) {
			return modelThreadInfoSelectFrom;
		}
		modelThreadInfoSelectFrom = new ThreadInfoModel(getThreadInfoSelectFromHub());
		modelThreadInfoSelectFrom.setDisplayName("Thread Info");
		modelThreadInfoSelectFrom.setPluralDisplayName("Thread Infos");
		modelThreadInfoSelectFrom.setForJfc(getForJfc());
		modelThreadInfoSelectFrom.setAllowNew(false);
		modelThreadInfoSelectFrom.setAllowSave(true);
		modelThreadInfoSelectFrom.setAllowAdd(false);
		modelThreadInfoSelectFrom.setAllowMove(false);
		modelThreadInfoSelectFrom.setAllowRemove(false);
		modelThreadInfoSelectFrom.setAllowDelete(false);
		modelThreadInfoSelectFrom.setAllowSearch(true);
		modelThreadInfoSelectFrom.setAllowHubSearch(true);
		modelThreadInfoSelectFrom.setAllowGotoEdit(true);
		modelThreadInfoSelectFrom.setViewOnly(getViewOnly());
		modelThreadInfoSelectFrom.setAllowNew(false);
		modelThreadInfoSelectFrom.setAllowTableFilter(true);
		modelThreadInfoSelectFrom.setAllowTableSorting(true);
		modelThreadInfoSelectFrom.setAllowCut(false);
		modelThreadInfoSelectFrom.setAllowCopy(false);
		modelThreadInfoSelectFrom.setAllowPaste(false);
		modelThreadInfoSelectFrom.setAllowMultiSelect(false);
		return modelThreadInfoSelectFrom;
	}

	public PypeReportMessageSearchModel getPypeReportMessageSearchModel() {
		if (modelPypeReportMessageSearch != null) {
			return modelPypeReportMessageSearch;
		}
		modelPypeReportMessageSearch = new PypeReportMessageSearchModel();
		HubSelectDelegate.adoptWhereHub(modelPypeReportMessageSearch.getHub(), ReportInstanceProcess.P_PypeReportMessage, getHub());
		return modelPypeReportMessageSearch;
	}

	public ReportInfoSearchModel getReportInfoSearchModel() {
		if (modelReportInfoSearch != null) {
			return modelReportInfoSearch;
		}
		modelReportInfoSearch = new ReportInfoSearchModel();
		OAFilter filter = new OAEqualPathFilter(ReportInstanceProcessModel.this.getHub(),
				ReportInstanceProcessPP.reportInstanceProcessorInfo().pp, ReportInfoPP.reportInstanceProcessorInfo().pp);
		filter = new OAAndFilter(filter, new OAInFilter(ReportInstanceProcessModel.this.getHub(),
				ReportInstanceProcessPP.reportInstanceProcessorInfo().reportInfos().pp));
		modelReportInfoSearch.getReportInfoSearch().setExtraWhereFilter(filter);
		return modelReportInfoSearch;
	}

	public StoreInfoSearchModel getStoreInfoSearchModel() {
		if (modelStoreInfoSearch != null) {
			return modelStoreInfoSearch;
		}
		modelStoreInfoSearch = new StoreInfoSearchModel();
		OAFilter filter = new OAEqualPathFilter(ReportInstanceProcessModel.this.getHub(),
				ReportInstanceProcessPP.reportInstanceProcessorInfo().reporterCorp().pp, StoreInfoPP.reporterCorp().pp);
		filter = new OAAndFilter(filter, new OAInFilter(ReportInstanceProcessModel.this.getHub(),
				ReportInstanceProcessPP.reportInstanceProcessorInfo().reporterCorp().storeInfos().pp));
		modelStoreInfoSearch.getStoreInfoSearch().setExtraWhereFilter(filter);
		return modelStoreInfoSearch;
	}

	public ThreadInfoSearchModel getThreadInfoSearchModel() {
		if (modelThreadInfoSearch != null) {
			return modelThreadInfoSearch;
		}
		modelThreadInfoSearch = new ThreadInfoSearchModel();
		OAFilter filter = new OAEqualPathFilter(ReportInstanceProcessModel.this.getHub(),
				ReportInstanceProcessPP.reportInstanceProcessorInfo().reporterCorp().pp, ThreadInfoPP.reporterCorp().pp);
		filter = new OAAndFilter(filter, new OAInFilter(ReportInstanceProcessModel.this.getHub(),
				ReportInstanceProcessPP.reportInstanceProcessorInfo().reporterCorp().threadInfos().pp));
		modelThreadInfoSearch.getThreadInfoSearch().setExtraWhereFilter(filter);
		return modelThreadInfoSearch;
	}

	public HubCopy<ReportInstanceProcess> createHubCopy() {
		Hub<ReportInstanceProcess> hubReportInstanceProcessx = new Hub<>(ReportInstanceProcess.class);
		HubCopy<ReportInstanceProcess> hc = new HubCopy<>(getHub(), hubReportInstanceProcessx, true);
		return hc;
	}

	public ReportInstanceProcessModel createCopy() {
		ReportInstanceProcessModel mod = new ReportInstanceProcessModel(createHubCopy().getHub());
		return mod;
	}
}
