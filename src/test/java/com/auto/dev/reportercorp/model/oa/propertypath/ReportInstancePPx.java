package com.auto.dev.reportercorp.model.oa.propertypath;

import java.io.Serializable;

import com.auto.dev.reportercorp.model.oa.ReportInstance;

public class ReportInstancePPx implements PPxInterface, Serializable {
	private static final long serialVersionUID = 1L;
	public final String pp; // propertyPath

	public ReportInstancePPx(String name) {
		this(null, name);
	}

	public ReportInstancePPx(PPxInterface parent, String name) {
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

	public ReportInstanceDataPPx calcReportInstanceData() {
		ReportInstanceDataPPx ppx = new ReportInstanceDataPPx(this, ReportInstance.P_CalcReportInstanceData);
		return ppx;
	}

	public ReportInstancePPx compositeReportInstances() {
		ReportInstancePPx ppx = new ReportInstancePPx(this, ReportInstance.P_CompositeReportInstances);
		return ppx;
	}

	public ReportInstancePPx parentCompositeReportInstance() {
		ReportInstancePPx ppx = new ReportInstancePPx(this, ReportInstance.P_ParentCompositeReportInstance);
		return ppx;
	}

	public PypeReportMessagePPx pypeReportMessage() {
		PypeReportMessagePPx ppx = new PypeReportMessagePPx(this, ReportInstance.P_PypeReportMessage);
		return ppx;
	}

	public ReportVersionPPx reportVersion() {
		ReportVersionPPx ppx = new ReportVersionPPx(this, ReportInstance.P_ReportVersion);
		return ppx;
	}

	public String id() {
		return pp + "." + ReportInstance.P_Id;
	}

	public String created() {
		return pp + "." + ReportInstance.P_Created;
	}

	public String storeNumber() {
		return pp + "." + ReportInstance.P_StoreNumber;
	}

	public String fileName() {
		return pp + "." + ReportInstance.P_FileName;
	}

	public String title() {
		return pp + "." + ReportInstance.P_Title;
	}

	public String compositePos() {
		return pp + "." + ReportInstance.P_CompositePos;
	}

	public String data() {
		return pp + "." + ReportInstance.P_Data;
	}

	public String calcStoreNumber() {
		return pp + "." + ReportInstance.P_CalcStoreNumber;
	}

	public String calcReportName() {
		return pp + "." + ReportInstance.P_CalcReportName;
	}

	public String calcPdfUrl() {
		return pp + "." + ReportInstance.P_CalcPdfUrl;
	}

	@Override
	public String toString() {
		return pp;
	}

	public String pp() {
		return pp;
	}
}
