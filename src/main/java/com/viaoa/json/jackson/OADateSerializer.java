package com.viaoa.json.jackson;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.viaoa.util.OADate;

/**
 */
public class OADateSerializer extends JsonSerializer<OADate> {
	@Override
	public void serialize(OADate value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
		if (value == null) {
			gen.writeNull();
		} else {
			gen.writeString(value.toString(OADate.JsonFormat));
		}
	}
}
