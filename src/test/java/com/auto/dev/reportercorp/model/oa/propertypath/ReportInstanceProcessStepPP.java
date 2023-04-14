package com.auto.dev.reportercorp.model.oa.propertypath;

import com.auto.dev.reportercorp.model.oa.ReportInstanceProcessStep;

public class ReportInstanceProcessStepPP {
	private static ProcessStepPPx processStep;
	private static ReportInstanceProcessPPx reportInstanceProcess;

	public static ProcessStepPPx processStep() {
		if (processStep == null) {
			processStep = new ProcessStepPPx(ReportInstanceProcessStep.P_ProcessStep);
		}
		return processStep;
	}

	public static ReportInstanceProcessPPx reportInstanceProcess() {
		if (reportInstanceProcess == null) {
			reportInstanceProcess = new ReportInstanceProcessPPx(ReportInstanceProcessStep.P_ReportInstanceProcess);
		}
		return reportInstanceProcess;
	}

	public static String id() {
		String s = ReportInstanceProcessStep.P_Id;
		return s;
	}

	public static String created() {
		String s = ReportInstanceProcessStep.P_Created;
		return s;
	}

	public static String used() {
		String s = ReportInstanceProcessStep.P_Used;
		return s;
	}

	public static String result() {
		String s = ReportInstanceProcessStep.P_Result;
		return s;
	}

	public static String success() {
		String s = ReportInstanceProcessStep.P_Success;
		return s;
	}

	public static String pp() {
		return ""; // this
	}
}
