package com.viaoa.json.node;

import com.viaoa.json.io.JsonOutputStream;
import com.viaoa.util.OAString;

/**
 * Represents a json string type.
 *
 * @author vvia
 */
public class OAJsonStringNode extends OAJsonNode {
	String value;

	public OAJsonStringNode(String val) {
		this.value = val;
	}

	protected void toJson(final JsonOutputStream jos) {
		String s = OAString.escapeJSON(value);
		jos.append(String.format("\"%s\"", s));
	}

	public String getValue() {
		String s = OAString.unescapeJSON(value);
		return s;
	}
}
