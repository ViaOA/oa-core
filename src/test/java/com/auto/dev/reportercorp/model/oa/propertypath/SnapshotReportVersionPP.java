package com.auto.dev.reportercorp.model.oa.propertypath;

import com.auto.dev.reportercorp.model.oa.SnapshotReportVersion;

public class SnapshotReportVersionPP {
	private static SnapshotReportVersionPPx parentSnapshotReportVersion;
	private static SnapshotReportTemplatePPx snapshotReportTemplate;
	private static SnapshotReportVersionPPx snapshotReportVersions;

	public static SnapshotReportVersionPPx parentSnapshotReportVersion() {
		if (parentSnapshotReportVersion == null) {
			parentSnapshotReportVersion = new SnapshotReportVersionPPx(SnapshotReportVersion.P_ParentSnapshotReportVersion);
		}
		return parentSnapshotReportVersion;
	}

	public static SnapshotReportTemplatePPx snapshotReportTemplate() {
		if (snapshotReportTemplate == null) {
			snapshotReportTemplate = new SnapshotReportTemplatePPx(SnapshotReportVersion.P_SnapshotReportTemplate);
		}
		return snapshotReportTemplate;
	}

	public static SnapshotReportVersionPPx snapshotReportVersions() {
		if (snapshotReportVersions == null) {
			snapshotReportVersions = new SnapshotReportVersionPPx(SnapshotReportVersion.P_SnapshotReportVersions);
		}
		return snapshotReportVersions;
	}

	public static String id() {
		String s = SnapshotReportVersion.P_Id;
		return s;
	}

	public static String realId() {
		String s = SnapshotReportVersion.P_RealId;
		return s;
	}

	public static String created() {
		String s = SnapshotReportVersion.P_Created;
		return s;
	}

	public static String verified() {
		String s = SnapshotReportVersion.P_Verified;
		return s;
	}

	public static String pp() {
		return ""; // this
	}
}
