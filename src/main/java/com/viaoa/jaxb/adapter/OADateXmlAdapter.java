package com.viaoa.jaxb.adapter;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import com.viaoa.util.OADate;

public class OADateXmlAdapter extends XmlAdapter<String, OADate> {

	@Override
	public String marshal(OADate dt) throws Exception {
		if (dt == null) {
			return null;
		}

		String s = dt.toString("yyyy-MM-dd");
		return s;
	}

	@Override
	public OADate unmarshal(String cal) throws Exception {
		if (cal == null) {
			return null;
		}

		OADate dt = new OADate(cal, "yyyy-MM-dd");

		return dt;
	}

}
