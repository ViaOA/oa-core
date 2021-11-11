package com.viaoa.json.jackson;

import java.io.IOException;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.viaoa.util.OATime;

/**
 */
public class OATimeDeserializer extends JsonDeserializer<OATime> {

	@Override
	public OATime deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JacksonException {
		String s = jp.getText();
		if (s == null) {
			return null;
		}

		OATime t = new OATime(s, OATime.JsonFormat);

		return t;
	}
}
