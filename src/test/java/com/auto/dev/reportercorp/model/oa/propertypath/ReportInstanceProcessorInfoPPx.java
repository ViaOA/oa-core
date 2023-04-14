package com.auto.dev.reportercorp.model.oa.propertypath;

import java.io.Serializable;

import com.auto.dev.reportercorp.model.oa.ReportInstanceProcessorInfo;

public class ReportInstanceProcessorInfoPPx implements PPxInterface, Serializable {
	private static final long serialVersionUID = 1L;
	public final String pp; // propertyPath

	public ReportInstanceProcessorInfoPPx(String name) {
		this(null, name);
	}

	public ReportInstanceProcessorInfoPPx(PPxInterface parent, String name) {
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

	public ReporterCorpPPx reporterCorp() {
		ReporterCorpPPx ppx = new ReporterCorpPPx(this, ReportInstanceProcessorInfo.P_ReporterCorp);
		return ppx;
	}

	public ReportInfoPPx reportInfos() {
		ReportInfoPPx ppx = new ReportInfoPPx(this, ReportInstanceProcessorInfo.P_ReportInfos);
		return ppx;
	}

	public ReportInstanceProcessPPx reportInstanceProcesses() {
		ReportInstanceProcessPPx ppx = new ReportInstanceProcessPPx(this, ReportInstanceProcessorInfo.P_ReportInstanceProcesses);
		return ppx;
	}

	public StatusInfoPPx statusInfo() {
		StatusInfoPPx ppx = new StatusInfoPPx(this, ReportInstanceProcessorInfo.P_StatusInfo);
		return ppx;
	}

	public String id() {
		return pp + "." + ReportInstanceProcessorInfo.P_Id;
	}

	public String created() {
		return pp + "." + ReportInstanceProcessorInfo.P_Created;
	}

	public String paused() {
		return pp + "." + ReportInstanceProcessorInfo.P_Paused;
	}

	public String receivedCount() {
		return pp + "." + ReportInstanceProcessorInfo.P_ReceivedCount;
	}

	public String processedCount() {
		return pp + "." + ReportInstanceProcessorInfo.P_ProcessedCount;
	}

	public String fixedJsonCount() {
		return pp + "." + ReportInstanceProcessorInfo.P_FixedJsonCount;
	}

	public String pypeErrorCount() {
		return pp + "." + ReportInstanceProcessorInfo.P_PypeErrorCount;
	}

	public String processingErrorCount() {
		return pp + "." + ReportInstanceProcessorInfo.P_ProcessingErrorCount;
	}

	@Override
	public String toString() {
		return pp;
	}

	public String pp() {
		return pp;
	}
}
