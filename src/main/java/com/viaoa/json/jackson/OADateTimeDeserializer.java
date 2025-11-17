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
import com.viaoa.util.OADateTime;

/**
 * Jackson {@link JsonDeserializer} for {@link OADateTime}.
 * <p>
 * This deserializer expects the JSON value to be a {@code String} formatted
 * according to {@link OADateTime#JsonFormat}. It converts the text into an
 * {@link OADateTime} instance when reading JSON into OA-aware objects or POJOs.
 * <p>
 * It is typically installed via {@link OAJacksonModule} so that OA temporal
 * types round-trip cleanly through Jackson.
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
