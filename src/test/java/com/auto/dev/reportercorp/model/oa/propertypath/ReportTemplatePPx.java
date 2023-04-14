package com.auto.dev.reportercorp.model.oa.propertypath;

import java.io.Serializable;

import com.auto.dev.reportercorp.model.oa.ReportTemplate;

public class ReportTemplatePPx implements PPxInterface, Serializable {
	private static final long serialVersionUID = 1L;
	public final String pp; // propertyPath

	public ReportTemplatePPx(String name) {
		this(null, name);
	}

	public ReportTemplatePPx(PPxInterface parent, String name) {
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

	public ReportPPx report() {
		ReportPPx ppx = new ReportPPx(this, ReportTemplate.P_Report);
		return ppx;
	}

	public ReportVersionPPx reportVersions() {
		ReportVersionPPx ppx = new ReportVersionPPx(this, ReportTemplate.P_ReportVersions);
		return ppx;
	}

	public StoreImportTemplatePPx storeImportTemplates() {
		StoreImportTemplatePPx ppx = new StoreImportTemplatePPx(this, ReportTemplate.P_StoreImportTemplates);
		return ppx;
	}

	public String id() {
		return pp + "." + ReportTemplate.P_Id;
	}

	public String created() {
		return pp + "." + ReportTemplate.P_Created;
	}

	public String md5hash() {
		return pp + "." + ReportTemplate.P_Md5hash;
	}

	public String template() {
		return pp + "." + ReportTemplate.P_Template;
	}

	public String verified() {
		return pp + "." + ReportTemplate.P_Verified;
	}

	public String calcMd5hashPrefix() {
		return pp + "." + ReportTemplate.P_CalcMd5hashPrefix;
	}

	public String hasTemplate() {
		return pp + "." + ReportTemplate.P_HasTemplate;
	}

	public String templateSize() {
		return pp + "." + ReportTemplate.P_TemplateSize;
	}

	public ReportTemplatePPx needsTemplateFilter() {
		ReportTemplatePPx ppx = new ReportTemplatePPx(this, ":needsTemplate()");
		return ppx;
	}

	@Override
	public String toString() {
		return pp;
	}

	public String pp() {
		return pp;
	}
}
