package com.auto.dev.reportercorp.model.oa.propertypath;

import com.auto.dev.reportercorp.model.oa.SnapshotReportTemplate;

public class SnapshotReportTemplatePP {
	private static SnapshotReportPPx snapshotReport;
	private static SnapshotReportVersionPPx snapshotReportVersions;

	public static SnapshotReportPPx snapshotReport() {
		if (snapshotReport == null) {
			snapshotReport = new SnapshotReportPPx(SnapshotReportTemplate.P_SnapshotReport);
		}
		return snapshotReport;
	}

	public static SnapshotReportVersionPPx snapshotReportVersions() {
		if (snapshotReportVersions == null) {
			snapshotReportVersions = new SnapshotReportVersionPPx(SnapshotReportTemplate.P_SnapshotReportVersions);
		}
		return snapshotReportVersions;
	}

	public static String id() {
		String s = SnapshotReportTemplate.P_Id;
		return s;
	}

	public static String realId() {
		String s = SnapshotReportTemplate.P_RealId;
		return s;
	}

	public static String created() {
		String s = SnapshotReportTemplate.P_Created;
		return s;
	}

	public static String md5hash() {
		String s = SnapshotReportTemplate.P_Md5hash;
		return s;
	}

	public static String template() {
		String s = SnapshotReportTemplate.P_Template;
		return s;
	}

	public static String verified() {
		String s = SnapshotReportTemplate.P_Verified;
		return s;
	}

	public static String pp() {
		return ""; // this
	}
}
