package com.auto.dev.reportercorp.model.oa.propertypath;

import java.io.Serializable;

import com.auto.dev.reportercorp.model.oa.ReporterCorp;

public class ReporterCorpPPx implements PPxInterface, Serializable {
	private static final long serialVersionUID = 1L;
	public final String pp; // propertyPath

	public ReporterCorpPPx(String name) {
		this(null, name);
	}

	public ReporterCorpPPx(PPxInterface parent, String name) {
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
		EnvironmentPPx ppx = new EnvironmentPPx(this, ReporterCorp.P_Environment);
		return ppx;
	}

	public ReporterCorpParamPPx reporterCorpParams() {
		ReporterCorpParamPPx ppx = new ReporterCorpParamPPx(this, ReporterCorp.P_ReporterCorpParams);
		return ppx;
	}

	public ReportInstanceProcessorInfoPPx reportInstanceProcessorInfo() {
		ReportInstanceProcessorInfoPPx ppx = new ReportInstanceProcessorInfoPPx(this, ReporterCorp.P_ReportInstanceProcessorInfo);
		return ppx;
	}

	public StatusInfoPPx statusInfo() {
		StatusInfoPPx ppx = new StatusInfoPPx(this, ReporterCorp.P_StatusInfo);
		return ppx;
	}

	public StoreInfoPPx storeInfos() {
		StoreInfoPPx ppx = new StoreInfoPPx(this, ReporterCorp.P_StoreInfos);
		return ppx;
	}

	public ThreadInfoPPx threadInfos() {
		ThreadInfoPPx ppx = new ThreadInfoPPx(this, ReporterCorp.P_ThreadInfos);
		return ppx;
	}

	public String id() {
		return pp + "." + ReporterCorp.P_Id;
	}

	public String created() {
		return pp + "." + ReporterCorp.P_Created;
	}

	public String lastSync() {
		return pp + "." + ReporterCorp.P_LastSync;
	}

	public String stopped() {
		return pp + "." + ReporterCorp.P_Stopped;
	}

	public String nodeName() {
		return pp + "." + ReporterCorp.P_NodeName;
	}

	public String baseUrl() {
		return pp + "." + ReporterCorp.P_BaseUrl;
	}

	public String paused() {
		return pp + "." + ReporterCorp.P_Paused;
	}

	public String console() {
		return pp + "." + ReporterCorp.P_Console;
	}

	public String pause() {
		return pp + ".pause";
	}

	public String unpause() {
		return pp + ".unpause";
	}

	public String clearCache() {
		return pp + ".clearCache";
	}

	public String getPojoReports() {
		return pp + ".getPojoReports";
	}

	public String resizeCacheObjects() {
		return pp + ".resizeCacheObjects";
	}

	@Override
	public String toString() {
		return pp;
	}

	public String pp() {
		return pp;
	}
}
