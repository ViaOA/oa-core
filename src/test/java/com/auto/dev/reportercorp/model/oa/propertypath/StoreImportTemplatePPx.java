package com.auto.dev.reportercorp.model.oa.propertypath;

import java.io.Serializable;

import com.auto.dev.reportercorp.model.oa.StoreImportTemplate;

public class StoreImportTemplatePPx implements PPxInterface, Serializable {
	private static final long serialVersionUID = 1L;
	public final String pp; // propertyPath

	public StoreImportTemplatePPx(String name) {
		this(null, name);
	}

	public StoreImportTemplatePPx(PPxInterface parent, String name) {
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

	public ReportTemplatePPx reportTemplate() {
		ReportTemplatePPx ppx = new ReportTemplatePPx(this, StoreImportTemplate.P_ReportTemplate);
		return ppx;
	}

	public StoreImportPPx storeImport() {
		StoreImportPPx ppx = new StoreImportPPx(this, StoreImportTemplate.P_StoreImport);
		return ppx;
	}

	public String id() {
		return pp + "." + StoreImportTemplate.P_Id;
	}

	public String created() {
		return pp + "." + StoreImportTemplate.P_Created;
	}

	public String md5hash() {
		return pp + "." + StoreImportTemplate.P_Md5hash;
	}

	public String template() {
		return pp + "." + StoreImportTemplate.P_Template;
	}

	public String updateType() {
		return pp + "." + StoreImportTemplate.P_UpdateType;
	}

	public String updated() {
		return pp + "." + StoreImportTemplate.P_Updated;
	}

	@Override
	public String toString() {
		return pp;
	}

	public String pp() {
		return pp;
	}
}
