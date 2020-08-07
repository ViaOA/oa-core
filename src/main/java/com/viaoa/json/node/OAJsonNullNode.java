package com.viaoa.json.node;

import com.viaoa.json.io.JsonOutputStream;

public class OAJsonNullNode extends OAJsonNode {
	protected void toJson(final JsonOutputStream jos) {
		jos.append("null");
	}

}
