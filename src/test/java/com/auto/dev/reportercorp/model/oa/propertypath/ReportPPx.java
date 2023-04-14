package com.auto.dev.reportercorp.model.oa.propertypath;

import java.io.Serializable;

import com.auto.dev.reportercorp.model.oa.Report;

public class ReportPPx implements PPxInterface, Serializable {
	private static final long serialVersionUID = 1L;
	public final String pp; // propertyPath

	public ReportPPx(String name) {
		this(null, name);
	}

	public ReportPPx(PPxInterface parent, String name) {
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

	public ReportPPx parentReport() {
		ReportPPx ppx = new ReportPPx(this, Report.P_ParentReport);
		return ppx;
	}

	public ReportInfoPPx reportInfos() {
		ReportInfoPPx ppx = new ReportInfoPPx(this, Report.P_ReportInfos);
		return ppx;
	}

	public ReportTemplatePPx reportTemplates() {
		ReportTemplatePPx ppx = new ReportTemplatePPx(this, Report.P_ReportTemplates);
		return ppx;
	}

	public StoreImportReportPPx storeImportReports() {
		StoreImportReportPPx ppx = new StoreImportReportPPx(this, Report.P_StoreImportReports);
		return ppx;
	}

	public ReportPPx subReports() {
		ReportPPx ppx = new ReportPPx(this, Report.P_SubReports);
		return ppx;
	}

	public String id() {
		return pp + "." + Report.P_Id;
	}

	public String created() {
		return pp + "." + Report.P_Created;
	}

	public String name() {
		return pp + "." + Report.P_Name;
	}

	public String title() {
		return pp + "." + Report.P_Title;
	}

	public String description() {
		return pp + "." + Report.P_Description;
	}

	public String composite() {
		return pp + "." + Report.P_Composite;
	}

	public String fileName() {
		return pp + "." + Report.P_FileName;
	}

	public String type() {
		return pp + "." + Report.P_Type;
	}

	public String packet() {
		return pp + "." + Report.P_Packet;
	}

	public String verified() {
		return pp + "." + Report.P_Verified;
	}

	public String display() {
		return pp + "." + Report.P_Display;
	}

	public String category() {
		return pp + "." + Report.P_Category;
	}

	public String retentionDays() {
		return pp + "." + Report.P_RetentionDays;
	}

	public String subReportSeq() {
		return pp + "." + Report.P_SubReportSeq;
	}

	public String calcDisplay() {
		return pp + "." + Report.P_CalcDisplay;
	}

	public ReportPPx masterOnlyFilter() {
		ReportPPx ppx = new ReportPPx(this, ":masterOnly()");
		return ppx;
	}

	public ReportPPx needsTemplateFilter() {
		ReportPPx ppx = new ReportPPx(this, ":needsTemplate()");
		return ppx;
	}

	@Override
	public String toString() {
		return pp;
	}

	public String pp() {
		return pp;
	}
}
