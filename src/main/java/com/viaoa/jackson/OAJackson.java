package com.viaoa.jackson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.viaoa.datasource.OADataSource;
import com.viaoa.hub.Hub;
import com.viaoa.object.OACascade;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectCacheDelegate;
import com.viaoa.object.OAObjectKey;
import com.viaoa.object.OAThreadLocalDelegate;
import com.viaoa.util.OAConv;

/**
 * OA + JSON that uses the Jackson library.
 * <p>
 * This should be used directory for read/write with OAObject & Hub.
 * <p>
 * It will internally use ObjectMapper.
 * <p>
 * Note: this is not thread safe, create and use a separate instance.
 *
 * @author vvia
 */
public class OAJackson {

	private final ArrayList<String> alPropertyPath = new ArrayList<>();
	private boolean bIncludeOwned = true;

	private Class readObjectClass;

	/**
	 * Used during reading, to be able to find refId that use guid for object key.
	 */
	private Map<Integer, OAObject> hmGuidObject;

	/**
	 * Used during writing, to know if an object has already been output. If so, then it will output refId as each a String that is a
	 * singlePart Id, a Integer if single part numeric id, or guid if object does not have an assigned id.
	 */
	private OACascade cascade;

	/**
	 * Flag to know if owned references are included, default is true.
	 */
	public void setIncludeOwned(boolean b) {
		bIncludeOwned = b;
	}

	public boolean getIncludeOwned() {
		return bIncludeOwned;
	}

	/**
	 * Used internally to know the class of the root node.
	 */
	protected Class<? extends OAObject> getReadObjectClass() {
		return readObjectClass;
	}

	/**
	 * Add property paths to include when writing.
	 *
	 * @param pp
	 */
	public void addPropertyPath(String pp) {
		if (pp != null) {
			alPropertyPath.add(pp);
		}
	}

	public ArrayList<String> getPropertyPaths() {
		return alPropertyPath;
	}

	public void clearPropertyPaths() {
		alPropertyPath.clear();
	}

	public static ObjectMapper createObjectMapper() {
		ObjectMapper objectMapper;
		objectMapper = new ObjectMapper();
		objectMapper.registerModule(new JavaTimeModule());
		objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		objectMapper.setSerializationInclusion(Include.NON_NULL);
		// objectMapper.setDefaultPropertyInclusion(Include.NON_DEFAULT);

		objectMapper.registerModule(new OAJacksonModule());
		objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

		return objectMapper;
	}

	/**
	 * Convert OAObject to a JSON string.
	 */
	public String write(OAObject obj) throws JsonProcessingException {
		this.cascade = null;
		String json;
		try {
			OAThreadLocalDelegate.setOAJackson(this);

			json = createObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(obj);

		} finally {
			OAThreadLocalDelegate.setOAJackson(null);
		}

		return json;
	}

	/**
	 * Convert a JSON string to an OAObject graph.
	 */
	public <T extends OAObject> T readObject(final String json, final Class<T> clazz, final boolean bUseValidation)
			throws JsonProcessingException {
		this.readObjectClass = clazz;
		ObjectMapper om = createObjectMapper();

		hmGuidObject = null;
		Map<Integer, OAObject> hmGuidMap = getGuidMap();

		T obj;
		try {
			OAThreadLocalDelegate.setOAJackson(this);
			if (!bUseValidation) {
				OAThreadLocalDelegate.setLoading(true);
			}
			obj = (T) om.readValue(json, OAObject.class);
		} finally {
			if (!bUseValidation) {
				OAThreadLocalDelegate.setLoading(false);
			}
			OAThreadLocalDelegate.setOAJackson(null);
			readObjectClass = null;
		}

		return obj;
	}

	public OACascade getCascade() {
		if (cascade == null) {
			cascade = new OACascade();
		}
		return cascade;
	}

	public Map<Integer, OAObject> getGuidMap() {
		if (hmGuidObject == null) {
			hmGuidObject = new HashMap();
		}
		return hmGuidObject;
	}

	public String write(final Hub<? extends OAObject> hub) throws JsonProcessingException {
		this.cascade = null;
		final OACascade cascade = getCascade();

		final ObjectMapper objectMapper = createObjectMapper();

		ArrayNode nodeArray = objectMapper.createArrayNode();

		try {
			OAThreadLocalDelegate.setOAJackson(this);

			for (final OAObject oaObj : hub) {
				if (cascade.wasCascaded(oaObj, true)) {
					// write single objKey or guid to arrayNode
					OAObjectKey key = oaObj.getObjectKey();

					String id = OAJacksonSerializer.convertObjectKeyToJsonSinglePartId(key);

					if (id.indexOf('-') >= 0 || id.indexOf("guid.") == 0) {
						nodeArray.add(id);
					} else {
						nodeArray.add(OAConv.toLong(id));
					}

				} else {
					JsonNode node = objectMapper.valueToTree(oaObj);
					nodeArray.add(node);
				}
			}
		} finally {
			OAThreadLocalDelegate.setOAJackson(null);
		}

		String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(nodeArray);

		return json;
	}

	public <T extends OAObject> void readIntoHub(final String json, final Hub<T> hub, final boolean bUseValidation) throws Exception {

		this.readObjectClass = hub.getObjectClass();
		ObjectMapper om = createObjectMapper();

		hmGuidObject = null;
		Map<Integer, OAObject> hmGuidMap = getGuidMap();

		try {
			OAThreadLocalDelegate.setOAJackson(this);

			final JsonNode nodeRoot = om.readTree(json);
			if (nodeRoot.isArray()) {
				ArrayNode nodeArray = (ArrayNode) nodeRoot;
				int x = nodeArray.size();
				for (int i = 0; i < x; i++) {
					JsonNode node = nodeArray.get(i);
					if (node.isObject()) {
						// qqqqq
						T objx = om.readerFor(OAObject.class).readValue(node); // will use OAJacksondeserializer
						hub.add(objx);
					} else if (node.isNumber()) {
						// key
						OAObjectKey ok = OAJacksonDeserializer.convertNumberToObjectKey(getReadObjectClass(), node.asInt());

						OAObject objNew = (OAObject) OAObjectCacheDelegate.get(getReadObjectClass(), ok);
						if (objNew != null) {
							hub.add((T) objNew);
						} else {
							objNew = (OAObject) OADataSource.getObject(getReadObjectClass(), ok);
							hub.add((T) objNew);
						}
					} else {
						String s = node.textValue();
						if (s.indexOf("guid.") == 0) {
							s = s.substring(5);
							int guid = Integer.parseInt(s);
							hub.add((T) getGuidMap().get(guid));
						} else {
							// convert multipart key to OAObjectKey
							OAObjectKey ok = OAJacksonDeserializer.convertJsonSinglePartIdToObjectKey(getReadObjectClass(), s);

							OAObject objNew = (OAObject) OAObjectCacheDelegate.get(getReadObjectClass(), ok);
							if (objNew != null) {
								hub.add((T) objNew);
							} else {
								objNew = (OAObject) OADataSource.getObject(getReadObjectClass(), ok);
								hub.add((T) objNew);
							}
						}
					}
				}
			} else {
				hub.add(readObject(json, hub.getObjectClass(), bUseValidation));
			}

			if (!bUseValidation) {
				OAThreadLocalDelegate.setLoading(true);
			}

		} finally {
			if (!bUseValidation) {
				OAThreadLocalDelegate.setLoading(false);
			}
			OAThreadLocalDelegate.setOAJackson(null);
			readObjectClass = null;
		}
	}

}
