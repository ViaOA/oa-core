package com.viaoa.json.jackson;

import java.io.IOException;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.viaoa.util.OADate;

/**
 */
public class OADateDeserializer extends JsonDeserializer<OADate> {

	@Override
	public OADate deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JacksonException {
		String s = jp.getText();
		if (s == null) {
			return null;
		}

		OADate d = new OADate(s, OADate.JsonFormat);

		return d;
	}
}
