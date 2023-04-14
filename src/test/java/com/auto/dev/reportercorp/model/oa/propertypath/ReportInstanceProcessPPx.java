package com.auto.dev.reportercorp.model.oa.propertypath;

import java.io.Serializable;

import com.auto.dev.reportercorp.model.oa.ReportInstanceProcess;

public class ReportInstanceProcessPPx implements PPxInterface, Serializable {
	private static final long serialVersionUID = 1L;
	public final String pp; // propertyPath

	public ReportInstanceProcessPPx(String name) {
		this(null, name);
	}

	public ReportInstanceProcessPPx(PPxInterface parent, String name) {
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

	public PypeReportMessagePPx pypeReportMessage() {
		PypeReportMessagePPx ppx = new PypeReportMessagePPx(this, ReportInstanceProcess.P_PypeReportMessage);
		return ppx;
	}

	public ReportInfoPPx reportInfo() {
		ReportInfoPPx ppx = new ReportInfoPPx(this, ReportInstanceProcess.P_ReportInfo);
		return ppx;
	}

	public ReportInstanceProcessorInfoPPx reportInstanceProcessorInfo() {
		ReportInstanceProcessorInfoPPx ppx = new ReportInstanceProcessorInfoPPx(this, ReportInstanceProcess.P_ReportInstanceProcessorInfo);
		return ppx;
	}

	public ReportInstanceProcessStepPPx reportInstanceProcessSteps() {
		ReportInstanceProcessStepPPx ppx = new ReportInstanceProcessStepPPx(this, ReportInstanceProcess.P_ReportInstanceProcessSteps);
		return ppx;
	}

	public StatusInfoPPx statusInfo() {
		StatusInfoPPx ppx = new StatusInfoPPx(this, ReportInstanceProcess.P_StatusInfo);
		return ppx;
	}

	public StoreInfoPPx storeInfo() {
		StoreInfoPPx ppx = new StoreInfoPPx(this, ReportInstanceProcess.P_StoreInfo);
		return ppx;
	}

	public ThreadInfoPPx threadInfo() {
		ThreadInfoPPx ppx = new ThreadInfoPPx(this, ReportInstanceProcess.P_ThreadInfo);
		return ppx;
	}

	public String id() {
		return pp + "." + ReportInstanceProcess.P_Id;
	}

	public String created() {
		return pp + "." + ReportInstanceProcess.P_Created;
	}

	public String counter() {
		return pp + "." + ReportInstanceProcess.P_Counter;
	}

	public String completed() {
		return pp + "." + ReportInstanceProcess.P_Completed;
	}

	public String fixedJson() {
		return pp + "." + ReportInstanceProcess.P_FixedJson;
	}

	public String pypeError() {
		return pp + "." + ReportInstanceProcess.P_PypeError;
	}

	public String processingError() {
		return pp + "." + ReportInstanceProcess.P_ProcessingError;
	}

	public String convertToJson() {
		return pp + ".convertToJson";
	}

	@Override
	public String toString() {
		return pp;
	}

	public String pp() {
		return pp;
	}
}
