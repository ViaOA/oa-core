package com.auto.dev.reportercorp.model.oa;

import java.util.logging.Logger;

import com.viaoa.annotation.OAClass;
import com.viaoa.annotation.OAColumn;
import com.viaoa.annotation.OAFkey;
import com.viaoa.annotation.OAId;
import com.viaoa.annotation.OAOne;
import com.viaoa.annotation.OAProperty;
import com.viaoa.object.OAObject;
import com.viaoa.util.OADateTime;

@OAClass(lowerName = "reportInstanceProcessStep", pluralName = "ReportInstanceProcessSteps", shortName = "rip", displayName = "Report Instance Process Step", useDataSource = false, displayProperty = "processStep")
public class ReportInstanceProcessStep extends OAObject {
	private static final long serialVersionUID = 1L;
	private static Logger LOG = Logger.getLogger(ReportInstanceProcessStep.class.getName());

	public static final String P_Id = "id";
	public static final String P_Created = "created";
	public static final String P_Used = "used";
	public static final String P_Result = "result";
	public static final String P_Success = "success";

	public static final String P_ProcessStep = "processStep";
	public static final String P_ProcessStepId = "processStepId"; // fkey
	public static final String P_ReportInstanceProcess = "reportInstanceProcess";
	public static final String P_ReportInstanceProcessId = "reportInstanceProcessId"; // fkey

	protected volatile int id;
	protected volatile OADateTime created;
	protected volatile boolean used;
	protected volatile String result;
	protected volatile boolean success;

	// Links to other objects.
	protected volatile transient ProcessStep processStep;
	protected volatile transient ReportInstanceProcess reportInstanceProcess;

	public ReportInstanceProcessStep() {
		if (!isLoading()) {
			setObjectDefaults();
		}
	}

	@Override
	public void setObjectDefaults() {
		setCreated(new OADateTime());
	}

	public ReportInstanceProcessStep(int id) {
		this();
		setId(id);
	}

	@OAProperty(isUnique = true, trackPrimitiveNull = false, displayLength = 6, noPojo = true)
	@OAId
	@OAColumn(name = "id", sqlType = java.sql.Types.INTEGER)
	public int getId() {
		return id;
	}

	public void setId(int newValue) {
		int old = id;
		fireBeforePropertyChange(P_Id, old, newValue);
		this.id = newValue;
		firePropertyChange(P_Id, old, this.id);
	}

	@OAProperty(defaultValue = "new OADateTime()", displayLength = 15, isProcessed = true, ignoreTimeZone = true)
	@OAColumn(name = "created", sqlType = java.sql.Types.TIMESTAMP)
	public OADateTime getCreated() {
		return created;
	}

	public void setCreated(OADateTime newValue) {
		OADateTime old = created;
		fireBeforePropertyChange(P_Created, old, newValue);
		this.created = newValue;
		firePropertyChange(P_Created, old, this.created);
	}

	@OAProperty(displayLength = 5)
	@OAColumn(name = "used", sqlType = java.sql.Types.BOOLEAN)
	public boolean getUsed() {
		return used;
	}

	public boolean isUsed() {
		return getUsed();
	}

	public void setUsed(boolean newValue) {
		boolean old = used;
		fireBeforePropertyChange(P_Used, old, newValue);
		this.used = newValue;
		firePropertyChange(P_Used, old, this.used);
	}

	@OAProperty(maxLength = 250, displayLength = 50, uiColumnLength = 20)
	@OAColumn(name = "result", maxLength = 250)
	public String getResult() {
		return result;
	}

	public void setResult(String newValue) {
		String old = result;
		fireBeforePropertyChange(P_Result, old, newValue);
		this.result = newValue;
		firePropertyChange(P_Result, old, this.result);
	}

	@OAProperty(displayLength = 5, uiColumnLength = 7)
	@OAColumn(name = "success", sqlType = java.sql.Types.BOOLEAN)
	public boolean getSuccess() {
		return success;
	}

	public boolean isSuccess() {
		return getSuccess();
	}

	public void setSuccess(boolean newValue) {
		boolean old = success;
		fireBeforePropertyChange(P_Success, old, newValue);
		this.success = newValue;
		firePropertyChange(P_Success, old, this.success);
	}

	@OAOne(displayName = "Process Step", reverseName = ProcessStep.P_ReportInstanceProcessSteps, required = true, allowCreateNew = false, fkeys = {
			@OAFkey(fromProperty = P_ProcessStepId, toProperty = ProcessStep.P_Id) })
	public ProcessStep getProcessStep() {
		if (processStep == null) {
			processStep = (ProcessStep) getObject(P_ProcessStep);
		}
		return processStep;
	}

	public void setProcessStep(ProcessStep newValue) {
		ProcessStep old = this.processStep;
		fireBeforePropertyChange(P_ProcessStep, old, newValue);
		this.processStep = newValue;
		firePropertyChange(P_ProcessStep, old, this.processStep);
	}

	@OAProperty(isFkeyOnly = true)
	@OAColumn(name = "process_step_id")
	public Integer getProcessStepId() {
		return (Integer) getFkeyProperty(P_ProcessStepId);
	}

	public void setProcessStepId(Integer newValue) {
		this.processStep = null;
		setFkeyProperty(P_ProcessStepId, newValue);
	}

	@OAOne(displayName = "Report Instance Process", reverseName = ReportInstanceProcess.P_ReportInstanceProcessSteps, required = true, allowCreateNew = false, fkeys = {
			@OAFkey(fromProperty = P_ReportInstanceProcessId, toProperty = ReportInstanceProcess.P_Id) })
	public ReportInstanceProcess getReportInstanceProcess() {
		if (reportInstanceProcess == null) {
			reportInstanceProcess = (ReportInstanceProcess) getObject(P_ReportInstanceProcess);
		}
		return reportInstanceProcess;
	}

	public void setReportInstanceProcess(ReportInstanceProcess newValue) {
		ReportInstanceProcess old = this.reportInstanceProcess;
		fireBeforePropertyChange(P_ReportInstanceProcess, old, newValue);
		this.reportInstanceProcess = newValue;
		firePropertyChange(P_ReportInstanceProcess, old, this.reportInstanceProcess);
	}

	@OAProperty(isFkeyOnly = true)
	@OAColumn(name = "report_instance_process_id")
	public Integer getReportInstanceProcessId() {
		return (Integer) getFkeyProperty(P_ReportInstanceProcessId);
	}

	public void setReportInstanceProcessId(Integer newValue) {
		this.reportInstanceProcess = null;
		setFkeyProperty(P_ReportInstanceProcessId, newValue);
	}
}
