package com.viaoa.xml;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.*;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.viaoa.json.OAJson;
import com.viaoa.json.jackson.OAJacksonModule;

/**
 * XML version of OAJson.
 * 
 * @author vvia
 * @since 20230917
 */
public class OAXml extends OAJson {
	
	private static ObjectMapper xmlObjectMapper;
	
	public ObjectMapper getXmlObjectMapper() {
		if (xmlObjectMapper == null) {
			XmlMapper objectMapperx = new XmlMapper();
			objectMapperx.registerModule(new JavaTimeModule());
			objectMapperx.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
			objectMapperx.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
			objectMapperx.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	
			objectMapperx.setDefaultPropertyInclusion(Include.ALWAYS);
			// objectMapperx.setSerializationInclusion(Include.NON_NULL);
	
			objectMapperx.registerModule(new OAJacksonModule());
			objectMapperx.enable(SerializationFeature.INDENT_OUTPUT);
			xmlObjectMapper = objectMapperx;
		}
		return xmlObjectMapper;
	}
	
	public ObjectMapper getObjectMapper() {
		return getXmlObjectMapper();
	}

	public String toXml(Object obj) throws JsonProcessingException {
		return write(obj);
	}
	
}
