package com.auto.dev.reportercorp.model.oa.propertypath;

import com.auto.dev.reportercorp.model.oa.ReportInstanceData;

public class ReportInstanceDataPP {
	private static ReportInstancePPx calcReportInstance;

	public static ReportInstancePPx calcReportInstance() {
		if (calcReportInstance == null) {
			calcReportInstance = new ReportInstancePPx(ReportInstanceData.P_CalcReportInstance);
		}
		return calcReportInstance;
	}

	public static String id() {
		String s = ReportInstanceData.P_Id;
		return s;
	}

	public static String created() {
		String s = ReportInstanceData.P_Created;
		return s;
	}

	public static String type() {
		String s = ReportInstanceData.P_Type;
		return s;
	}

	public static String printNow() {
		String s = ReportInstanceData.P_PrintNow;
		return s;
	}

	public static String printerId() {
		String s = ReportInstanceData.P_PrinterId;
		return s;
	}

	public static String reportData() {
		String s = ReportInstanceData.P_ReportData;
		return s;
	}

	public static String reportName() {
		String s = ReportInstanceData.P_ReportName;
		return s;
	}

	public static String storeNumber() {
		String s = ReportInstanceData.P_StoreNumber;
		return s;
	}

	public static String businessDate() {
		String s = ReportInstanceData.P_BusinessDate;
		return s;
	}

	public static String processingDate() {
		String s = ReportInstanceData.P_ProcessingDate;
		return s;
	}

	public static String additionalParameters() {
		String s = ReportInstanceData.P_AdditionalParameters;
		return s;
	}

	public static String pp() {
		return ""; // this
	}
}
