package com.auto.dev.reportercorp.model.oa.propertypath;

import java.io.Serializable;

import com.auto.dev.reportercorp.model.oa.StoreImportReport;

public class StoreImportReportPPx implements PPxInterface, Serializable {
	private static final long serialVersionUID = 1L;
	public final String pp; // propertyPath

	public StoreImportReportPPx(String name) {
		this(null, name);
	}

	public StoreImportReportPPx(PPxInterface parent, String name) {
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

	public ReportPPx report() {
		ReportPPx ppx = new ReportPPx(this, StoreImportReport.P_Report);
		return ppx;
	}

	public StoreImportPPx storeImport() {
		StoreImportPPx ppx = new StoreImportPPx(this, StoreImportReport.P_StoreImport);
		return ppx;
	}

	public String id() {
		return pp + "." + StoreImportReport.P_Id;
	}

	public String created() {
		return pp + "." + StoreImportReport.P_Created;
	}

	public String name() {
		return pp + "." + StoreImportReport.P_Name;
	}

	public String fileName() {
		return pp + "." + StoreImportReport.P_FileName;
	}

	public String title() {
		return pp + "." + StoreImportReport.P_Title;
	}

	public String description() {
		return pp + "." + StoreImportReport.P_Description;
	}

	public String retentionDays() {
		return pp + "." + StoreImportReport.P_RetentionDays;
	}

	public String type() {
		return pp + "." + StoreImportReport.P_Type;
	}

	public String md5hash() {
		return pp + "." + StoreImportReport.P_Md5hash;
	}

	public String updateType() {
		return pp + "." + StoreImportReport.P_UpdateType;
	}

	public String updated() {
		return pp + "." + StoreImportReport.P_Updated;
	}

	@Override
	public String toString() {
		return pp;
	}

	public String pp() {
		return pp;
	}
}
