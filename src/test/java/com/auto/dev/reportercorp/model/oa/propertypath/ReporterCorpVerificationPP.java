package com.auto.dev.reportercorp.model.oa.propertypath;

import com.auto.dev.reportercorp.model.oa.ReporterCorpVerification;

public class ReporterCorpVerificationPP {
	private static EnvironmentVerificationPPx environmentVerification;

	public static EnvironmentVerificationPPx environmentVerification() {
		if (environmentVerification == null) {
			environmentVerification = new EnvironmentVerificationPPx(ReporterCorpVerification.P_EnvironmentVerification);
		}
		return environmentVerification;
	}

	public static String id() {
		String s = ReporterCorpVerification.P_Id;
		return s;
	}

	public static String created() {
		String s = ReporterCorpVerification.P_Created;
		return s;
	}

	public static String pp() {
		return ""; // this
	}
}
