package com.viaoa.jackson;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.viaoa.object.OAObject;

/**
 * Used by OAJackson to register module for ObjectMapper.
 */
public class OAJacksonModule extends SimpleModule {

	public OAJacksonModule() {
		super("OAJackson", new Version(1, 0, 0, "RELEASE", "com.viaoa", "jackson"));
		addSerializer(OAObject.class, new OAJacksonSerializer());
		addDeserializer(OAObject.class, new OAJacksonDeserializer());
	}

}
