package com.auto.dev.reportercorp.model.oa.propertypath;

import java.io.Serializable;

import com.auto.dev.reportercorp.model.oa.SnapshotReportTemplate;

public class SnapshotReportTemplatePPx implements PPxInterface, Serializable {
	private static final long serialVersionUID = 1L;
	public final String pp; // propertyPath

	public SnapshotReportTemplatePPx(String name) {
		this(null, name);
	}

	public SnapshotReportTemplatePPx(PPxInterface parent, String name) {
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

	public SnapshotReportPPx snapshotReport() {
		SnapshotReportPPx ppx = new SnapshotReportPPx(this, SnapshotReportTemplate.P_SnapshotReport);
		return ppx;
	}

	public SnapshotReportVersionPPx snapshotReportVersions() {
		SnapshotReportVersionPPx ppx = new SnapshotReportVersionPPx(this, SnapshotReportTemplate.P_SnapshotReportVersions);
		return ppx;
	}

	public String id() {
		return pp + "." + SnapshotReportTemplate.P_Id;
	}

	public String realId() {
		return pp + "." + SnapshotReportTemplate.P_RealId;
	}

	public String created() {
		return pp + "." + SnapshotReportTemplate.P_Created;
	}

	public String md5hash() {
		return pp + "." + SnapshotReportTemplate.P_Md5hash;
	}

	public String template() {
		return pp + "." + SnapshotReportTemplate.P_Template;
	}

	public String verified() {
		return pp + "." + SnapshotReportTemplate.P_Verified;
	}

	@Override
	public String toString() {
		return pp;
	}

	public String pp() {
		return pp;
	}
}
