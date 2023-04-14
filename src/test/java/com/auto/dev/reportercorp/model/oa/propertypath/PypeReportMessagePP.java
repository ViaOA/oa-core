package com.auto.dev.reportercorp.model.oa.propertypath;

import com.auto.dev.reportercorp.model.oa.PypeReportMessage;

public class PypeReportMessagePP {
	private static ReportInstancePPx reportInstance;
	private static ReportInstanceProcessPPx reportInstanceProcess;

	public static ReportInstancePPx reportInstance() {
		if (reportInstance == null) {
			reportInstance = new ReportInstancePPx(PypeReportMessage.P_ReportInstance);
		}
		return reportInstance;
	}

	public static ReportInstanceProcessPPx reportInstanceProcess() {
		if (reportInstanceProcess == null) {
			reportInstanceProcess = new ReportInstanceProcessPPx(PypeReportMessage.P_ReportInstanceProcess);
		}
		return reportInstanceProcess;
	}

	public static String id() {
		String s = PypeReportMessage.P_Id;
		return s;
	}

	public static String created() {
		String s = PypeReportMessage.P_Created;
		return s;
	}

	public static String store() {
		String s = PypeReportMessage.P_Store;
		return s;
	}

	public static String processingDate() {
		String s = PypeReportMessage.P_ProcessingDate;
		return s;
	}

	public static String title() {
		String s = PypeReportMessage.P_Title;
		return s;
	}

	public static String template() {
		String s = PypeReportMessage.P_Template;
		return s;
	}

	public static String filename() {
		String s = PypeReportMessage.P_Filename;
		return s;
	}

	public static String data() {
		String s = PypeReportMessage.P_Data;
		return s;
	}

	public static String subreports() {
		String s = PypeReportMessage.P_Subreports;
		return s;
	}

	public static String convertedDate() {
		String s = PypeReportMessage.P_ConvertedDate;
		return s;
	}

	public static String status() {
		String s = PypeReportMessage.P_Status;
		return s;
	}

	public static String sendToPypeEndpoint() {
		String s = "sendToPypeEndpoint";
		return s;
	}

	public static String pp() {
		return ""; // this
	}
}
