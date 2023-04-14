package com.auto.dev.reportercorp.model.oa.propertypath;

import com.auto.dev.reportercorp.model.oa.StoreImportReport;

public class StoreImportReportPP {
	private static ReportPPx report;
	private static StoreImportPPx storeImport;

	public static ReportPPx report() {
		if (report == null) {
			report = new ReportPPx(StoreImportReport.P_Report);
		}
		return report;
	}

	public static StoreImportPPx storeImport() {
		if (storeImport == null) {
			storeImport = new StoreImportPPx(StoreImportReport.P_StoreImport);
		}
		return storeImport;
	}

	public static String id() {
		String s = StoreImportReport.P_Id;
		return s;
	}

	public static String created() {
		String s = StoreImportReport.P_Created;
		return s;
	}

	public static String name() {
		String s = StoreImportReport.P_Name;
		return s;
	}

	public static String fileName() {
		String s = StoreImportReport.P_FileName;
		return s;
	}

	public static String title() {
		String s = StoreImportReport.P_Title;
		return s;
	}

	public static String description() {
		String s = StoreImportReport.P_Description;
		return s;
	}

	public static String retentionDays() {
		String s = StoreImportReport.P_RetentionDays;
		return s;
	}

	public static String type() {
		String s = StoreImportReport.P_Type;
		return s;
	}

	public static String md5hash() {
		String s = StoreImportReport.P_Md5hash;
		return s;
	}

	public static String updateType() {
		String s = StoreImportReport.P_UpdateType;
		return s;
	}

	public static String updated() {
		String s = StoreImportReport.P_Updated;
		return s;
	}

	public static String pp() {
		return ""; // this
	}
}
