package com.auto.dev.reportercorp.model.oa.propertypath;

import com.auto.dev.reportercorp.model.oa.ProcessStep;

public class ProcessStepPP {
	private static ReportInstanceProcessStepPPx reportInstanceProcessSteps;

	public static ReportInstanceProcessStepPPx reportInstanceProcessSteps() {
		if (reportInstanceProcessSteps == null) {
			reportInstanceProcessSteps = new ReportInstanceProcessStepPPx(ProcessStep.P_ReportInstanceProcessSteps);
		}
		return reportInstanceProcessSteps;
	}

	public static String id() {
		String s = ProcessStep.P_Id;
		return s;
	}

	public static String created() {
		String s = ProcessStep.P_Created;
		return s;
	}

	public static String step() {
		String s = ProcessStep.P_Step;
		return s;
	}

	public static String pp() {
		return ""; // this
	}
}
