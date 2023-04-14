package com.auto.dev.reportercorp.model.oa.propertypath;

import com.auto.dev.reportercorp.model.oa.ReportInstance;

public class ReportInstancePP {
	private static ReportInstanceDataPPx calcReportInstanceData;
	private static ReportInstancePPx compositeReportInstances;
	private static ReportInstancePPx parentCompositeReportInstance;
	private static PypeReportMessagePPx pypeReportMessage;
	private static ReportVersionPPx reportVersion;

	public static ReportInstanceDataPPx calcReportInstanceData() {
		if (calcReportInstanceData == null) {
			calcReportInstanceData = new ReportInstanceDataPPx(ReportInstance.P_CalcReportInstanceData);
		}
		return calcReportInstanceData;
	}

	public static ReportInstancePPx compositeReportInstances() {
		if (compositeReportInstances == null) {
			compositeReportInstances = new ReportInstancePPx(ReportInstance.P_CompositeReportInstances);
		}
		return compositeReportInstances;
	}

	public static ReportInstancePPx parentCompositeReportInstance() {
		if (parentCompositeReportInstance == null) {
			parentCompositeReportInstance = new ReportInstancePPx(ReportInstance.P_ParentCompositeReportInstance);
		}
		return parentCompositeReportInstance;
	}

	public static PypeReportMessagePPx pypeReportMessage() {
		if (pypeReportMessage == null) {
			pypeReportMessage = new PypeReportMessagePPx(ReportInstance.P_PypeReportMessage);
		}
		return pypeReportMessage;
	}

	public static ReportVersionPPx reportVersion() {
		if (reportVersion == null) {
			reportVersion = new ReportVersionPPx(ReportInstance.P_ReportVersion);
		}
		return reportVersion;
	}

	public static String id() {
		String s = ReportInstance.P_Id;
		return s;
	}

	public static String created() {
		String s = ReportInstance.P_Created;
		return s;
	}

	public static String storeNumber() {
		String s = ReportInstance.P_StoreNumber;
		return s;
	}

	public static String fileName() {
		String s = ReportInstance.P_FileName;
		return s;
	}

	public static String title() {
		String s = ReportInstance.P_Title;
		return s;
	}

	public static String compositePos() {
		String s = ReportInstance.P_CompositePos;
		return s;
	}

	public static String data() {
		String s = ReportInstance.P_Data;
		return s;
	}

	public static String calcStoreNumber() {
		String s = ReportInstance.P_CalcStoreNumber;
		return s;
	}

	public static String calcReportName() {
		String s = ReportInstance.P_CalcReportName;
		return s;
	}

	public static String calcPdfUrl() {
		String s = ReportInstance.P_CalcPdfUrl;
		return s;
	}

	public static String pp() {
		return ""; // this
	}
}
