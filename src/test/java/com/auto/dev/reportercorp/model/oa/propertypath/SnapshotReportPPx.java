package com.auto.dev.reportercorp.model.oa.propertypath;

import java.io.Serializable;

import com.auto.dev.reportercorp.model.oa.SnapshotReport;

public class SnapshotReportPPx implements PPxInterface, Serializable {
	private static final long serialVersionUID = 1L;
	public final String pp; // propertyPath

	public SnapshotReportPPx(String name) {
		this(null, name);
	}

	public SnapshotReportPPx(PPxInterface parent, String name) {
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

	public EnvironmentSnapshotPPx environmentSnapshot() {
		EnvironmentSnapshotPPx ppx = new EnvironmentSnapshotPPx(this, SnapshotReport.P_EnvironmentSnapshot);
		return ppx;
	}

	public SnapshotReportPPx parentSnapshotReport() {
		SnapshotReportPPx ppx = new SnapshotReportPPx(this, SnapshotReport.P_ParentSnapshotReport);
		return ppx;
	}

	public SnapshotReportPPx snapshotReports() {
		SnapshotReportPPx ppx = new SnapshotReportPPx(this, SnapshotReport.P_SnapshotReports);
		return ppx;
	}

	public SnapshotReportTemplatePPx snapshotReportTemplates() {
		SnapshotReportTemplatePPx ppx = new SnapshotReportTemplatePPx(this, SnapshotReport.P_SnapshotReportTemplates);
		return ppx;
	}

	public String id() {
		return pp + "." + SnapshotReport.P_Id;
	}

	public String realId() {
		return pp + "." + SnapshotReport.P_RealId;
	}

	public String created() {
		return pp + "." + SnapshotReport.P_Created;
	}

	public String name() {
		return pp + "." + SnapshotReport.P_Name;
	}

	public String title() {
		return pp + "." + SnapshotReport.P_Title;
	}

	public String description() {
		return pp + "." + SnapshotReport.P_Description;
	}

	public String composite() {
		return pp + "." + SnapshotReport.P_Composite;
	}

	public String fileName() {
		return pp + "." + SnapshotReport.P_FileName;
	}

	public String type() {
		return pp + "." + SnapshotReport.P_Type;
	}

	public String packet() {
		return pp + "." + SnapshotReport.P_Packet;
	}

	public String verified() {
		return pp + "." + SnapshotReport.P_Verified;
	}

	public String display() {
		return pp + "." + SnapshotReport.P_Display;
	}

	public String category() {
		return pp + "." + SnapshotReport.P_Category;
	}

	public String retentionDays() {
		return pp + "." + SnapshotReport.P_RetentionDays;
	}

	public String subReportSeq() {
		return pp + "." + SnapshotReport.P_SubReportSeq;
	}

	@Override
	public String toString() {
		return pp;
	}

	public String pp() {
		return pp;
	}
}
