package com.auto.dev.reportercorp.model.oa.propertypath;

import com.auto.dev.reportercorp.model.oa.ReportInstanceProcessorInfo;

public class ReportInstanceProcessorInfoPP {
	private static ReporterCorpPPx reporterCorp;
	private static ReportInfoPPx reportInfos;
	private static ReportInstanceProcessPPx reportInstanceProcesses;
	private static StatusInfoPPx statusInfo;

	public static ReporterCorpPPx reporterCorp() {
		if (reporterCorp == null) {
			reporterCorp = new ReporterCorpPPx(ReportInstanceProcessorInfo.P_ReporterCorp);
		}
		return reporterCorp;
	}

	public static ReportInfoPPx reportInfos() {
		if (reportInfos == null) {
			reportInfos = new ReportInfoPPx(ReportInstanceProcessorInfo.P_ReportInfos);
		}
		return reportInfos;
	}

	public static ReportInstanceProcessPPx reportInstanceProcesses() {
		if (reportInstanceProcesses == null) {
			reportInstanceProcesses = new ReportInstanceProcessPPx(ReportInstanceProcessorInfo.P_ReportInstanceProcesses);
		}
		return reportInstanceProcesses;
	}

	public static StatusInfoPPx statusInfo() {
		if (statusInfo == null) {
			statusInfo = new StatusInfoPPx(ReportInstanceProcessorInfo.P_StatusInfo);
		}
		return statusInfo;
	}

	public static String id() {
		String s = ReportInstanceProcessorInfo.P_Id;
		return s;
	}

	public static String created() {
		String s = ReportInstanceProcessorInfo.P_Created;
		return s;
	}

	public static String paused() {
		String s = ReportInstanceProcessorInfo.P_Paused;
		return s;
	}

	public static String receivedCount() {
		String s = ReportInstanceProcessorInfo.P_ReceivedCount;
		return s;
	}

	public static String processedCount() {
		String s = ReportInstanceProcessorInfo.P_ProcessedCount;
		return s;
	}

	public static String fixedJsonCount() {
		String s = ReportInstanceProcessorInfo.P_FixedJsonCount;
		return s;
	}

	public static String pypeErrorCount() {
		String s = ReportInstanceProcessorInfo.P_PypeErrorCount;
		return s;
	}

	public static String processingErrorCount() {
		String s = ReportInstanceProcessorInfo.P_ProcessingErrorCount;
		return s;
	}

	public static String pp() {
		return ""; // this
	}
}
