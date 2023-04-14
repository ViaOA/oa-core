package com.auto.dev.reportercorp.model.oa.propertypath;

import com.auto.dev.reportercorp.model.oa.ThreadInfo;

public class ThreadInfoPP {
	private static ReporterCorpPPx reporterCorp;
	private static ReportInstanceProcessPPx reportInstanceProcesses;
	private static StatusInfoPPx statusInfo;

	public static ReporterCorpPPx reporterCorp() {
		if (reporterCorp == null) {
			reporterCorp = new ReporterCorpPPx(ThreadInfo.P_ReporterCorp);
		}
		return reporterCorp;
	}

	public static ReportInstanceProcessPPx reportInstanceProcesses() {
		if (reportInstanceProcesses == null) {
			reportInstanceProcesses = new ReportInstanceProcessPPx(ThreadInfo.P_ReportInstanceProcesses);
		}
		return reportInstanceProcesses;
	}

	public static StatusInfoPPx statusInfo() {
		if (statusInfo == null) {
			statusInfo = new StatusInfoPPx(ThreadInfo.P_StatusInfo);
		}
		return statusInfo;
	}

	public static String id() {
		String s = ThreadInfo.P_Id;
		return s;
	}

	public static String created() {
		String s = ThreadInfo.P_Created;
		return s;
	}

	public static String name() {
		String s = ThreadInfo.P_Name;
		return s;
	}

	public static String paused() {
		String s = ThreadInfo.P_Paused;
		return s;
	}

	public static String stackTrace() {
		String s = ThreadInfo.P_StackTrace;
		return s;
	}

	public static String active() {
		String s = ThreadInfo.P_Active;
		return s;
	}

	public static String lastActive() {
		String s = ThreadInfo.P_LastActive;
		return s;
	}

	public static String activeCount() {
		String s = ThreadInfo.P_ActiveCount;
		return s;
	}

	public static String pause() {
		String s = "pause";
		return s;
	}

	public static String unpause() {
		String s = "unpause";
		return s;
	}

	public static String pp() {
		return ""; // this
	}
}
