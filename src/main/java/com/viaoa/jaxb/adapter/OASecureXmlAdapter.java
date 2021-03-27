package com.viaoa.jaxb.adapter;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import com.viaoa.util.OAEncryption;

public class OASecureXmlAdapter extends XmlAdapter<String, String> {
	@Override
	public String marshal(String val) throws Exception {
		if (val == null) {
			return null;
		}
		String s = OAEncryption.encrypt(val);
		// String s = "********";        
		return s;
	}

	@Override
	public String unmarshal(String val) throws Exception {
		String s = OAEncryption.decrypt(val);
		return s;
	}

}
