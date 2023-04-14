package com.auto.dev.reportercorp.model.oa.propertypath;

import java.io.Serializable;

import com.auto.dev.reportercorp.model.oa.SnapshotReportVersion;

public class SnapshotReportVersionPPx implements PPxInterface, Serializable {
	private static final long serialVersionUID = 1L;
	public final String pp; // propertyPath

	public SnapshotReportVersionPPx(String name) {
		this(null, name);
	}

	public SnapshotReportVersionPPx(PPxInterface parent, String name) {
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

	public SnapshotReportVersionPPx parentSnapshotReportVersion() {
		SnapshotReportVersionPPx ppx = new SnapshotReportVersionPPx(this, SnapshotReportVersion.P_ParentSnapshotReportVersion);
		return ppx;
	}

	public SnapshotReportTemplatePPx snapshotReportTemplate() {
		SnapshotReportTemplatePPx ppx = new SnapshotReportTemplatePPx(this, SnapshotReportVersion.P_SnapshotReportTemplate);
		return ppx;
	}

	public SnapshotReportVersionPPx snapshotReportVersions() {
		SnapshotReportVersionPPx ppx = new SnapshotReportVersionPPx(this, SnapshotReportVersion.P_SnapshotReportVersions);
		return ppx;
	}

	public String id() {
		return pp + "." + SnapshotReportVersion.P_Id;
	}

	public String realId() {
		return pp + "." + SnapshotReportVersion.P_RealId;
	}

	public String created() {
		return pp + "." + SnapshotReportVersion.P_Created;
	}

	public String verified() {
		return pp + "." + SnapshotReportVersion.P_Verified;
	}

	@Override
	public String toString() {
		return pp;
	}

	public String pp() {
		return pp;
	}
}
