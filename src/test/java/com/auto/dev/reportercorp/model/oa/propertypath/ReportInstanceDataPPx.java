package com.auto.dev.reportercorp.model.oa.propertypath;

import java.io.Serializable;

import com.auto.dev.reportercorp.model.oa.ReportInstanceData;

public class ReportInstanceDataPPx implements PPxInterface, Serializable {
	private static final long serialVersionUID = 1L;
	public final String pp; // propertyPath

	public ReportInstanceDataPPx(String name) {
		this(null, name);
	}

	public ReportInstanceDataPPx(PPxInterface parent, String name) {
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

	public ReportInstancePPx calcReportInstance() {
		ReportInstancePPx ppx = new ReportInstancePPx(this, ReportInstanceData.P_CalcReportInstance);
		return ppx;
	}

	public String id() {
		return pp + "." + ReportInstanceData.P_Id;
	}

	public String created() {
		return pp + "." + ReportInstanceData.P_Created;
	}

	public String type() {
		return pp + "." + ReportInstanceData.P_Type;
	}

	public String printNow() {
		return pp + "." + ReportInstanceData.P_PrintNow;
	}

	public String printerId() {
		return pp + "." + ReportInstanceData.P_PrinterId;
	}

	public String reportData() {
		return pp + "." + ReportInstanceData.P_ReportData;
	}

	public String reportName() {
		return pp + "." + ReportInstanceData.P_ReportName;
	}

	public String storeNumber() {
		return pp + "." + ReportInstanceData.P_StoreNumber;
	}

	public String businessDate() {
		return pp + "." + ReportInstanceData.P_BusinessDate;
	}

	public String processingDate() {
		return pp + "." + ReportInstanceData.P_ProcessingDate;
	}

	public String additionalParameters() {
		return pp + "." + ReportInstanceData.P_AdditionalParameters;
	}

	@Override
	public String toString() {
		return pp;
	}

	public String pp() {
		return pp;
	}
}
