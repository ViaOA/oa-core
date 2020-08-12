package com.viaoa.json;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import javax.json.Json;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParser.Event;

/*
	see: https://jaunt-api.com
		https://jaunt-api.com/javadocs/index.html

*/

/**
 * JSON Util methods
 *
 * @author vvia
 */
public class OAJsonUtil {

	/**
	 * Simply load flat json into a map. Only loads json key/string values.
	 */
	public Map<String, String> loadIntoFlatMap(final String json) {
		final Map<String, String> map = new HashMap();

		final JsonParser parser = Json.createParser(new StringReader(json));
		String key = null;
		while (parser.hasNext()) {
			final Event event = parser.next();
			if (event == Event.KEY_NAME) {
				key = parser.getString();
			} else if (event == Event.VALUE_STRING) {
				if (key != null) {
					String value = parser.getString();
					map.put(key, value);
				}
			}
		}
		parser.close();
		return map;
	}

}
