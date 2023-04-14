package com.auto.dev.reportercorp.model.oa.propertypath;

import java.io.Serializable;

import com.auto.dev.reportercorp.model.oa.ProcessStep;

public class ProcessStepPPx implements PPxInterface, Serializable {
	private static final long serialVersionUID = 1L;
	public final String pp; // propertyPath

	public ProcessStepPPx(String name) {
		this(null, name);
	}

	public ProcessStepPPx(PPxInterface parent, String name) {
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

	public ReportInstanceProcessStepPPx reportInstanceProcessSteps() {
		ReportInstanceProcessStepPPx ppx = new ReportInstanceProcessStepPPx(this, ProcessStep.P_ReportInstanceProcessSteps);
		return ppx;
	}

	public String id() {
		return pp + "." + ProcessStep.P_Id;
	}

	public String created() {
		return pp + "." + ProcessStep.P_Created;
	}

	public String step() {
		return pp + "." + ProcessStep.P_Step;
	}

	@Override
	public String toString() {
		return pp;
	}

	public String pp() {
		return pp;
	}
}
