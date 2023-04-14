package com.auto.dev.reportercorp.model;

import java.util.logging.Logger;

import com.auto.dev.reportercorp.model.oa.ProcessStep;
import com.viaoa.hub.Hub;
import com.viaoa.hub.HubCopy;
import com.viaoa.hub.HubDelegate;
import com.viaoa.object.OAObjectModel;

public class ProcessStepModel extends OAObjectModel {
	private static Logger LOG = Logger.getLogger(ProcessStepModel.class.getName());

	// Hubs
	protected Hub<ProcessStep> hub;
	// selected processSteps
	protected Hub<ProcessStep> hubMultiSelect;

	public ProcessStepModel() {
		setDisplayName("Process Step");
		setPluralDisplayName("Process Steps");
	}

	public ProcessStepModel(Hub<ProcessStep> hubProcessStep) {
		this();
		if (hubProcessStep != null) {
			HubDelegate.setObjectClass(hubProcessStep, ProcessStep.class);
		}
		this.hub = hubProcessStep;
	}

	public ProcessStepModel(ProcessStep processStep) {
		this();
		getHub().add(processStep);
		getHub().setPos(0);
	}

	public Hub<ProcessStep> getOriginalHub() {
		return getHub();
	}

	public ProcessStep getProcessStep() {
		return getHub().getAO();
	}

	public Hub<ProcessStep> getHub() {
		if (hub == null) {
			hub = new Hub<ProcessStep>(ProcessStep.class);
		}
		return hub;
	}

	public Hub<ProcessStep> getMultiSelectHub() {
		if (hubMultiSelect == null) {
			hubMultiSelect = new Hub<ProcessStep>(ProcessStep.class);
		}
		return hubMultiSelect;
	}

	public HubCopy<ProcessStep> createHubCopy() {
		Hub<ProcessStep> hubProcessStepx = new Hub<>(ProcessStep.class);
		HubCopy<ProcessStep> hc = new HubCopy<>(getHub(), hubProcessStepx, true);
		return hc;
	}

	public ProcessStepModel createCopy() {
		ProcessStepModel mod = new ProcessStepModel(createHubCopy().getHub());
		return mod;
	}
}
