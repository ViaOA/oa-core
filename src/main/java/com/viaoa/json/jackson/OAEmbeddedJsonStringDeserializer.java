package com.viaoa.json.jackson;

import java.io.IOException;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Load a Json Object into a Java String property.
 * <p>
 * This allows a Json object or array to be stored in a Java String property, without being escaped.
 * <p>
 * Example: A department that has employees array, and we want to load into Java Dept, but have the Dept.employees just loaded into a String
 * as json.
 * <p>
 * NOTE: this is not needed in OAObjects, only POJOs.<br>
 * OAJacksonSerializer and Deserializer have this functionality built in, and dont require these annotations.
 * <p>
 * Note: for Pojos, this requires using @JsonRawValue for the serialization so that it wont escape the String during serialization.
 */
public class OAEmbeddedJsonStringDeserializer extends JsonDeserializer<String> {

	@Override
	public String deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JacksonException {
		Object objx = jp.readValueAs(JsonNode.class);
		String s = objx == null ? null : objx.toString();
		return s;
	}
}
