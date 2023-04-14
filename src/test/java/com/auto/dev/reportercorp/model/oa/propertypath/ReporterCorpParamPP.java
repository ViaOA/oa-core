package com.auto.dev.reportercorp.model.oa.propertypath;

import com.auto.dev.reportercorp.model.oa.ReporterCorpParam;

public class ReporterCorpParamPP {
	private static ReporterCorpPPx reporterCorp;

	public static ReporterCorpPPx reporterCorp() {
		if (reporterCorp == null) {
			reporterCorp = new ReporterCorpPPx(ReporterCorpParam.P_ReporterCorp);
		}
		return reporterCorp;
	}

	public static String id() {
		String s = ReporterCorpParam.P_Id;
		return s;
	}

	public static String created() {
		String s = ReporterCorpParam.P_Created;
		return s;
	}

	public static String type() {
		String s = ReporterCorpParam.P_Type;
		return s;
	}

	public static String name() {
		String s = ReporterCorpParam.P_Name;
		return s;
	}

	public static String value() {
		String s = ReporterCorpParam.P_Value;
		return s;
	}

	public static String pp() {
		return ""; // this
	}
}
