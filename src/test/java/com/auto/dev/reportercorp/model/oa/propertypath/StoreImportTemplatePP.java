package com.auto.dev.reportercorp.model.oa.propertypath;

import com.auto.dev.reportercorp.model.oa.StoreImportTemplate;

public class StoreImportTemplatePP {
	private static ReportTemplatePPx reportTemplate;
	private static StoreImportPPx storeImport;

	public static ReportTemplatePPx reportTemplate() {
		if (reportTemplate == null) {
			reportTemplate = new ReportTemplatePPx(StoreImportTemplate.P_ReportTemplate);
		}
		return reportTemplate;
	}

	public static StoreImportPPx storeImport() {
		if (storeImport == null) {
			storeImport = new StoreImportPPx(StoreImportTemplate.P_StoreImport);
		}
		return storeImport;
	}

	public static String id() {
		String s = StoreImportTemplate.P_Id;
		return s;
	}

	public static String created() {
		String s = StoreImportTemplate.P_Created;
		return s;
	}

	public static String md5hash() {
		String s = StoreImportTemplate.P_Md5hash;
		return s;
	}

	public static String template() {
		String s = StoreImportTemplate.P_Template;
		return s;
	}

	public static String updateType() {
		String s = StoreImportTemplate.P_UpdateType;
		return s;
	}

	public static String updated() {
		String s = StoreImportTemplate.P_Updated;
		return s;
	}

	public static String pp() {
		return ""; // this
	}
}
