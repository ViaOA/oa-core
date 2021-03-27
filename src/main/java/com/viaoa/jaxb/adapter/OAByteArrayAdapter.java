package com.viaoa.jaxb.adapter;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import com.viaoa.util.Base64;

/*

	@XmlJavaTypeAdapter(OAByteArrayAdapter.class)

*/

public class OAByteArrayAdapter extends XmlAdapter<String, byte[]> {
	@Override
	public String marshal(byte[] bs) throws Exception {
		char[] cs = Base64.encode(bs);
		String s = new String(cs);
		return s;
	}

	@Override
	public byte[] unmarshal(String val) throws Exception {
		byte[] bs = Base64.decode(val.toCharArray());
		return bs;
	}

}
