package com.auto.dev.reportercorp.model.oa.propertypath;

import java.io.Serializable;

import com.auto.dev.reportercorp.model.oa.StoreImport;

public class StoreImportPPx implements PPxInterface, Serializable {
	private static final long serialVersionUID = 1L;
	public final String pp; // propertyPath

	public StoreImportPPx(String name) {
		this(null, name);
	}

	public StoreImportPPx(PPxInterface parent, String name) {
		String s = null;
		if (parent != null) {
			s = parent.toString();
		}
		if (s == null) {
			s = "";
		}
		if (name != null && name.length() > 0) {
			if (s.length() > 0 && name.charAt(0) != ':') {
				s += ".";
			}
			s += name;
		}
		pp = s;
	}

	public StoreImportReportPPx storeImportReports() {
		StoreImportReportPPx ppx = new StoreImportReportPPx(this, StoreImport.P_StoreImportReports);
		return ppx;
	}

	public StoreImportTemplatePPx storeImportTemplates() {
		StoreImportTemplatePPx ppx = new StoreImportTemplatePPx(this, StoreImport.P_StoreImportTemplates);
		return ppx;
	}

	public String id() {
		return pp + "." + StoreImport.P_Id;
	}

	public String created() {
		return pp + "." + StoreImport.P_Created;
	}

	public String reporterCorpServerBaseUrl() {
		return pp + "." + StoreImport.P_ReporterCorpServerBaseUrl;
	}

	public String console() {
		return pp + "." + StoreImport.P_Console;
	}

	public String hasCorpReportsToUpdate() {
		return pp + "." + StoreImport.P_HasCorpReportsToUpdate;
	}

	public String hasCorpTemplatesToUpdate() {
		return pp + "." + StoreImport.P_HasCorpTemplatesToUpdate;
	}

	public String loadReportsFromStore() {
		return pp + ".loadReportsFromStore";
	}

	public String loadTemplatesFromStore() {
		return pp + ".loadTemplatesFromStore";
	}

	public String updateCorpReports() {
		return pp + ".updateCorpReports";
	}

	public String updateCorpTemplates() {
		return pp + ".updateCorpTemplates";
	}

	@Override
	public String toString() {
		return pp;
	}

	public String pp() {
		return pp;
	}
}
