package com.viaoa.json.jackson;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.viaoa.util.OADateTime;

/**
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
