package com.viaoa.json.node;

import com.viaoa.json.io.JsonOutputStream;

public class OAJsonBooleanNode extends OAJsonNode {
	Boolean value;

	public OAJsonBooleanNode(Boolean val) {
		this.value = val;
	}

	protected void toJson(final JsonOutputStream jos) {
		jos.append(value.toString());
	}

}
