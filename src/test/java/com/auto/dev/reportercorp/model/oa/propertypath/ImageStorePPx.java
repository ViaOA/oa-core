package com.auto.dev.reportercorp.model.oa.propertypath;

import java.io.Serializable;

import com.auto.dev.reportercorp.model.oa.ImageStore;

public class ImageStorePPx implements PPxInterface, Serializable {
	private static final long serialVersionUID = 1L;
	public final String pp; // propertyPath

	public ImageStorePPx(String name) {
		this(null, name);
	}

	public ImageStorePPx(PPxInterface parent, String name) {
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

	public String id() {
		return pp + "." + ImageStore.P_Id;
	}

	public String created() {
		return pp + "." + ImageStore.P_Created;
	}

	public String bytes() {
		return pp + "." + ImageStore.P_Bytes;
	}

	public String origFileName() {
		return pp + "." + ImageStore.P_OrigFileName;
	}

	@Override
	public String toString() {
		return pp;
	}

	public String pp() {
		return pp;
	}
}
