package com.auto.dev.reportercorp.model.oa.propertypath;

import java.io.Serializable;

import com.auto.dev.reportercorp.model.oa.StoreInfo;

public class StoreInfoPPx implements PPxInterface, Serializable {
	private static final long serialVersionUID = 1L;
	public final String pp; // propertyPath

	public StoreInfoPPx(String name) {
		this(null, name);
	}

	public StoreInfoPPx(PPxInterface parent, String name) {
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
		ReporterCorpPPx ppx = new ReporterCorpPPx(this, StoreInfo.P_ReporterCorp);
		return ppx;
	}

	public ReportInstanceProcessPPx reportInstanceProcesses() {
		ReportInstanceProcessPPx ppx = new ReportInstanceProcessPPx(this, StoreInfo.P_ReportInstanceProcesses);
		return ppx;
	}

	public StatusInfoPPx statusInfo() {
		StatusInfoPPx ppx = new StatusInfoPPx(this, StoreInfo.P_StatusInfo);
		return ppx;
	}

	public String id() {
		return pp + "." + StoreInfo.P_Id;
	}

	public String created() {
		return pp + "." + StoreInfo.P_Created;
	}

	public String storeNumber() {
		return pp + "." + StoreInfo.P_StoreNumber;
	}

	@Override
	public String toString() {
		return pp;
	}

	public String pp() {
		return pp;
	}
}
