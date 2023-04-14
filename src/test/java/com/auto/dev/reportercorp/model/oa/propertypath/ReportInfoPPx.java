package com.auto.dev.reportercorp.model.oa.propertypath;

import java.io.Serializable;

import com.auto.dev.reportercorp.model.oa.ReportInfo;

public class ReportInfoPPx implements PPxInterface, Serializable {
	private static final long serialVersionUID = 1L;
	public final String pp; // propertyPath

	public ReportInfoPPx(String name) {
		this(null, name);
	}

	public ReportInfoPPx(PPxInterface parent, String name) {
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
		ReportPPx ppx = new ReportPPx(this, ReportInfo.P_Report);
		return ppx;
	}

	public ReportInstanceProcessPPx reportInstanceProcesses() {
		ReportInstanceProcessPPx ppx = new ReportInstanceProcessPPx(this, ReportInfo.P_ReportInstanceProcesses);
		return ppx;
	}

	public ReportInstanceProcessorInfoPPx reportInstanceProcessorInfo() {
		ReportInstanceProcessorInfoPPx ppx = new ReportInstanceProcessorInfoPPx(this, ReportInfo.P_ReportInstanceProcessorInfo);
		return ppx;
	}

	public String id() {
		return pp + "." + ReportInfo.P_Id;
	}

	public String created() {
		return pp + "." + ReportInfo.P_Created;
	}

	public String receivedCount() {
		return pp + "." + ReportInfo.P_ReceivedCount;
	}

	public String processedCount() {
		return pp + "." + ReportInfo.P_ProcessedCount;
	}

	public String fixedJsonCount() {
		return pp + "." + ReportInfo.P_FixedJsonCount;
	}

	public String processingErrorCount() {
		return pp + "." + ReportInfo.P_ProcessingErrorCount;
	}

	@Override
	public String toString() {
		return pp;
	}

	public String pp() {
		return pp;
	}
}
