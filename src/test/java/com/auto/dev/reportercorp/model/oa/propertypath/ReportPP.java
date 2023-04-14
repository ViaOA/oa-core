package com.auto.dev.reportercorp.model.oa.propertypath;

import com.auto.dev.reportercorp.model.oa.Report;

public class ReportPP {
	private static ReportPPx parentReport;
	private static ReportInfoPPx reportInfos;
	private static ReportTemplatePPx reportTemplates;
	private static StoreImportReportPPx storeImportReports;
	private static ReportPPx subReports;

	public static ReportPPx parentReport() {
		if (parentReport == null) {
			parentReport = new ReportPPx(Report.P_ParentReport);
		}
		return parentReport;
	}

	public static ReportInfoPPx reportInfos() {
		if (reportInfos == null) {
			reportInfos = new ReportInfoPPx(Report.P_ReportInfos);
		}
		return reportInfos;
	}

	public static ReportTemplatePPx reportTemplates() {
		if (reportTemplates == null) {
			reportTemplates = new ReportTemplatePPx(Report.P_ReportTemplates);
		}
		return reportTemplates;
	}

	public static StoreImportReportPPx storeImportReports() {
		if (storeImportReports == null) {
			storeImportReports = new StoreImportReportPPx(Report.P_StoreImportReports);
		}
		return storeImportReports;
	}

	public static ReportPPx subReports() {
		if (subReports == null) {
			subReports = new ReportPPx(Report.P_SubReports);
		}
		return subReports;
	}

	public static String id() {
		String s = Report.P_Id;
		return s;
	}

	public static String created() {
		String s = Report.P_Created;
		return s;
	}

	public static String name() {
		String s = Report.P_Name;
		return s;
	}

	public static String title() {
		String s = Report.P_Title;
		return s;
	}

	public static String description() {
		String s = Report.P_Description;
		return s;
	}

	public static String composite() {
		String s = Report.P_Composite;
		return s;
	}

	public static String fileName() {
		String s = Report.P_FileName;
		return s;
	}

	public static String type() {
		String s = Report.P_Type;
		return s;
	}

	public static String packet() {
		String s = Report.P_Packet;
		return s;
	}

	public static String verified() {
		String s = Report.P_Verified;
		return s;
	}

	public static String display() {
		String s = Report.P_Display;
		return s;
	}

	public static String category() {
		String s = Report.P_Category;
		return s;
	}

	public static String retentionDays() {
		String s = Report.P_RetentionDays;
		return s;
	}

	public static String subReportSeq() {
		String s = Report.P_SubReportSeq;
		return s;
	}

	public static String calcDisplay() {
		String s = Report.P_CalcDisplay;
		return s;
	}

	public static String pp() {
		return ""; // this
	}
}
