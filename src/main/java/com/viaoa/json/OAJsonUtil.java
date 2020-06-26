package com.viaoa.json;

import java.io.StringReader;

import javax.json.Json;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParser.Event;

public class OAJsonUtil {

	/*
	 * see:  20200127 OAJaxb.java <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
	 * 
	 */
	public void test(String sz) {
		final JsonParser parser = Json.createParser(new StringReader(sz));
		String key = null;
		String value = null;
		while (parser.hasNext()) {
			final Event event = parser.next();
			if (event == Event.KEY_NAME) {
				key = parser.getString();
			} else if (event == Event.VALUE_STRING) {
				if ("http://wso2.org/claims/enduser".equalsIgnoreCase(key)) {
					value = parser.getString();
					break;
				}
			}
		}
		parser.close();
	}
}
