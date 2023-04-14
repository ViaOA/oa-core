package com.auto.dev.reportercorp.model.oa.propertypath;

import com.auto.dev.reportercorp.model.oa.StatusInfo;

public class StatusInfoPP {
	private static ReporterCorpPPx reporterCorp;
	private static ReportInstanceProcessPPx reportInstanceProcess;
	private static ReportInstanceProcessorInfoPPx reportInstanceProcessorInfo;
	private static StatusInfoMessagePPx statusInfoActivityMessages;
	private static StatusInfoMessagePPx statusInfoAlertMessages;
	private static StatusInfoMessagePPx statusInfoMessages;
	private static StoreInfoPPx storeInfo;
	private static ThreadInfoPPx threadInfo;

	public static ReporterCorpPPx reporterCorp() {
		if (reporterCorp == null) {
			reporterCorp = new ReporterCorpPPx(StatusInfo.P_ReporterCorp);
		}
		return reporterCorp;
	}

	public static ReportInstanceProcessPPx reportInstanceProcess() {
		if (reportInstanceProcess == null) {
			reportInstanceProcess = new ReportInstanceProcessPPx(StatusInfo.P_ReportInstanceProcess);
		}
		return reportInstanceProcess;
	}

	public static ReportInstanceProcessorInfoPPx reportInstanceProcessorInfo() {
		if (reportInstanceProcessorInfo == null) {
			reportInstanceProcessorInfo = new ReportInstanceProcessorInfoPPx(StatusInfo.P_ReportInstanceProcessorInfo);
		}
		return reportInstanceProcessorInfo;
	}

	public static StatusInfoMessagePPx statusInfoActivityMessages() {
		if (statusInfoActivityMessages == null) {
			statusInfoActivityMessages = new StatusInfoMessagePPx(StatusInfo.P_StatusInfoActivityMessages);
		}
		return statusInfoActivityMessages;
	}

	public static StatusInfoMessagePPx statusInfoAlertMessages() {
		if (statusInfoAlertMessages == null) {
			statusInfoAlertMessages = new StatusInfoMessagePPx(StatusInfo.P_StatusInfoAlertMessages);
		}
		return statusInfoAlertMessages;
	}

	public static StatusInfoMessagePPx statusInfoMessages() {
		if (statusInfoMessages == null) {
			statusInfoMessages = new StatusInfoMessagePPx(StatusInfo.P_StatusInfoMessages);
		}
		return statusInfoMessages;
	}

	public static StoreInfoPPx storeInfo() {
		if (storeInfo == null) {
			storeInfo = new StoreInfoPPx(StatusInfo.P_StoreInfo);
		}
		return storeInfo;
	}

	public static ThreadInfoPPx threadInfo() {
		if (threadInfo == null) {
			threadInfo = new ThreadInfoPPx(StatusInfo.P_ThreadInfo);
		}
		return threadInfo;
	}

	public static String id() {
		String s = StatusInfo.P_Id;
		return s;
	}

	public static String created() {
		String s = StatusInfo.P_Created;
		return s;
	}

	public static String status() {
		String s = StatusInfo.P_Status;
		return s;
	}

	public static String lastStatus() {
		String s = StatusInfo.P_LastStatus;
		return s;
	}

	public static String statusCount() {
		String s = StatusInfo.P_StatusCount;
		return s;
	}

	public static String alert() {
		String s = StatusInfo.P_Alert;
		return s;
	}

	public static String lastAlert() {
		String s = StatusInfo.P_LastAlert;
		return s;
	}

	public static String alertCount() {
		String s = StatusInfo.P_AlertCount;
		return s;
	}

	public static String activity() {
		String s = StatusInfo.P_Activity;
		return s;
	}

	public static String lastActivity() {
		String s = StatusInfo.P_LastActivity;
		return s;
	}

	public static String activityCount() {
		String s = StatusInfo.P_ActivityCount;
		return s;
	}

	public static String console() {
		String s = StatusInfo.P_Console;
		return s;
	}

	public static String pp() {
		return ""; // this
	}
}
