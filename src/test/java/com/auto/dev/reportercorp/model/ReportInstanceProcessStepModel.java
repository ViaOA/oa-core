package com.auto.dev.reportercorp.model;

import java.util.logging.Logger;

import com.auto.dev.reportercorp.delegate.ModelDelegate;
import com.auto.dev.reportercorp.model.oa.ProcessStep;
import com.auto.dev.reportercorp.model.oa.ReportInstanceProcess;
import com.auto.dev.reportercorp.model.oa.ReportInstanceProcessStep;
import com.auto.dev.reportercorp.model.search.ReportInstanceProcessSearchModel;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubCombined;
import com.viaoa.hub.HubCopy;
import com.viaoa.hub.HubDelegate;
import com.viaoa.hub.HubDetailDelegate;
import com.viaoa.hub.HubSelectDelegate;
import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObjectCallbackDelegate;
import com.viaoa.object.OAObjectModel;

public class ReportInstanceProcessStepModel extends OAObjectModel {
	private static Logger LOG = Logger.getLogger(ReportInstanceProcessStepModel.class.getName());

	// Hubs
	protected Hub<ReportInstanceProcessStep> hub;
	// selected reportInstanceProcessSteps
	protected Hub<ReportInstanceProcessStep> hubMultiSelect;
	// detail hubs
	protected Hub<ProcessStep> hubProcessStep;
	protected Hub<ReportInstanceProcess> hubReportInstanceProcess;

	// AddHubs used for references
	protected Hub<ProcessStep> hubProcessStepSelectFrom;

	// ObjectModels
	protected ProcessStepModel modelProcessStep;
	protected ReportInstanceProcessModel modelReportInstanceProcess;

	// selectFrom
	protected ProcessStepModel modelProcessStepSelectFrom;

	// SearchModels used for references
	protected ReportInstanceProcessSearchModel modelReportInstanceProcessSearch;

	public ReportInstanceProcessStepModel() {
		setDisplayName("Report Instance Process Step");
		setPluralDisplayName("Report Instance Process Steps");
	}

	public ReportInstanceProcessStepModel(Hub<ReportInstanceProcessStep> hubReportInstanceProcessStep) {
		this();
		if (hubReportInstanceProcessStep != null) {
			HubDelegate.setObjectClass(hubReportInstanceProcessStep, ReportInstanceProcessStep.class);
		}
		this.hub = hubReportInstanceProcessStep;
	}

	public ReportInstanceProcessStepModel(ReportInstanceProcessStep reportInstanceProcessStep) {
		this();
		getHub().add(reportInstanceProcessStep);
		getHub().setPos(0);
	}

	public Hub<ReportInstanceProcessStep> getOriginalHub() {
		return getHub();
	}

	public Hub<ProcessStep> getProcessStepHub() {
		if (hubProcessStep != null) {
			return hubProcessStep;
		}
		hubProcessStep = getHub().getDetailHub(ReportInstanceProcessStep.P_ProcessStep);
		return hubProcessStep;
	}

	public Hub<ReportInstanceProcess> getReportInstanceProcessHub() {
		if (hubReportInstanceProcess != null) {
			return hubReportInstanceProcess;
		}
		// this is the owner, use detailHub
		hubReportInstanceProcess = getHub().getDetailHub(ReportInstanceProcessStep.P_ReportInstanceProcess);
		return hubReportInstanceProcess;
	}

	public Hub<ProcessStep> getProcessStepSelectFromHub() {
		if (hubProcessStepSelectFrom != null) {
			return hubProcessStepSelectFrom;
		}
		hubProcessStepSelectFrom = new Hub<ProcessStep>(ProcessStep.class);
		Hub<ProcessStep> hubProcessStepSelectFrom1 = ModelDelegate.getProcessSteps().createSharedHub();
		HubCombined<ProcessStep> hubCombined = new HubCombined(hubProcessStepSelectFrom, hubProcessStepSelectFrom1, getProcessStepHub());
		hubProcessStepSelectFrom.setLinkHub(getHub(), ReportInstanceProcessStep.P_ProcessStep);
		return hubProcessStepSelectFrom;
	}

	public ReportInstanceProcessStep getReportInstanceProcessStep() {
		return getHub().getAO();
	}

	public Hub<ReportInstanceProcessStep> getHub() {
		if (hub == null) {
			hub = new Hub<ReportInstanceProcessStep>(ReportInstanceProcessStep.class);
		}
		return hub;
	}

	public Hub<ReportInstanceProcessStep> getMultiSelectHub() {
		if (hubMultiSelect == null) {
			hubMultiSelect = new Hub<ReportInstanceProcessStep>(ReportInstanceProcessStep.class);
		}
		return hubMultiSelect;
	}

	public ProcessStepModel getProcessStepModel() {
		if (modelProcessStep != null) {
			return modelProcessStep;
		}
		modelProcessStep = new ProcessStepModel(getProcessStepHub());
		modelProcessStep.setDisplayName("Process Step");
		modelProcessStep.setPluralDisplayName("Process Steps");
		modelProcessStep.setForJfc(getForJfc());
		modelProcessStep.setAllowNew(false);
		modelProcessStep.setAllowSave(true);
		modelProcessStep.setAllowAdd(false);
		modelProcessStep.setAllowRemove(false);
		modelProcessStep.setAllowClear(false);
		modelProcessStep.setAllowDelete(false);
		modelProcessStep.setAllowSearch(false);
		modelProcessStep.setAllowHubSearch(false);
		modelProcessStep.setAllowGotoEdit(false);
		modelProcessStep.setViewOnly(true);
		// call ReportInstanceProcessStep.processStepModelCallback(ProcessStepModel) to be able to customize this model
		OAObjectCallbackDelegate.onObjectCallbackModel(	ReportInstanceProcessStep.class, ReportInstanceProcessStep.P_ProcessStep,
														modelProcessStep);

		return modelProcessStep;
	}

	public ReportInstanceProcessModel getReportInstanceProcessModel() {
		if (modelReportInstanceProcess != null) {
			return modelReportInstanceProcess;
		}
		modelReportInstanceProcess = new ReportInstanceProcessModel(getReportInstanceProcessHub());
		modelReportInstanceProcess.setDisplayName("Report Instance Process");
		modelReportInstanceProcess.setPluralDisplayName("Report Instance Processes");
		modelReportInstanceProcess.setForJfc(getForJfc());
		modelReportInstanceProcess.setAllowNew(false);
		modelReportInstanceProcess.setAllowSave(true);
		modelReportInstanceProcess.setAllowAdd(false);
		modelReportInstanceProcess.setAllowRemove(false);
		modelReportInstanceProcess.setAllowClear(false);
		modelReportInstanceProcess.setAllowDelete(false);
		modelReportInstanceProcess.setAllowSearch(false);
		modelReportInstanceProcess.setAllowHubSearch(true);
		modelReportInstanceProcess.setAllowGotoEdit(true);
		OALinkInfo li = HubDetailDelegate.getLinkInfoFromDetailToMaster(getOriginalHub());
		modelReportInstanceProcess
				.setCreateUI(li == null || !ReportInstanceProcessStep.P_ReportInstanceProcess.equalsIgnoreCase(li.getName()));
		modelReportInstanceProcess.setViewOnly(getViewOnly());
		// call ReportInstanceProcessStep.reportInstanceProcessModelCallback(ReportInstanceProcessModel) to be able to customize this model
		OAObjectCallbackDelegate.onObjectCallbackModel(	ReportInstanceProcessStep.class, ReportInstanceProcessStep.P_ReportInstanceProcess,
														modelReportInstanceProcess);

		return modelReportInstanceProcess;
	}

	public ProcessStepModel getProcessStepSelectFromModel() {
		if (modelProcessStepSelectFrom != null) {
			return modelProcessStepSelectFrom;
		}
		modelProcessStepSelectFrom = new ProcessStepModel(getProcessStepSelectFromHub());
		modelProcessStepSelectFrom.setDisplayName("Process Step");
		modelProcessStepSelectFrom.setPluralDisplayName("Process Steps");
		modelProcessStepSelectFrom.setForJfc(getForJfc());
		modelProcessStepSelectFrom.setAllowNew(false);
		modelProcessStepSelectFrom.setAllowSave(true);
		modelProcessStepSelectFrom.setAllowAdd(false);
		modelProcessStepSelectFrom.setAllowMove(false);
		modelProcessStepSelectFrom.setAllowRemove(false);
		modelProcessStepSelectFrom.setAllowDelete(false);
		modelProcessStepSelectFrom.setAllowSearch(false);
		modelProcessStepSelectFrom.setAllowHubSearch(true);
		modelProcessStepSelectFrom.setAllowGotoEdit(true);
		modelProcessStepSelectFrom.setViewOnly(getViewOnly());
		modelProcessStepSelectFrom.setAllowNew(false);
		modelProcessStepSelectFrom.setAllowTableFilter(true);
		modelProcessStepSelectFrom.setAllowTableSorting(true);
		modelProcessStepSelectFrom.setAllowCut(false);
		modelProcessStepSelectFrom.setAllowCopy(false);
		modelProcessStepSelectFrom.setAllowPaste(false);
		modelProcessStepSelectFrom.setAllowMultiSelect(false);
		return modelProcessStepSelectFrom;
	}

	public ReportInstanceProcessSearchModel getReportInstanceProcessSearchModel() {
		if (modelReportInstanceProcessSearch != null) {
			return modelReportInstanceProcessSearch;
		}
		modelReportInstanceProcessSearch = new ReportInstanceProcessSearchModel();
		HubSelectDelegate.adoptWhereHub(modelReportInstanceProcessSearch.getHub(), ReportInstanceProcessStep.P_ReportInstanceProcess,
										getHub());
		return modelReportInstanceProcessSearch;
	}

	public HubCopy<ReportInstanceProcessStep> createHubCopy() {
		Hub<ReportInstanceProcessStep> hubReportInstanceProcessStepx = new Hub<>(ReportInstanceProcessStep.class);
		HubCopy<ReportInstanceProcessStep> hc = new HubCopy<>(getHub(), hubReportInstanceProcessStepx, true);
		return hc;
	}

	public ReportInstanceProcessStepModel createCopy() {
		ReportInstanceProcessStepModel mod = new ReportInstanceProcessStepModel(createHubCopy().getHub());
		return mod;
	}
}
