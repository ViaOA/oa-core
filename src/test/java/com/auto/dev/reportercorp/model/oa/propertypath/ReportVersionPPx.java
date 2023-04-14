package com.auto.dev.reportercorp.model.oa.propertypath;

import java.io.Serializable;

import com.auto.dev.reportercorp.model.oa.ReportVersion;

public class ReportVersionPPx implements PPxInterface, Serializable {
	private static final long serialVersionUID = 1L;
	public final String pp; // propertyPath

	public ReportVersionPPx(String name) {
		this(null, name);
	}

	public ReportVersionPPx(PPxInterface parent, String name) {
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

	public ReportVersionPPx parentReportVersion() {
		ReportVersionPPx ppx = new ReportVersionPPx(this, ReportVersion.P_ParentReportVersion);
		return ppx;
	}

	public ReportInstancePPx reportInstances() {
		ReportInstancePPx ppx = new ReportInstancePPx(this, ReportVersion.P_ReportInstances);
		return ppx;
	}

	public ReportTemplatePPx reportTemplate() {
		ReportTemplatePPx ppx = new ReportTemplatePPx(this, ReportVersion.P_ReportTemplate);
		return ppx;
	}

	public ReportVersionPPx subReportVersions() {
		ReportVersionPPx ppx = new ReportVersionPPx(this, ReportVersion.P_SubReportVersions);
		return ppx;
	}

	public String id() {
		return pp + "." + ReportVersion.P_Id;
	}

	public String created() {
		return pp + "." + ReportVersion.P_Created;
	}

	public String verified() {
		return pp + "." + ReportVersion.P_Verified;
	}

	public String calcVersionDisplay() {
		return pp + "." + ReportVersion.P_CalcVersionDisplay;
	}

	public String allowSubReportVersion() {
		return pp + "." + ReportVersion.P_AllowSubReportVersion;
	}

	public String calcUnique() {
		return pp + "." + ReportVersion.P_CalcUnique;
	}

	@Override
	public String toString() {
		return pp;
	}

	public String pp() {
		return pp;
	}
}
