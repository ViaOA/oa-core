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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.viaoa.util.OADateTime;

/**
 * Jackson {@link com.fasterxml.jackson.databind.JsonSerializer JsonSerializer}
 * for {@link OADateTime}.
 * <p>
 * This serializer writes {@link OADateTime} values as JSON strings using
 * {@link OADateTime#JsonFormat}. A {@code null} value is written as JSON
 * {@code null}.
 * <p>
 * It is typically registered by {@link OAJacksonModule} for OA-aware
 * Jackson {@code ObjectMapper} instances.
 */
public class OADateTimeSerializer extends JsonSerializer<OADateTime> {
	@Override
	public void serialize(OADateTime value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
		if (value == null) {
			gen.writeNull();
		} else {
			gen.writeString(value.toString(OADateTime.JsonFormat));
		}
	}
}
