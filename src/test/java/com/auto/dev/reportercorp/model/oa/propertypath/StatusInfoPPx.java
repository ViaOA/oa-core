package com.auto.dev.reportercorp.model.oa.propertypath;

import java.io.Serializable;

import com.auto.dev.reportercorp.model.oa.StatusInfo;

public class StatusInfoPPx implements PPxInterface, Serializable {
	private static final long serialVersionUID = 1L;
	public final String pp; // propertyPath

	public StatusInfoPPx(String name) {
		this(null, name);
	}

	public StatusInfoPPx(PPxInterface parent, String name) {
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
		ReporterCorpPPx ppx = new ReporterCorpPPx(this, StatusInfo.P_ReporterCorp);
		return ppx;
	}

	public ReportInstanceProcessPPx reportInstanceProcess() {
		ReportInstanceProcessPPx ppx = new ReportInstanceProcessPPx(this, StatusInfo.P_ReportInstanceProcess);
		return ppx;
	}

	public ReportInstanceProcessorInfoPPx reportInstanceProcessorInfo() {
		ReportInstanceProcessorInfoPPx ppx = new ReportInstanceProcessorInfoPPx(this, StatusInfo.P_ReportInstanceProcessorInfo);
		return ppx;
	}

	public StatusInfoMessagePPx statusInfoActivityMessages() {
		StatusInfoMessagePPx ppx = new StatusInfoMessagePPx(this, StatusInfo.P_StatusInfoActivityMessages);
		return ppx;
	}

	public StatusInfoMessagePPx statusInfoAlertMessages() {
		StatusInfoMessagePPx ppx = new StatusInfoMessagePPx(this, StatusInfo.P_StatusInfoAlertMessages);
		return ppx;
	}

	public StatusInfoMessagePPx statusInfoMessages() {
		StatusInfoMessagePPx ppx = new StatusInfoMessagePPx(this, StatusInfo.P_StatusInfoMessages);
		return ppx;
	}

	public StoreInfoPPx storeInfo() {
		StoreInfoPPx ppx = new StoreInfoPPx(this, StatusInfo.P_StoreInfo);
		return ppx;
	}

	public ThreadInfoPPx threadInfo() {
		ThreadInfoPPx ppx = new ThreadInfoPPx(this, StatusInfo.P_ThreadInfo);
		return ppx;
	}

	public String id() {
		return pp + "." + StatusInfo.P_Id;
	}

	public String created() {
		return pp + "." + StatusInfo.P_Created;
	}

	public String status() {
		return pp + "." + StatusInfo.P_Status;
	}

	public String lastStatus() {
		return pp + "." + StatusInfo.P_LastStatus;
	}

	public String statusCount() {
		return pp + "." + StatusInfo.P_StatusCount;
	}

	public String alert() {
		return pp + "." + StatusInfo.P_Alert;
	}

	public String lastAlert() {
		return pp + "." + StatusInfo.P_LastAlert;
	}

	public String alertCount() {
		return pp + "." + StatusInfo.P_AlertCount;
	}

	public String activity() {
		return pp + "." + StatusInfo.P_Activity;
	}

	public String lastActivity() {
		return pp + "." + StatusInfo.P_LastActivity;
	}

	public String activityCount() {
		return pp + "." + StatusInfo.P_ActivityCount;
	}

	public String console() {
		return pp + "." + StatusInfo.P_Console;
	}

	@Override
	public String toString() {
		return pp;
	}

	public String pp() {
		return pp;
	}
}
