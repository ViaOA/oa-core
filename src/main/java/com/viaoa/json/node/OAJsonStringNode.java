package com.viaoa.json.node;

import com.viaoa.json.io.JsonOutputStream;

public class OAJsonStringNode extends OAJsonNode {
	String value;

	public OAJsonStringNode(String val) {
		this.value = val;
	}

	protected void toJson(final JsonOutputStream jos) {
		jos.append(String.format("\"%s\"", value));
	}

}
