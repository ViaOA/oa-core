/*
 * Copyright 1999–2025 Vince Via (vvia@viaoa.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
 * Jackson {@link JsonDeserializer} that delegates JSON-to-{@link OAObject}
 * conversion to {@link OAJacksonDeserializerLoader}.
 * <p>
 * The deserializer relies on an {@link OAJson} instance stored in
 * {@link com.viaoa.object.OAThreadLocalDelegate} to drive configuration such
 * as the root object, target type, and callbacks. It converts the current
 * JSON subtree into an {@link OAObject} graph, updating or creating instances
 * as needed.
 * <p>
 * Normally this class is only used indirectly through {@link OAJacksonModule}
 * and {@link OAJson}, rather than being referenced directly by application code.
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
