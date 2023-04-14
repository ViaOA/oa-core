package com.auto.dev.reportercorp.model.oa.propertypath;

import com.auto.dev.reportercorp.model.oa.StoreInfo;

public class StoreInfoPP {
	private static ReporterCorpPPx reporterCorp;
	private static ReportInstanceProcessPPx reportInstanceProcesses;
	private static StatusInfoPPx statusInfo;

	public static ReporterCorpPPx reporterCorp() {
		if (reporterCorp == null) {
			reporterCorp = new ReporterCorpPPx(StoreInfo.P_ReporterCorp);
		}
		return reporterCorp;
	}

	public static ReportInstanceProcessPPx reportInstanceProcesses() {
		if (reportInstanceProcesses == null) {
			reportInstanceProcesses = new ReportInstanceProcessPPx(StoreInfo.P_ReportInstanceProcesses);
		}
		return reportInstanceProcesses;
	}

	public static StatusInfoPPx statusInfo() {
		if (statusInfo == null) {
			statusInfo = new StatusInfoPPx(StoreInfo.P_StatusInfo);
		}
		return statusInfo;
	}

	public static String id() {
		String s = StoreInfo.P_Id;
		return s;
	}

	public static String created() {
		String s = StoreInfo.P_Created;
		return s;
	}

	public static String storeNumber() {
		String s = StoreInfo.P_StoreNumber;
		return s;
	}

	public static String pp() {
		return ""; // this
	}
}
