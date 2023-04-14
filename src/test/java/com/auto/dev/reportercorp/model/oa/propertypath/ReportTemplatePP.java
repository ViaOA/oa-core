package com.auto.dev.reportercorp.model.oa.propertypath;

import com.auto.dev.reportercorp.model.oa.ReportTemplate;

public class ReportTemplatePP {
	private static ReportPPx report;
	private static ReportVersionPPx reportVersions;
	private static StoreImportTemplatePPx storeImportTemplates;

	public static ReportPPx report() {
		if (report == null) {
			report = new ReportPPx(ReportTemplate.P_Report);
		}
		return report;
	}

	public static ReportVersionPPx reportVersions() {
		if (reportVersions == null) {
			reportVersions = new ReportVersionPPx(ReportTemplate.P_ReportVersions);
		}
		return reportVersions;
	}

	public static StoreImportTemplatePPx storeImportTemplates() {
		if (storeImportTemplates == null) {
			storeImportTemplates = new StoreImportTemplatePPx(ReportTemplate.P_StoreImportTemplates);
		}
		return storeImportTemplates;
	}

	public static String id() {
		String s = ReportTemplate.P_Id;
		return s;
	}

	public static String created() {
		String s = ReportTemplate.P_Created;
		return s;
	}

	public static String md5hash() {
		String s = ReportTemplate.P_Md5hash;
		return s;
	}

	public static String template() {
		String s = ReportTemplate.P_Template;
		return s;
	}

	public static String verified() {
		String s = ReportTemplate.P_Verified;
		return s;
	}

	public static String calcMd5hashPrefix() {
		String s = ReportTemplate.P_CalcMd5hashPrefix;
		return s;
	}

	public static String hasTemplate() {
		String s = ReportTemplate.P_HasTemplate;
		return s;
	}

	public static String templateSize() {
		String s = ReportTemplate.P_TemplateSize;
		return s;
	}

	public static String pp() {
		return ""; // this
	}
}
