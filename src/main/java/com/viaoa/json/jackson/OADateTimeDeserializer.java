package com.viaoa.json.jackson;

import java.io.IOException;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.viaoa.util.OADateTime;

/**
 */
public class OADateTimeDeserializer extends JsonDeserializer<OADateTime> {

	@Override
	public OADateTime deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JacksonException {
		String s = jp.getText();
		if (s == null) {
			return null;
		}

		OADateTime dt = new OADateTime(s, OADateTime.JsonFormat);

		return dt;
	}
}
