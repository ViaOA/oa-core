package com.viaoa.json.node;

import com.viaoa.json.io.JsonOutputStream;

public class OAJsonNumberNode extends OAJsonNode {
	Number value;

	public OAJsonNumberNode(Number val) {
		this.value = val;
	}

	protected void toJson(final JsonOutputStream jos) {
		String s = value.toString();
		jos.append(s);
	}

}
