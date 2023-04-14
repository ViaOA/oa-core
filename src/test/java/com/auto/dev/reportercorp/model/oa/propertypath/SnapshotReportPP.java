package com.auto.dev.reportercorp.model.oa.propertypath;

import com.auto.dev.reportercorp.model.oa.SnapshotReport;

public class SnapshotReportPP {
	private static EnvironmentSnapshotPPx environmentSnapshot;
	private static SnapshotReportPPx parentSnapshotReport;
	private static SnapshotReportPPx snapshotReports;
	private static SnapshotReportTemplatePPx snapshotReportTemplates;

	public static EnvironmentSnapshotPPx environmentSnapshot() {
		if (environmentSnapshot == null) {
			environmentSnapshot = new EnvironmentSnapshotPPx(SnapshotReport.P_EnvironmentSnapshot);
		}
		return environmentSnapshot;
	}

	public static SnapshotReportPPx parentSnapshotReport() {
		if (parentSnapshotReport == null) {
			parentSnapshotReport = new SnapshotReportPPx(SnapshotReport.P_ParentSnapshotReport);
		}
		return parentSnapshotReport;
	}

	public static SnapshotReportPPx snapshotReports() {
		if (snapshotReports == null) {
			snapshotReports = new SnapshotReportPPx(SnapshotReport.P_SnapshotReports);
		}
		return snapshotReports;
	}

	public static SnapshotReportTemplatePPx snapshotReportTemplates() {
		if (snapshotReportTemplates == null) {
			snapshotReportTemplates = new SnapshotReportTemplatePPx(SnapshotReport.P_SnapshotReportTemplates);
		}
		return snapshotReportTemplates;
	}

	public static String id() {
		String s = SnapshotReport.P_Id;
		return s;
	}

	public static String realId() {
		String s = SnapshotReport.P_RealId;
		return s;
	}

	public static String created() {
		String s = SnapshotReport.P_Created;
		return s;
	}

	public static String name() {
		String s = SnapshotReport.P_Name;
		return s;
	}

	public static String title() {
		String s = SnapshotReport.P_Title;
		return s;
	}

	public static String description() {
		String s = SnapshotReport.P_Description;
		return s;
	}

	public static String composite() {
		String s = SnapshotReport.P_Composite;
		return s;
	}

	public static String fileName() {
		String s = SnapshotReport.P_FileName;
		return s;
	}

	public static String type() {
		String s = SnapshotReport.P_Type;
		return s;
	}

	public static String packet() {
		String s = SnapshotReport.P_Packet;
		return s;
	}

	public static String verified() {
		String s = SnapshotReport.P_Verified;
		return s;
	}

	public static String display() {
		String s = SnapshotReport.P_Display;
		return s;
	}

	public static String category() {
		String s = SnapshotReport.P_Category;
		return s;
	}

	public static String retentionDays() {
		String s = SnapshotReport.P_RetentionDays;
		return s;
	}

	public static String subReportSeq() {
		String s = SnapshotReport.P_SubReportSeq;
		return s;
	}

	public static String pp() {
		return ""; // this
	}
}
