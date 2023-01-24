package com.viaoa.json.jackson;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.viaoa.object.OAObject;
import com.viaoa.util.OADate;
import com.viaoa.util.OADateTime;
import com.viaoa.util.OATime;

/**
 * Used by OAJackson to register module for ObjectMapper.
 */
public class OAJacksonModule extends SimpleModule {

	public OAJacksonModule() {
		super("OAJackson", new Version(1, 0, 0, "RELEASE", "com.viaoa", "jackson"));

		addSerializer(OAObject.class, new OAJacksonSerializerPojo());
		addDeserializer(OAObject.class, new OAJacksonDeserializer());

		addSerializer(OADateTime.class, new OADateTimeSerializer());
		addDeserializer(OADateTime.class, new OADateTimeDeserializer());

		addSerializer(OADate.class, new OADateSerializer());
		addDeserializer(OADate.class, new OADateDeserializer());

		addSerializer(OATime.class, new OATimeSerializer());
		addDeserializer(OATime.class, new OATimeDeserializer());
	}

}
