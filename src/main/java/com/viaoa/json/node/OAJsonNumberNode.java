package com.viaoa.json.node;

import com.viaoa.json.io.JsonOutputStream;
import com.viaoa.util.OAConv;
import com.viaoa.util.OAString;

/**
 * Represents a json numeric type.
 *
 * @author vvia
 */
public class OAJsonNumberNode extends OAJsonNode {
	Number value;
	int decimalPlaces = -1;
	String format;

	public OAJsonNumberNode(Number val) {
		this.value = val;
	}

	public OAJsonNumberNode(Number val, int decimalPlaces) {
		this.value = val;
		this.decimalPlaces = decimalPlaces;
		if (decimalPlaces == 0) {
			this.format = "#";
		} else if (decimalPlaces > 0) {
			this.format = OAString.pad("#.", decimalPlaces, true, '9');
		}
	}

	protected void toJson(final JsonOutputStream jos) {
		String s = OAConv.toString(value, format);
		jos.append(s);
	}

	public Number getValue() {
		return value;
	}
}
