package com.auto.reportercorp.util;

import java.io.IOException;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * This allows a Java (String) property to load a JSON object as a String.<br>
 * This should be combined with @JsonRawValue, so that writing the Java String property will be a JSON object and not a JSON escaped String
 * (ex: with \").
 */
public class EmbeddedJsonStringDeserializer extends JsonDeserializer<String> {
	@Override
	public String deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JacksonException {
		Object objx = jp.readValueAs(JsonNode.class);

		String s = objx == null ? null : objx.toString();
		return s;
	}
}
