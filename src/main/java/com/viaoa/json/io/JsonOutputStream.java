package com.viaoa.json.io;

import java.io.OutputStream;

import com.viaoa.util.OAString;

public class JsonOutputStream {
	int indent;
	String indentSpaces;
	OutputStream os;

	public JsonOutputStream(OutputStream os, String indentSpaces) {
		this.os = os;
		this.indentSpaces = indentSpaces;
	}

	public void append(String txt) {
		try {
			os.write(txt.getBytes());
		} catch (Exception e) {
			throw new RuntimeException("", e);
		}
	}

	public boolean endLine() {
		if (OAString.isEmpty(indentSpaces)) {
			return false;
		}
		try {
			os.write(OAString.NL.getBytes());
		} catch (Exception e) {
			throw new RuntimeException("", e);
		}
		return true;
	}

	public boolean addSpace() {
		if (OAString.isEmpty(indentSpaces)) {
			return false;
		}
		try {
			os.write(" ".getBytes());
		} catch (Exception e) {
			throw new RuntimeException("", e);
		}
		return true;
	}

	public boolean startLine() {
		if (OAString.isEmpty(indentSpaces)) {
			return false;
		}
		try {
			for (int i = 0; i < indent; i++) {
				os.write(indentSpaces.getBytes());
			}
		} catch (Exception e) {
			throw new RuntimeException("", e);
		}
		return true;
	}

	public void addIndent() {
		indent++;
	}

	public void subtractIndent() {
		indent--;
	}

}
