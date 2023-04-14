package com.auto.dev.reportercorp.model;

import java.util.logging.Logger;

import com.auto.dev.reportercorp.delegate.ModelDelegate;
import com.auto.dev.reportercorp.model.oa.Report;
import com.auto.dev.reportercorp.model.oa.ReportInfo;
import com.auto.dev.reportercorp.model.oa.ReportInstanceProcess;
import com.auto.dev.reportercorp.model.oa.ReportInstanceProcessorInfo;
import com.auto.dev.reportercorp.model.oa.propertypath.ReportInfoPP;
import com.auto.dev.reportercorp.model.oa.propertypath.ReportInstanceProcessPP;
import com.auto.dev.reportercorp.model.search.ReportInstanceProcessSearchModel;
import com.auto.dev.reportercorp.model.search.ReportSearchModel;
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

public class ReportInfoModel extends OAObjectModel {
	private static Logger LOG = Logger.getLogger(ReportInfoModel.class.getName());

	// Hubs
	protected Hub<ReportInfo> hub;
	// selected reportInfos
	protected Hub<ReportInfo> hubMultiSelect;
	// detail hubs
	protected Hub<Report> hubReport;
	protected Hub<ReportInstanceProcessorInfo> hubReportInstanceProcessorInfo;
	protected Hub<ReportInstanceProcess> hubReportInstanceProcesses;

	// AddHubs used for references
	protected Hub<Report> hubReportSelectFrom;

	// ObjectModels
	protected ReportModel modelReport;
	protected ReportInstanceProcessorInfoModel modelReportInstanceProcessorInfo;
	protected ReportInstanceProcessModel modelReportInstanceProcesses;

	// selectFrom
	protected ReportModel modelReportSelectFrom;

	// SearchModels used for references
	protected ReportSearchModel modelReportSearch;
	protected ReportInstanceProcessSearchModel modelReportInstanceProcessesSearch;

	public ReportInfoModel() {
		setDisplayName("Report Info");
		setPluralDisplayName("Report Infos");
	}

	public ReportInfoModel(Hub<ReportInfo> hubReportInfo) {
		this();
		if (hubReportInfo != null) {
			HubDelegate.setObjectClass(hubReportInfo, ReportInfo.class);
		}
		this.hub = hubReportInfo;
	}

	public ReportInfoModel(ReportInfo reportInfo) {
		this();
		getHub().add(reportInfo);
		getHub().setPos(0);
	}

	public Hub<ReportInfo> getOriginalHub() {
		return getHub();
	}

	public Hub<Report> getReportHub() {
		if (hubReport != null) {
			return hubReport;
		}
		hubReport = getHub().getDetailHub(ReportInfo.P_Report);
		return hubReport;
	}

	public Hub<ReportInstanceProcessorInfo> getReportInstanceProcessorInfoHub() {
		if (hubReportInstanceProcessorInfo != null) {
			return hubReportInstanceProcessorInfo;
		}
		// this is the owner, use detailHub
		hubReportInstanceProcessorInfo = getHub().getDetailHub(ReportInfo.P_ReportInstanceProcessorInfo);
		return hubReportInstanceProcessorInfo;
	}

	public Hub<ReportInstanceProcess> getReportInstanceProcesses() {
		if (hubReportInstanceProcesses == null) {
			hubReportInstanceProcesses = getHub().getDetailHub(ReportInfo.P_ReportInstanceProcesses);
		}
		return hubReportInstanceProcesses;
	}

	public Hub<Report> getReportSelectFromHub() {
		if (hubReportSelectFrom != null) {
			return hubReportSelectFrom;
		}
		hubReportSelectFrom = new Hub<Report>(Report.class);
		Hub<Report> hubReportSelectFrom1 = ModelDelegate.getReports().createSharedHub();
		HubCombined<Report> hubCombined = new HubCombined(hubReportSelectFrom, hubReportSelectFrom1, getReportHub());
		hubReportSelectFrom.setLinkHub(getHub(), ReportInfo.P_Report);
		return hubReportSelectFrom;
	}

	public ReportInfo getReportInfo() {
		return getHub().getAO();
	}

	public Hub<ReportInfo> getHub() {
		if (hub == null) {
			hub = new Hub<ReportInfo>(ReportInfo.class);
		}
		return hub;
	}

	public Hub<ReportInfo> getMultiSelectHub() {
		if (hubMultiSelect == null) {
			hubMultiSelect = new Hub<ReportInfo>(ReportInfo.class);
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
		modelReport.setViewOnly(true);
		// call ReportInfo.reportModelCallback(ReportModel) to be able to customize this model
		OAObjectCallbackDelegate.onObjectCallbackModel(ReportInfo.class, ReportInfo.P_Report, modelReport);

		return modelReport;
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
				.setCreateUI(li == null || !ReportInfo.P_ReportInstanceProcessorInfo.equalsIgnoreCase(li.getName()));
		modelReportInstanceProcessorInfo.setViewOnly(getViewOnly());
		// call ReportInfo.reportInstanceProcessorInfoModelCallback(ReportInstanceProcessorInfoModel) to be able to customize this model
		OAObjectCallbackDelegate.onObjectCallbackModel(	ReportInfo.class, ReportInfo.P_ReportInstanceProcessorInfo,
														modelReportInstanceProcessorInfo);

		return modelReportInstanceProcessorInfo;
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
		// call ReportInfo.reportInstanceProcessesModelCallback(ReportInstanceProcessModel) to be able to customize this model
		OAObjectCallbackDelegate.onObjectCallbackModel(	ReportInfo.class, ReportInfo.P_ReportInstanceProcesses,
														modelReportInstanceProcesses);

		return modelReportInstanceProcesses;
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
		HubSelectDelegate.adoptWhereHub(modelReportSearch.getHub(), ReportInfo.P_Report, getHub());
		return modelReportSearch;
	}

	public ReportInstanceProcessSearchModel getReportInstanceProcessesSearchModel() {
		if (modelReportInstanceProcessesSearch != null) {
			return modelReportInstanceProcessesSearch;
		}
		modelReportInstanceProcessesSearch = new ReportInstanceProcessSearchModel();
		OAFilter filter = new OAEqualPathFilter(ReportInfoModel.this.getHub(), ReportInstanceProcessPP.reportInstanceProcessorInfo().pp,
				ReportInfoPP.reportInstanceProcessorInfo().pp);
		modelReportInstanceProcessesSearch.getReportInstanceProcessSearch().setExtraWhereFilter(filter);
		return modelReportInstanceProcessesSearch;
	}

	public HubCopy<ReportInfo> createHubCopy() {
		Hub<ReportInfo> hubReportInfox = new Hub<>(ReportInfo.class);
		HubCopy<ReportInfo> hc = new HubCopy<>(getHub(), hubReportInfox, true);
		return hc;
	}

	public ReportInfoModel createCopy() {
		ReportInfoModel mod = new ReportInfoModel(createHubCopy().getHub());
		return mod;
	}
}
