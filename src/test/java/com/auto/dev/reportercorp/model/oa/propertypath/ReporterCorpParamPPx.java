package com.auto.dev.reportercorp.model.oa.propertypath;

import java.io.Serializable;

import com.auto.dev.reportercorp.model.oa.ReporterCorpParam;

public class ReporterCorpParamPPx implements PPxInterface, Serializable {
	private static final long serialVersionUID = 1L;
	public final String pp; // propertyPath

	public ReporterCorpParamPPx(String name) {
		this(null, name);
	}

	public ReporterCorpParamPPx(PPxInterface parent, String name) {
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

	public ReporterCorpPPx reporterCorp() {
		ReporterCorpPPx ppx = new ReporterCorpPPx(this, ReporterCorpParam.P_ReporterCorp);
		return ppx;
	}

	public String id() {
		return pp + "." + ReporterCorpParam.P_Id;
	}

	public String created() {
		return pp + "." + ReporterCorpParam.P_Created;
	}

	public String type() {
		return pp + "." + ReporterCorpParam.P_Type;
	}

	public String name() {
		return pp + "." + ReporterCorpParam.P_Name;
	}

	public String value() {
		return pp + "." + ReporterCorpParam.P_Value;
	}

	@Override
	public String toString() {
		return pp;
	}

	public String pp() {
		return pp;
	}
}
