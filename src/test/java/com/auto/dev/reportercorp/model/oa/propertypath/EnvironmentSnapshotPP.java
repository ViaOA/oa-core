package com.auto.dev.reportercorp.model.oa.propertypath;

import com.auto.dev.reportercorp.model.oa.EnvironmentSnapshot;

public class EnvironmentSnapshotPP {
	private static EnvironmentPPx environment;
	private static SnapshotReportPPx snapshotReports;

	public static EnvironmentPPx environment() {
		if (environment == null) {
			environment = new EnvironmentPPx(EnvironmentSnapshot.P_Environment);
		}
		return environment;
	}

	public static SnapshotReportPPx snapshotReports() {
		if (snapshotReports == null) {
			snapshotReports = new SnapshotReportPPx(EnvironmentSnapshot.P_SnapshotReports);
		}
		return snapshotReports;
	}

	public static String id() {
		String s = EnvironmentSnapshot.P_Id;
		return s;
	}

	public static String created() {
		String s = EnvironmentSnapshot.P_Created;
		return s;
	}

	public static String pp() {
		return ""; // this
	}
}
