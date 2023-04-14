package com.auto.dev.reportercorp.model.oa.propertypath;

import java.io.Serializable;

import com.auto.dev.reportercorp.model.oa.EnvironmentSnapshot;

public class EnvironmentSnapshotPPx implements PPxInterface, Serializable {
	private static final long serialVersionUID = 1L;
	public final String pp; // propertyPath

	public EnvironmentSnapshotPPx(String name) {
		this(null, name);
	}

	public EnvironmentSnapshotPPx(PPxInterface parent, String name) {
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

	public EnvironmentPPx environment() {
		EnvironmentPPx ppx = new EnvironmentPPx(this, EnvironmentSnapshot.P_Environment);
		return ppx;
	}

	public SnapshotReportPPx snapshotReports() {
		SnapshotReportPPx ppx = new SnapshotReportPPx(this, EnvironmentSnapshot.P_SnapshotReports);
		return ppx;
	}

	public String id() {
		return pp + "." + EnvironmentSnapshot.P_Id;
	}

	public String created() {
		return pp + "." + EnvironmentSnapshot.P_Created;
	}

	@Override
	public String toString() {
		return pp;
	}

	public String pp() {
		return pp;
	}
}
