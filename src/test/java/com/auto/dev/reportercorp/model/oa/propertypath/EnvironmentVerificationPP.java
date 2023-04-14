package com.auto.dev.reportercorp.model.oa.propertypath;

import com.auto.dev.reportercorp.model.oa.EnvironmentVerification;

public class EnvironmentVerificationPP {
	private static ReporterCorpVerificationPPx reporterCorpVerifications;
	private static VerificationPPx verification;

	public static ReporterCorpVerificationPPx reporterCorpVerifications() {
		if (reporterCorpVerifications == null) {
			reporterCorpVerifications = new ReporterCorpVerificationPPx(EnvironmentVerification.P_ReporterCorpVerifications);
		}
		return reporterCorpVerifications;
	}

	public static VerificationPPx verification() {
		if (verification == null) {
			verification = new VerificationPPx(EnvironmentVerification.P_Verification);
		}
		return verification;
	}

	public static String id() {
		String s = EnvironmentVerification.P_Id;
		return s;
	}

	public static String created() {
		String s = EnvironmentVerification.P_Created;
		return s;
	}

	public static String pp() {
		return ""; // this
	}
}
