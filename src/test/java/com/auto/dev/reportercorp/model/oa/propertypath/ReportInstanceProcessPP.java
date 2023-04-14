package com.auto.dev.reportercorp.model.oa.propertypath;

import com.auto.dev.reportercorp.model.oa.ReportInstanceProcess;

public class ReportInstanceProcessPP {
	private static PypeReportMessagePPx pypeReportMessage;
	private static ReportInfoPPx reportInfo;
	private static ReportInstanceProcessorInfoPPx reportInstanceProcessorInfo;
	private static ReportInstanceProcessStepPPx reportInstanceProcessSteps;
	private static StatusInfoPPx statusInfo;
	private static StoreInfoPPx storeInfo;
	private static ThreadInfoPPx threadInfo;

	public static PypeReportMessagePPx pypeReportMessage() {
		if (pypeReportMessage == null) {
			pypeReportMessage = new PypeReportMessagePPx(ReportInstanceProcess.P_PypeReportMessage);
		}
		return pypeReportMessage;
	}

	public static ReportInfoPPx reportInfo() {
		if (reportInfo == null) {
			reportInfo = new ReportInfoPPx(ReportInstanceProcess.P_ReportInfo);
		}
		return reportInfo;
	}

	public static ReportInstanceProcessorInfoPPx reportInstanceProcessorInfo() {
		if (reportInstanceProcessorInfo == null) {
			reportInstanceProcessorInfo = new ReportInstanceProcessorInfoPPx(ReportInstanceProcess.P_ReportInstanceProcessorInfo);
		}
		return reportInstanceProcessorInfo;
	}

	public static ReportInstanceProcessStepPPx reportInstanceProcessSteps() {
		if (reportInstanceProcessSteps == null) {
			reportInstanceProcessSteps = new ReportInstanceProcessStepPPx(ReportInstanceProcess.P_ReportInstanceProcessSteps);
		}
		return reportInstanceProcessSteps;
	}

	public static StatusInfoPPx statusInfo() {
		if (statusInfo == null) {
			statusInfo = new StatusInfoPPx(ReportInstanceProcess.P_StatusInfo);
		}
		return statusInfo;
	}

	public static StoreInfoPPx storeInfo() {
		if (storeInfo == null) {
			storeInfo = new StoreInfoPPx(ReportInstanceProcess.P_StoreInfo);
		}
		return storeInfo;
	}

	public static ThreadInfoPPx threadInfo() {
		if (threadInfo == null) {
			threadInfo = new ThreadInfoPPx(ReportInstanceProcess.P_ThreadInfo);
		}
		return threadInfo;
	}

	public static String id() {
		String s = ReportInstanceProcess.P_Id;
		return s;
	}

	public static String created() {
		String s = ReportInstanceProcess.P_Created;
		return s;
	}

	public static String counter() {
		String s = ReportInstanceProcess.P_Counter;
		return s;
	}

	public static String completed() {
		String s = ReportInstanceProcess.P_Completed;
		return s;
	}

	public static String fixedJson() {
		String s = ReportInstanceProcess.P_FixedJson;
		return s;
	}

	public static String pypeError() {
		String s = ReportInstanceProcess.P_PypeError;
		return s;
	}

	public static String processingError() {
		String s = ReportInstanceProcess.P_ProcessingError;
		return s;
	}

	public static String convertToJson() {
		String s = "convertToJson";
		return s;
	}

	public static String pp() {
		return ""; // this
	}
}
