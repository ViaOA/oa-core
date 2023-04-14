package com.auto.dev.reportercorp.model.oa.propertypath;

import com.auto.dev.reportercorp.model.oa.ReporterCorp;

public class ReporterCorpPP {
	private static EnvironmentPPx environment;
	private static ReporterCorpParamPPx reporterCorpParams;
	private static ReportInstanceProcessorInfoPPx reportInstanceProcessorInfo;
	private static StatusInfoPPx statusInfo;
	private static StoreInfoPPx storeInfos;
	private static ThreadInfoPPx threadInfos;

	public static EnvironmentPPx environment() {
		if (environment == null) {
			environment = new EnvironmentPPx(ReporterCorp.P_Environment);
		}
		return environment;
	}

	public static ReporterCorpParamPPx reporterCorpParams() {
		if (reporterCorpParams == null) {
			reporterCorpParams = new ReporterCorpParamPPx(ReporterCorp.P_ReporterCorpParams);
		}
		return reporterCorpParams;
	}

	public static ReportInstanceProcessorInfoPPx reportInstanceProcessorInfo() {
		if (reportInstanceProcessorInfo == null) {
			reportInstanceProcessorInfo = new ReportInstanceProcessorInfoPPx(ReporterCorp.P_ReportInstanceProcessorInfo);
		}
		return reportInstanceProcessorInfo;
	}

	public static StatusInfoPPx statusInfo() {
		if (statusInfo == null) {
			statusInfo = new StatusInfoPPx(ReporterCorp.P_StatusInfo);
		}
		return statusInfo;
	}

	public static StoreInfoPPx storeInfos() {
		if (storeInfos == null) {
			storeInfos = new StoreInfoPPx(ReporterCorp.P_StoreInfos);
		}
		return storeInfos;
	}

	public static ThreadInfoPPx threadInfos() {
		if (threadInfos == null) {
			threadInfos = new ThreadInfoPPx(ReporterCorp.P_ThreadInfos);
		}
		return threadInfos;
	}

	public static String id() {
		String s = ReporterCorp.P_Id;
		return s;
	}

	public static String created() {
		String s = ReporterCorp.P_Created;
		return s;
	}

	public static String lastSync() {
		String s = ReporterCorp.P_LastSync;
		return s;
	}

	public static String stopped() {
		String s = ReporterCorp.P_Stopped;
		return s;
	}

	public static String nodeName() {
		String s = ReporterCorp.P_NodeName;
		return s;
	}

	public static String baseUrl() {
		String s = ReporterCorp.P_BaseUrl;
		return s;
	}

	public static String paused() {
		String s = ReporterCorp.P_Paused;
		return s;
	}

	public static String console() {
		String s = ReporterCorp.P_Console;
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

	public static String clearCache() {
		String s = "clearCache";
		return s;
	}

	public static String getPojoReports() {
		String s = "getPojoReports";
		return s;
	}

	public static String resizeCacheObjects() {
		String s = "resizeCacheObjects";
		return s;
	}

	public static String pp() {
		return ""; // this
	}
}
