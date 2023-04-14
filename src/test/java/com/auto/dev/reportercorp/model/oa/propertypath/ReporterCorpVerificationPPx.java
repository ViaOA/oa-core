package com.auto.dev.reportercorp.model.oa.propertypath;

import java.io.Serializable;

import com.auto.dev.reportercorp.model.oa.ReporterCorpVerification;

public class ReporterCorpVerificationPPx implements PPxInterface, Serializable {
	private static final long serialVersionUID = 1L;
	public final String pp; // propertyPath

	public ReporterCorpVerificationPPx(String name) {
		this(null, name);
	}

	public ReporterCorpVerificationPPx(PPxInterface parent, String name) {
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

	public EnvironmentVerificationPPx environmentVerification() {
		EnvironmentVerificationPPx ppx = new EnvironmentVerificationPPx(this, ReporterCorpVerification.P_EnvironmentVerification);
		return ppx;
	}

	public String id() {
		return pp + "." + ReporterCorpVerification.P_Id;
	}

	public String created() {
		return pp + "." + ReporterCorpVerification.P_Created;
	}

	@Override
	public String toString() {
		return pp;
	}

	public String pp() {
		return pp;
	}
}
