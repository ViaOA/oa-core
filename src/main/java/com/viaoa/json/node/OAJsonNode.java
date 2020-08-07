package com.viaoa.json.node;

import java.io.ByteArrayOutputStream;

import com.viaoa.json.io.JsonOutputStream;

public abstract class OAJsonNode {
	//	String name;

	public String toJson() {
		return toJson(2);
	}

	public String toJson(int indentAmount) {
		String indent;
		if (indentAmount > 0) {
			indent = "";
			for (int i = 0; i < indentAmount; i++) {
				indent += " ";
			}
		} else {
			indent = null;
		}
		ByteArrayOutputStream bos = new ByteArrayOutputStream();

		JsonOutputStream jos = new JsonOutputStream(bos, indent);

		toJson(jos);
		String result = bos.toString();
		return result;
	}

	protected abstract void toJson(final JsonOutputStream jos);
}
