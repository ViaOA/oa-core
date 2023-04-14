package com.auto.dev.reportercorp.model.oa.propertypath;

import java.io.Serializable;

import com.auto.dev.reportercorp.model.oa.ThreadInfo;

public class ThreadInfoPPx implements PPxInterface, Serializable {
	private static final long serialVersionUID = 1L;
	public final String pp; // propertyPath

	public ThreadInfoPPx(String name) {
		this(null, name);
	}

	public ThreadInfoPPx(PPxInterface parent, String name) {
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
		ReporterCorpPPx ppx = new ReporterCorpPPx(this, ThreadInfo.P_ReporterCorp);
		return ppx;
	}

	public ReportInstanceProcessPPx reportInstanceProcesses() {
		ReportInstanceProcessPPx ppx = new ReportInstanceProcessPPx(this, ThreadInfo.P_ReportInstanceProcesses);
		return ppx;
	}

	public StatusInfoPPx statusInfo() {
		StatusInfoPPx ppx = new StatusInfoPPx(this, ThreadInfo.P_StatusInfo);
		return ppx;
	}

	public String id() {
		return pp + "." + ThreadInfo.P_Id;
	}

	public String created() {
		return pp + "." + ThreadInfo.P_Created;
	}

	public String name() {
		return pp + "." + ThreadInfo.P_Name;
	}

	public String paused() {
		return pp + "." + ThreadInfo.P_Paused;
	}

	public String stackTrace() {
		return pp + "." + ThreadInfo.P_StackTrace;
	}

	public String active() {
		return pp + "." + ThreadInfo.P_Active;
	}

	public String lastActive() {
		return pp + "." + ThreadInfo.P_LastActive;
	}

	public String activeCount() {
		return pp + "." + ThreadInfo.P_ActiveCount;
	}

	public String pause() {
		return pp + ".pause";
	}

	public String unpause() {
		return pp + ".unpause";
	}

	@Override
	public String toString() {
		return pp;
	}

	public String pp() {
		return pp;
	}
}
