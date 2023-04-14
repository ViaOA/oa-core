package com.auto.dev.reportercorp.model.oa.propertypath;

import com.auto.dev.reportercorp.model.oa.ReportVersion;

public class ReportVersionPP {
	private static ReportVersionPPx parentReportVersion;
	private static ReportInstancePPx reportInstances;
	private static ReportTemplatePPx reportTemplate;
	private static ReportVersionPPx subReportVersions;

	public static ReportVersionPPx parentReportVersion() {
		if (parentReportVersion == null) {
			parentReportVersion = new ReportVersionPPx(ReportVersion.P_ParentReportVersion);
		}
		return parentReportVersion;
	}

	public static ReportInstancePPx reportInstances() {
		if (reportInstances == null) {
			reportInstances = new ReportInstancePPx(ReportVersion.P_ReportInstances);
		}
		return reportInstances;
	}

	public static ReportTemplatePPx reportTemplate() {
		if (reportTemplate == null) {
			reportTemplate = new ReportTemplatePPx(ReportVersion.P_ReportTemplate);
		}
		return reportTemplate;
	}

	public static ReportVersionPPx subReportVersions() {
		if (subReportVersions == null) {
			subReportVersions = new ReportVersionPPx(ReportVersion.P_SubReportVersions);
		}
		return subReportVersions;
	}

	public static String id() {
		String s = ReportVersion.P_Id;
		return s;
	}

	public static String created() {
		String s = ReportVersion.P_Created;
		return s;
	}

	public static String verified() {
		String s = ReportVersion.P_Verified;
		return s;
	}

	public static String calcVersionDisplay() {
		String s = ReportVersion.P_CalcVersionDisplay;
		return s;
	}

	public static String allowSubReportVersion() {
		String s = ReportVersion.P_AllowSubReportVersion;
		return s;
	}

	public static String calcUnique() {
		String s = ReportVersion.P_CalcUnique;
		return s;
	}

	public static String pp() {
		return ""; // this
	}
}
