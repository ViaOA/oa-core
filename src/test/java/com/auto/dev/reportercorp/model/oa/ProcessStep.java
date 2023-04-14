package com.auto.dev.reportercorp.model.oa;

import java.util.logging.Logger;

import com.viaoa.annotation.OACalculatedProperty;
import com.viaoa.annotation.OAClass;
import com.viaoa.annotation.OAColumn;
import com.viaoa.annotation.OAId;
import com.viaoa.annotation.OAMany;
import com.viaoa.annotation.OAProperty;
import com.viaoa.hub.Hub;
import com.viaoa.object.OAObject;
import com.viaoa.util.OADateTime;
import com.viaoa.util.OAString;

@OAClass(lowerName = "processStep", pluralName = "ProcessSteps", shortName = "prs", displayName = "Process Step", isLookup = true, isPreSelect = true, useDataSource = false, displayProperty = "step", sortProperty = "step")
public class ProcessStep extends OAObject {
	private static final long serialVersionUID = 1L;
	private static Logger LOG = Logger.getLogger(ProcessStep.class.getName());

	public static final String P_Id = "id";
	public static final String P_Created = "created";
	public static final String P_Step = "step";
	public static final String P_StepString = "stepString";
	public static final String P_StepEnum = "stepEnum";
	public static final String P_StepDisplay = "stepDisplay";

	public static final String P_ReportInstanceProcessSteps = "reportInstanceProcessSteps";

	protected volatile int id;
	protected volatile OADateTime created;
	protected volatile int step;

	public static enum Step {
		begin("Begin"),
		createReportInstance("Step#1 Create Report Instance"),
		getReport("Step#2 Get Report"),
		getReportTemplate("Step#3 Get Report Template"),
		subreports("Step#4 check subreports value"),
		checkCompositeReport("Step#4A check for Composite Report"),
		checkSubreports("Step#4B check Subreports"),
		processSubreports("Step#5 process subreports"),
		getReportVersion("Step#6 get Report Version"),
		saveReportInstance("Step#7 Save Report Instance"),
		processCompoundReports("Step#8 Process Compound Reports"),
		completed("Step#9 Completed");

		private String display;

		Step(String display) {
			this.display = display;
		}

		public String getDisplay() {
			return display;
		}
	}

	public static final int STEP_begin = 0;
	public static final int STEP_createReportInstance = 1;
	public static final int STEP_getReport = 2;
	public static final int STEP_getReportTemplate = 3;
	public static final int STEP_subreports = 4;
	public static final int STEP_checkCompositeReport = 5;
	public static final int STEP_checkSubreports = 6;
	public static final int STEP_processSubreports = 7;
	public static final int STEP_getReportVersion = 8;
	public static final int STEP_saveReportInstance = 9;
	public static final int STEP_processCompoundReports = 10;
	public static final int STEP_completed = 11;

	public ProcessStep() {
		if (!isLoading()) {
			setObjectDefaults();
		}
	}

	@Override
	public void setObjectDefaults() {
		setCreated(new OADateTime());
	}

	public ProcessStep(int id) {
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

	@OAProperty(displayLength = 6, isNameValue = true, importMatch = true)
	@OAColumn(name = "step", sqlType = java.sql.Types.INTEGER)
	public int getStep() {
		return step;
	}

	public void setStep(int newValue) {
		int old = step;
		fireBeforePropertyChange(P_Step, old, newValue);
		this.step = newValue;
		firePropertyChange(P_Step, old, this.step);
	}

	@OAProperty(enumPropertyName = P_Step)
	public String getStepString() {
		Step step = getStepEnum();
		if (step == null) {
			return null;
		}
		return step.name();
	}

	public void setStepString(String val) {
		int x = -1;
		if (OAString.isNotEmpty(val)) {
			Step step = Step.valueOf(val);
			if (step != null) {
				x = step.ordinal();
			}
		}
		if (x < 0) {
			setNull(P_Step);
		} else {
			setStep(x);
		}
	}

	@OAProperty(enumPropertyName = P_Step)
	public Step getStepEnum() {
		if (isNull(P_Step)) {
			return null;
		}
		final int val = getStep();
		if (val < 0 || val >= Step.values().length) {
			return null;
		}
		return Step.values()[val];
	}

	public void setStepEnum(Step val) {
		if (val == null) {
			setNull(P_Step);
		} else {
			setStep(val.ordinal());
		}
	}

	@OACalculatedProperty(enumPropertyName = P_Step, displayName = "Step", displayLength = 6, columnLength = 6, properties = { P_Step })
	public String getStepDisplay() {
		Step step = getStepEnum();
		if (step == null) {
			return null;
		}
		return step.getDisplay();
	}

	@OAMany(displayName = "Report Instance Process Steps", toClass = ReportInstanceProcessStep.class, reverseName = ReportInstanceProcessStep.P_ProcessStep, createMethod = false)
	private Hub<ReportInstanceProcessStep> getReportInstanceProcessSteps() {
		// oamodel has createMethod set to false, this method exists only for annotations.
		return null;
	}
}
