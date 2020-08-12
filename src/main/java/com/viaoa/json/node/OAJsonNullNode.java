package com.viaoa.json.node;

import com.viaoa.json.io.JsonOutputStream;

/**
 * Represents a json null type.
 *
 * @author vvia
 */
public class OAJsonNullNode extends OAJsonNode {
	protected void toJson(final JsonOutputStream jos) {
		jos.append("null");
	}

}
