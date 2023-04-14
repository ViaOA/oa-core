package com.auto.dev.reportercorp.model.oa.propertypath;

import com.auto.dev.reportercorp.model.oa.Environment;

public class EnvironmentPP {
	private static EnvironmentSnapshotPPx environmentSnapshots;
	private static ReporterCorpPPx reporterCorps;

	public static EnvironmentSnapshotPPx environmentSnapshots() {
		if (environmentSnapshots == null) {
			environmentSnapshots = new EnvironmentSnapshotPPx(Environment.P_EnvironmentSnapshots);
		}
		return environmentSnapshots;
	}

	public static ReporterCorpPPx reporterCorps() {
		if (reporterCorps == null) {
			reporterCorps = new ReporterCorpPPx(Environment.P_ReporterCorps);
		}
		return reporterCorps;
	}

	public static String id() {
		String s = Environment.P_Id;
		return s;
	}

	public static String created() {
		String s = Environment.P_Created;
		return s;
	}

	public static String type() {
		String s = Environment.P_Type;
		return s;
	}

	public static String name() {
		String s = Environment.P_Name;
		return s;
	}

	public static String nodeCount() {
		String s = Environment.P_NodeCount;
		return s;
	}

	public static String loadBalanceUrl() {
		String s = Environment.P_LoadBalanceUrl;
		return s;
	}

	public static String templateUrl() {
		String s = Environment.P_TemplateUrl;
		return s;
	}

	public static String jdbcUrl() {
		String s = Environment.P_JdbcUrl;
		return s;
	}

	public static String color() {
		String s = Environment.P_Color;
		return s;
	}

	public static String isProduction() {
		String s = Environment.P_IsProduction;
		return s;
	}

	public static String currentRuntimeEnvironment() {
		String s = Environment.P_CurrentRuntimeEnvironment;
		return s;
	}

	public static String updateReporterCorps() {
		String s = "updateReporterCorps";
		return s;
	}

	public static String pauseAll() {
		String s = "pauseAll";
		return s;
	}

	public static String unpauseAll() {
		String s = "unpauseAll";
		return s;
	}

	public static String clearCaches() {
		String s = "clearCaches";
		return s;
	}

	public static String createSnapshot() {
		String s = "createSnapshot";
		return s;
	}

	public static String setDefaults() {
		String s = "setDefaults";
		return s;
	}

	public static String pp() {
		return ""; // this
	}
}
