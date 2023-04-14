package com.auto.dev.reportercorp.model;

import java.util.logging.Logger;

import com.auto.dev.reportercorp.delegate.ModelDelegate;
import com.auto.dev.reportercorp.model.oa.Environment;
import com.auto.dev.reportercorp.model.oa.ReportInstanceProcessorInfo;
import com.auto.dev.reportercorp.model.oa.ReporterCorp;
import com.auto.dev.reportercorp.model.oa.ReporterCorpParam;
import com.auto.dev.reportercorp.model.oa.StatusInfo;
import com.auto.dev.reportercorp.model.oa.StoreInfo;
import com.auto.dev.reportercorp.model.oa.ThreadInfo;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubCombined;
import com.viaoa.hub.HubCopy;
import com.viaoa.hub.HubDelegate;
import com.viaoa.hub.HubDetailDelegate;
import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObjectCallbackDelegate;
import com.viaoa.object.OAObjectModel;

public class ReporterCorpModel extends OAObjectModel {
	private static Logger LOG = Logger.getLogger(ReporterCorpModel.class.getName());

	// Hubs
	protected Hub<ReporterCorp> hub;
	// selected reporterCorps
	protected Hub<ReporterCorp> hubMultiSelect;
	// detail hubs
	protected Hub<ReportInstanceProcessorInfo> hubReportInstanceProcessorInfo;
	protected Hub<Environment> hubEnvironment;
	protected Hub<StatusInfo> hubStatusInfo;
	protected Hub<ReporterCorpParam> hubReporterCorpParams;
	protected Hub<StoreInfo> hubStoreInfos;
	protected Hub<ThreadInfo> hubThreadInfos;

	// AddHubs used for references
	protected Hub<Environment> hubEnvironmentSelectFrom;

	// ObjectModels
	protected ReportInstanceProcessorInfoModel modelReportInstanceProcessorInfo;
	protected EnvironmentModel modelEnvironment;
	protected StatusInfoModel modelStatusInfo;
	protected ReporterCorpParamModel modelReporterCorpParams;
	protected StoreInfoModel modelStoreInfos;
	protected ThreadInfoModel modelThreadInfos;

	// selectFrom
	protected EnvironmentModel modelEnvironmentSelectFrom;

	public ReporterCorpModel() {
		setDisplayName("Reporter Corp");
		setPluralDisplayName("Reporter Corps");
	}

	public ReporterCorpModel(Hub<ReporterCorp> hubReporterCorp) {
		this();
		if (hubReporterCorp != null) {
			HubDelegate.setObjectClass(hubReporterCorp, ReporterCorp.class);
		}
		this.hub = hubReporterCorp;
	}

	public ReporterCorpModel(ReporterCorp reporterCorp) {
		this();
		getHub().add(reporterCorp);
		getHub().setPos(0);
	}

	public Hub<ReporterCorp> getOriginalHub() {
		return getHub();
	}

	public Hub<ReportInstanceProcessorInfo> getReportInstanceProcessorInfo() {
		if (hubReportInstanceProcessorInfo == null) {
			hubReportInstanceProcessorInfo = getHub().getDetailHub(ReporterCorp.P_ReportInstanceProcessorInfo);
		}
		return hubReportInstanceProcessorInfo;
	}

	public Hub<Environment> getEnvironmentHub() {
		if (hubEnvironment != null) {
			return hubEnvironment;
		}
		// this is the owner, use detailHub
		hubEnvironment = getHub().getDetailHub(ReporterCorp.P_Environment);
		return hubEnvironment;
	}

	public Hub<StatusInfo> getStatusInfoHub() {
		if (hubStatusInfo != null) {
			return hubStatusInfo;
		}
		hubStatusInfo = getHub().getDetailHub(ReporterCorp.P_StatusInfo);
		return hubStatusInfo;
	}

	public Hub<ReporterCorpParam> getReporterCorpParams() {
		if (hubReporterCorpParams == null) {
			hubReporterCorpParams = getHub().getDetailHub(ReporterCorp.P_ReporterCorpParams);
		}
		return hubReporterCorpParams;
	}

	public Hub<StoreInfo> getStoreInfos() {
		if (hubStoreInfos == null) {
			hubStoreInfos = getHub().getDetailHub(ReporterCorp.P_StoreInfos);
		}
		return hubStoreInfos;
	}

	public Hub<ThreadInfo> getThreadInfos() {
		if (hubThreadInfos == null) {
			hubThreadInfos = getHub().getDetailHub(ReporterCorp.P_ThreadInfos);
		}
		return hubThreadInfos;
	}

	public Hub<Environment> getEnvironmentSelectFromHub() {
		if (hubEnvironmentSelectFrom != null) {
			return hubEnvironmentSelectFrom;
		}
		hubEnvironmentSelectFrom = new Hub<Environment>(Environment.class);
		Hub<Environment> hubEnvironmentSelectFrom1 = ModelDelegate.getEnvironments().createSharedHub();
		HubCombined<Environment> hubCombined = new HubCombined(hubEnvironmentSelectFrom, hubEnvironmentSelectFrom1, getEnvironmentHub());
		hubEnvironmentSelectFrom.setLinkHub(getHub(), ReporterCorp.P_Environment);
		return hubEnvironmentSelectFrom;
	}

	public ReporterCorp getReporterCorp() {
		return getHub().getAO();
	}

	public Hub<ReporterCorp> getHub() {
		if (hub == null) {
			hub = new Hub<ReporterCorp>(ReporterCorp.class);
		}
		return hub;
	}

	public Hub<ReporterCorp> getMultiSelectHub() {
		if (hubMultiSelect == null) {
			hubMultiSelect = new Hub<ReporterCorp>(ReporterCorp.class);
		}
		return hubMultiSelect;
	}

	public ReportInstanceProcessorInfoModel getReportInstanceProcessorInfoModel() {
		if (modelReportInstanceProcessorInfo != null) {
			return modelReportInstanceProcessorInfo;
		}
		modelReportInstanceProcessorInfo = new ReportInstanceProcessorInfoModel(getReportInstanceProcessorInfo());
		modelReportInstanceProcessorInfo.setDisplayName("Report Instance Processor Info");
		modelReportInstanceProcessorInfo.setPluralDisplayName("ReportInstanceProcessorInfos");
		modelReportInstanceProcessorInfo.setAllowNew(false);
		modelReportInstanceProcessorInfo.setAllowAdd(false);
		modelReportInstanceProcessorInfo.setAllowRemove(false);
		modelReportInstanceProcessorInfo.setAllowDelete(false);
		modelReportInstanceProcessorInfo.setAllowCut(false);
		modelReportInstanceProcessorInfo.setAllowCopy(false);
		modelReportInstanceProcessorInfo.setAllowPaste(false);
		modelReportInstanceProcessorInfo.setAllowSearch(false);
		modelReportInstanceProcessorInfo.setAllowHubSearch(false);
		modelReportInstanceProcessorInfo.setViewOnly(getViewOnly());
		return modelReportInstanceProcessorInfo;
	}

	public EnvironmentModel getEnvironmentModel() {
		if (modelEnvironment != null) {
			return modelEnvironment;
		}
		modelEnvironment = new EnvironmentModel(getEnvironmentHub());
		modelEnvironment.setDisplayName("Environment");
		modelEnvironment.setPluralDisplayName("Environments");
		modelEnvironment.setForJfc(getForJfc());
		modelEnvironment.setAllowNew(false);
		modelEnvironment.setAllowSave(true);
		modelEnvironment.setAllowAdd(false);
		modelEnvironment.setAllowRemove(false);
		modelEnvironment.setAllowClear(false);
		modelEnvironment.setAllowDelete(false);
		modelEnvironment.setAllowSearch(false);
		modelEnvironment.setAllowHubSearch(false);
		modelEnvironment.setAllowGotoEdit(true);
		OALinkInfo li = HubDetailDelegate.getLinkInfoFromDetailToMaster(getOriginalHub());
		modelEnvironment.setCreateUI(li == null || !ReporterCorp.P_Environment.equalsIgnoreCase(li.getName()));
		modelEnvironment.setViewOnly(getViewOnly());
		// call ReporterCorp.environmentModelCallback(EnvironmentModel) to be able to customize this model
		OAObjectCallbackDelegate.onObjectCallbackModel(ReporterCorp.class, ReporterCorp.P_Environment, modelEnvironment);

		return modelEnvironment;
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
		// call ReporterCorp.statusInfoModelCallback(StatusInfoModel) to be able to customize this model
		OAObjectCallbackDelegate.onObjectCallbackModel(ReporterCorp.class, ReporterCorp.P_StatusInfo, modelStatusInfo);

		return modelStatusInfo;
	}

	public ReporterCorpParamModel getReporterCorpParamsModel() {
		if (modelReporterCorpParams != null) {
			return modelReporterCorpParams;
		}
		modelReporterCorpParams = new ReporterCorpParamModel(getReporterCorpParams());
		modelReporterCorpParams.setDisplayName("Reporter Corp Param");
		modelReporterCorpParams.setPluralDisplayName("Reporter Corp Params");
		if (HubDetailDelegate.getIsFromSameMasterHub(getOriginalHub(), getReporterCorpParams())) {
			modelReporterCorpParams.setCreateUI(false);
		}
		modelReporterCorpParams.setForJfc(getForJfc());
		modelReporterCorpParams.setAllowNew(true);
		modelReporterCorpParams.setAllowSave(true);
		modelReporterCorpParams.setAllowAdd(false);
		modelReporterCorpParams.setAllowMove(false);
		modelReporterCorpParams.setAllowRemove(false);
		modelReporterCorpParams.setAllowDelete(true);
		modelReporterCorpParams.setAllowRefresh(false);
		modelReporterCorpParams.setAllowSearch(false);
		modelReporterCorpParams.setAllowHubSearch(false);
		modelReporterCorpParams.setAllowGotoEdit(true);
		modelReporterCorpParams.setViewOnly(getViewOnly());
		modelReporterCorpParams.setAllowTableFilter(true);
		modelReporterCorpParams.setAllowTableSorting(true);
		modelReporterCorpParams.setAllowMultiSelect(false);
		modelReporterCorpParams.setAllowCopy(false);
		modelReporterCorpParams.setAllowCut(false);
		modelReporterCorpParams.setAllowPaste(false);
		// call ReporterCorp.reporterCorpParamsModelCallback(ReporterCorpParamModel) to be able to customize this model
		OAObjectCallbackDelegate.onObjectCallbackModel(ReporterCorp.class, ReporterCorp.P_ReporterCorpParams, modelReporterCorpParams);

		return modelReporterCorpParams;
	}

	public StoreInfoModel getStoreInfosModel() {
		if (modelStoreInfos != null) {
			return modelStoreInfos;
		}
		modelStoreInfos = new StoreInfoModel(getStoreInfos());
		modelStoreInfos.setDisplayName("Store Info");
		modelStoreInfos.setPluralDisplayName("Store Infos");
		if (HubDetailDelegate.getIsFromSameMasterHub(getOriginalHub(), getStoreInfos())) {
			modelStoreInfos.setCreateUI(false);
		}
		modelStoreInfos.setForJfc(getForJfc());
		modelStoreInfos.setAllowNew(true);
		modelStoreInfos.setAllowSave(true);
		modelStoreInfos.setAllowAdd(false);
		modelStoreInfos.setAllowMove(false);
		modelStoreInfos.setAllowRemove(false);
		modelStoreInfos.setAllowDelete(true);
		modelStoreInfos.setAllowRefresh(false);
		modelStoreInfos.setAllowSearch(false);
		modelStoreInfos.setAllowHubSearch(false);
		modelStoreInfos.setAllowGotoEdit(true);
		modelStoreInfos.setViewOnly(getViewOnly());
		modelStoreInfos.setAllowTableFilter(true);
		modelStoreInfos.setAllowTableSorting(true);
		modelStoreInfos.setAllowMultiSelect(false);
		modelStoreInfos.setAllowCopy(false);
		modelStoreInfos.setAllowCut(false);
		modelStoreInfos.setAllowPaste(false);
		// call ReporterCorp.storeInfosModelCallback(StoreInfoModel) to be able to customize this model
		OAObjectCallbackDelegate.onObjectCallbackModel(ReporterCorp.class, ReporterCorp.P_StoreInfos, modelStoreInfos);

		return modelStoreInfos;
	}

	public ThreadInfoModel getThreadInfosModel() {
		if (modelThreadInfos != null) {
			return modelThreadInfos;
		}
		modelThreadInfos = new ThreadInfoModel(getThreadInfos());
		modelThreadInfos.setDisplayName("Thread Info");
		modelThreadInfos.setPluralDisplayName("Thread Infos");
		if (HubDetailDelegate.getIsFromSameMasterHub(getOriginalHub(), getThreadInfos())) {
			modelThreadInfos.setCreateUI(false);
		}
		modelThreadInfos.setForJfc(getForJfc());
		modelThreadInfos.setAllowNew(true);
		modelThreadInfos.setAllowSave(true);
		modelThreadInfos.setAllowAdd(false);
		modelThreadInfos.setAllowMove(false);
		modelThreadInfos.setAllowRemove(false);
		modelThreadInfos.setAllowDelete(true);
		modelThreadInfos.setAllowRefresh(false);
		modelThreadInfos.setAllowSearch(false);
		modelThreadInfos.setAllowHubSearch(false);
		modelThreadInfos.setAllowGotoEdit(true);
		modelThreadInfos.setViewOnly(getViewOnly());
		modelThreadInfos.setAllowTableFilter(true);
		modelThreadInfos.setAllowTableSorting(true);
		modelThreadInfos.setAllowMultiSelect(false);
		modelThreadInfos.setAllowCopy(false);
		modelThreadInfos.setAllowCut(false);
		modelThreadInfos.setAllowPaste(false);
		// call ReporterCorp.threadInfosModelCallback(ThreadInfoModel) to be able to customize this model
		OAObjectCallbackDelegate.onObjectCallbackModel(ReporterCorp.class, ReporterCorp.P_ThreadInfos, modelThreadInfos);

		return modelThreadInfos;
	}

	public EnvironmentModel getEnvironmentSelectFromModel() {
		if (modelEnvironmentSelectFrom != null) {
			return modelEnvironmentSelectFrom;
		}
		modelEnvironmentSelectFrom = new EnvironmentModel(getEnvironmentSelectFromHub());
		modelEnvironmentSelectFrom.setDisplayName("Environment");
		modelEnvironmentSelectFrom.setPluralDisplayName("Environments");
		modelEnvironmentSelectFrom.setForJfc(getForJfc());
		modelEnvironmentSelectFrom.setAllowNew(false);
		modelEnvironmentSelectFrom.setAllowSave(true);
		modelEnvironmentSelectFrom.setAllowAdd(false);
		modelEnvironmentSelectFrom.setAllowMove(false);
		modelEnvironmentSelectFrom.setAllowRemove(false);
		modelEnvironmentSelectFrom.setAllowDelete(false);
		modelEnvironmentSelectFrom.setAllowSearch(false);
		modelEnvironmentSelectFrom.setAllowHubSearch(true);
		modelEnvironmentSelectFrom.setAllowGotoEdit(true);
		modelEnvironmentSelectFrom.setViewOnly(getViewOnly());
		modelEnvironmentSelectFrom.setAllowNew(false);
		modelEnvironmentSelectFrom.setAllowTableFilter(true);
		modelEnvironmentSelectFrom.setAllowTableSorting(true);
		modelEnvironmentSelectFrom.setAllowCut(false);
		modelEnvironmentSelectFrom.setAllowCopy(false);
		modelEnvironmentSelectFrom.setAllowPaste(false);
		modelEnvironmentSelectFrom.setAllowMultiSelect(false);
		return modelEnvironmentSelectFrom;
	}

	public HubCopy<ReporterCorp> createHubCopy() {
		Hub<ReporterCorp> hubReporterCorpx = new Hub<>(ReporterCorp.class);
		HubCopy<ReporterCorp> hc = new HubCopy<>(getHub(), hubReporterCorpx, true);
		return hc;
	}

	public ReporterCorpModel createCopy() {
		ReporterCorpModel mod = new ReporterCorpModel(createHubCopy().getHub());
		return mod;
	}
}
