package com.auto.dev.reportercorp.model.oa.propertypath;

import com.auto.dev.reportercorp.model.oa.Verification;

public class VerificationPP {
	private static EnvironmentVerificationPPx environmentVerifications;

	public static EnvironmentVerificationPPx environmentVerifications() {
		if (environmentVerifications == null) {
			environmentVerifications = new EnvironmentVerificationPPx(Verification.P_EnvironmentVerifications);
		}
		return environmentVerifications;
	}

	public static String id() {
		String s = Verification.P_Id;
		return s;
	}

	public static String created() {
		String s = Verification.P_Created;
		return s;
	}

	public static String pp() {
		return ""; // this
	}
}
