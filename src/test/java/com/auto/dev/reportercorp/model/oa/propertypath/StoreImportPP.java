package com.auto.dev.reportercorp.model.oa.propertypath;

import com.auto.dev.reportercorp.model.oa.StoreImport;

public class StoreImportPP {
	private static StoreImportReportPPx storeImportReports;
	private static StoreImportTemplatePPx storeImportTemplates;

	public static StoreImportReportPPx storeImportReports() {
		if (storeImportReports == null) {
			storeImportReports = new StoreImportReportPPx(StoreImport.P_StoreImportReports);
		}
		return storeImportReports;
	}

	public static StoreImportTemplatePPx storeImportTemplates() {
		if (storeImportTemplates == null) {
			storeImportTemplates = new StoreImportTemplatePPx(StoreImport.P_StoreImportTemplates);
		}
		return storeImportTemplates;
	}

	public static String id() {
		String s = StoreImport.P_Id;
		return s;
	}

	public static String created() {
		String s = StoreImport.P_Created;
		return s;
	}

	public static String reporterCorpServerBaseUrl() {
		String s = StoreImport.P_ReporterCorpServerBaseUrl;
		return s;
	}

	public static String console() {
		String s = StoreImport.P_Console;
		return s;
	}

	public static String hasCorpReportsToUpdate() {
		String s = StoreImport.P_HasCorpReportsToUpdate;
		return s;
	}

	public static String hasCorpTemplatesToUpdate() {
		String s = StoreImport.P_HasCorpTemplatesToUpdate;
		return s;
	}

	public static String loadReportsFromStore() {
		String s = "loadReportsFromStore";
		return s;
	}

	public static String loadTemplatesFromStore() {
		String s = "loadTemplatesFromStore";
		return s;
	}

	public static String updateCorpReports() {
		String s = "updateCorpReports";
		return s;
	}

	public static String updateCorpTemplates() {
		String s = "updateCorpTemplates";
		return s;
	}

	public static String pp() {
		return ""; // this
	}
}
