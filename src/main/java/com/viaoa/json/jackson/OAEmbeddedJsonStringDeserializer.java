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
