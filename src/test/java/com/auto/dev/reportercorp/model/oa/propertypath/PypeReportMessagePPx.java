package com.auto.dev.reportercorp.model.oa.propertypath;

import java.io.Serializable;

import com.auto.dev.reportercorp.model.oa.PypeReportMessage;

public class PypeReportMessagePPx implements PPxInterface, Serializable {
	private static final long serialVersionUID = 1L;
	public final String pp; // propertyPath

	public PypeReportMessagePPx(String name) {
		this(null, name);
	}

	public PypeReportMessagePPx(PPxInterface parent, String name) {
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

	public ReportInstancePPx reportInstance() {
		ReportInstancePPx ppx = new ReportInstancePPx(this, PypeReportMessage.P_ReportInstance);
		return ppx;
	}

	public ReportInstanceProcessPPx reportInstanceProcess() {
		ReportInstanceProcessPPx ppx = new ReportInstanceProcessPPx(this, PypeReportMessage.P_ReportInstanceProcess);
		return ppx;
	}

	public String id() {
		return pp + "." + PypeReportMessage.P_Id;
	}

	public String created() {
		return pp + "." + PypeReportMessage.P_Created;
	}

	public String store() {
		return pp + "." + PypeReportMessage.P_Store;
	}

	public String processingDate() {
		return pp + "." + PypeReportMessage.P_ProcessingDate;
	}

	public String title() {
		return pp + "." + PypeReportMessage.P_Title;
	}

	public String template() {
		return pp + "." + PypeReportMessage.P_Template;
	}

	public String filename() {
		return pp + "." + PypeReportMessage.P_Filename;
	}

	public String data() {
		return pp + "." + PypeReportMessage.P_Data;
	}

	public String subreports() {
		return pp + "." + PypeReportMessage.P_Subreports;
	}

	public String convertedDate() {
		return pp + "." + PypeReportMessage.P_ConvertedDate;
	}

	public String status() {
		return pp + "." + PypeReportMessage.P_Status;
	}

	public String sendToPypeEndpoint() {
		return pp + ".sendToPypeEndpoint";
	}

	@Override
	public String toString() {
		return pp;
	}

	public String pp() {
		return pp;
	}
}
