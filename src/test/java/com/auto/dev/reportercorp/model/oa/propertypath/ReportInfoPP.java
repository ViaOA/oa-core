package com.auto.dev.reportercorp.model.oa.propertypath;

import com.auto.dev.reportercorp.model.oa.ReportInfo;

public class ReportInfoPP {
	private static ReportPPx report;
	private static ReportInstanceProcessPPx reportInstanceProcesses;
	private static ReportInstanceProcessorInfoPPx reportInstanceProcessorInfo;

	public static ReportPPx report() {
		if (report == null) {
			report = new ReportPPx(ReportInfo.P_Report);
		}
		return report;
	}

	public static ReportInstanceProcessPPx reportInstanceProcesses() {
		if (reportInstanceProcesses == null) {
			reportInstanceProcesses = new ReportInstanceProcessPPx(ReportInfo.P_ReportInstanceProcesses);
		}
		return reportInstanceProcesses;
	}

	public static ReportInstanceProcessorInfoPPx reportInstanceProcessorInfo() {
		if (reportInstanceProcessorInfo == null) {
			reportInstanceProcessorInfo = new ReportInstanceProcessorInfoPPx(ReportInfo.P_ReportInstanceProcessorInfo);
		}
		return reportInstanceProcessorInfo;
	}

	public static String id() {
		String s = ReportInfo.P_Id;
		return s;
	}

	public static String created() {
		String s = ReportInfo.P_Created;
		return s;
	}

	public static String receivedCount() {
		String s = ReportInfo.P_ReceivedCount;
		return s;
	}

	public static String processedCount() {
		String s = ReportInfo.P_ProcessedCount;
		return s;
	}

	public static String fixedJsonCount() {
		String s = ReportInfo.P_FixedJsonCount;
		return s;
	}

	public static String processingErrorCount() {
		String s = ReportInfo.P_ProcessingErrorCount;
		return s;
	}

	public static String pp() {
		return ""; // this
	}
}
