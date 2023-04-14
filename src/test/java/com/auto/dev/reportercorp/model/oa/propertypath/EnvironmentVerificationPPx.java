package com.auto.dev.reportercorp.model.oa.propertypath;

import java.io.Serializable;

import com.auto.dev.reportercorp.model.oa.EnvironmentVerification;

public class EnvironmentVerificationPPx implements PPxInterface, Serializable {
	private static final long serialVersionUID = 1L;
	public final String pp; // propertyPath

	public EnvironmentVerificationPPx(String name) {
		this(null, name);
	}

	public EnvironmentVerificationPPx(PPxInterface parent, String name) {
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

	public ReporterCorpVerificationPPx reporterCorpVerifications() {
		ReporterCorpVerificationPPx ppx = new ReporterCorpVerificationPPx(this, EnvironmentVerification.P_ReporterCorpVerifications);
		return ppx;
	}

	public VerificationPPx verification() {
		VerificationPPx ppx = new VerificationPPx(this, EnvironmentVerification.P_Verification);
		return ppx;
	}

	public String id() {
		return pp + "." + EnvironmentVerification.P_Id;
	}

	public String created() {
		return pp + "." + EnvironmentVerification.P_Created;
	}

	@Override
	public String toString() {
		return pp;
	}

	public String pp() {
		return pp;
	}
}
