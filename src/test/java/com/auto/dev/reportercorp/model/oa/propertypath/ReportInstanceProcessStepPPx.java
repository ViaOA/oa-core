package com.auto.dev.reportercorp.model.oa.propertypath;

import java.io.Serializable;

import com.auto.dev.reportercorp.model.oa.ReportInstanceProcessStep;

public class ReportInstanceProcessStepPPx implements PPxInterface, Serializable {
	private static final long serialVersionUID = 1L;
	public final String pp; // propertyPath

	public ReportInstanceProcessStepPPx(String name) {
		this(null, name);
	}

	public ReportInstanceProcessStepPPx(PPxInterface parent, String name) {
		String s = null;
		if (parent != null) {
			s = parent.toString();
		}
		if (s == null) {
			s = "";
		}
		if (name != null && name.length() > 0) {
			if (s.length() > 0 && name.charAt(0) != ':') {
				s += ".";
			}
			s += name;
		}
		pp = s;
	}

	public ProcessStepPPx processStep() {
		ProcessStepPPx ppx = new ProcessStepPPx(this, ReportInstanceProcessStep.P_ProcessStep);
		return ppx;
	}

	public ReportInstanceProcessPPx reportInstanceProcess() {
		ReportInstanceProcessPPx ppx = new ReportInstanceProcessPPx(this, ReportInstanceProcessStep.P_ReportInstanceProcess);
		return ppx;
	}

	public String id() {
		return pp + "." + ReportInstanceProcessStep.P_Id;
	}

	public String created() {
		return pp + "." + ReportInstanceProcessStep.P_Created;
	}

	public String used() {
		return pp + "." + ReportInstanceProcessStep.P_Used;
	}

	public String result() {
		return pp + "." + ReportInstanceProcessStep.P_Result;
	}

	public String success() {
		return pp + "." + ReportInstanceProcessStep.P_Success;
	}

	@Override
	public String toString() {
		return pp;
	}

	public String pp() {
		return pp;
	}
}
