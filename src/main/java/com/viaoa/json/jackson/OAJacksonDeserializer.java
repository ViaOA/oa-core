package com.viaoa.json.jackson;

import java.io.IOException;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.viaoa.json.OAJson;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAThreadLocalDelegate;

/**
 * Used by OAJson to convert JSON to OAObject(s).
 * <p>
 * see: OAJacksonDeserializerLoader
 */
public class OAJacksonDeserializer extends JsonDeserializer<OAObject> {

	// https://fasterxml.github.io/jackson-databind/javadoc/2.9/com/fasterxml/jackson/databind/JsonDeserializer.html

	@Override
	public OAObject deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JacksonException {

		final OAJson oaj = OAThreadLocalDelegate.getOAJackson();
		final Class clazz = oaj.getReadObjectClass();

		OAJacksonDeserializerLoader deserializer = new OAJacksonDeserializerLoader(oaj);

		JsonNode node = jp.getCodec().readTree(jp);
		OAObject root = oaj.getRoot();

		OAObject obj = deserializer.load(node, root, clazz);
		return obj;
	}

}
