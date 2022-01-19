package com.viaoa.json;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.viaoa.datasource.OADataSource;
import com.viaoa.hub.Hub;
import com.viaoa.json.jackson.OAJacksonModule;
import com.viaoa.object.OACascade;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectCacheDelegate;
import com.viaoa.object.OAObjectInfo;
import com.viaoa.object.OAObjectInfoDelegate;
import com.viaoa.object.OAObjectKey;
import com.viaoa.object.OAPropertyInfo;
import com.viaoa.object.OAThreadLocalDelegate;
import com.viaoa.util.OAConv;

/**
 * OA + JSON that uses the Jackson library.
 * <p>
 * This should be used directly for read/write with OAObject & Hub.
 * <p>
 * It internally uses Jackson's ObjectMapper.
 * <p>
 * Note: this is not thread safe, create and use a separate instance.
 *
 * @author vvia
 */
public class OAJson {

	private final ArrayList<String> alPropertyPath = new ArrayList<>();
	private boolean bIncludeOwned = true;
	private boolean bIncludeAll;

	private Class readObjectClass;

	/**
	 * Used during reading, to be able to find refId that use guid for object key.
	 */
	private Map<Integer, OAObject> hmGuidObject;

	/**
	 * Used during writing, to know if an object has already been output. If so, then it will output refId of one of the following: 1: an
	 * Integer if id is numeric, 2: '-' separated string if multipart key, 3: 'guid.[guidValue]' if object does not have an assigned id.
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
	 * Flag to know if ALL references are included, default is false.
	 */
	public void setIncludeAll(boolean b) {
		bIncludeAll = b;
	}

	public boolean getIncludeAll() {
		return bIncludeAll;
	}

	/**
	 * Used internally to know the class of the root node.
	 */
	public Class<? extends OAObject> getReadObjectClass() {
		return readObjectClass;
	}

	/**
	 * Add property paths to include when writing.
	 */
	public void addPropertyPath(String propertyPath) {
		if (propertyPath != null) {
			alPropertyPath.add(propertyPath);
		}
	}

	public void addPropertyPaths(List<String> pps) {
		if (pps != null) {
			for (String pp : pps) {
				alPropertyPath.add(pp);
			}
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
	public String write(Object obj) throws JsonProcessingException {
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

	public void write(Object obj, File file) throws JsonProcessingException, IOException {
		this.cascade = null;
		String json;
		try {
			OAThreadLocalDelegate.setOAJackson(this);

			createObjectMapper().writerWithDefaultPrettyPrinter().writeValue(file, obj);

		} finally {
			OAThreadLocalDelegate.setOAJackson(null);
		}
	}

	/**
	 * Convert a JSON string to an Object graph.
	 */
	public <T extends Object> T readObject(final String json, final Class<T> clazz, final boolean bUseValidation)
			throws JsonProcessingException {
		this.readObjectClass = clazz;
		ObjectMapper om = createObjectMapper();

		hmGuidObject = null;
		getGuidMap();

		T obj;
		try {
			OAThreadLocalDelegate.setOAJackson(this);
			if (!bUseValidation) {
				OAThreadLocalDelegate.setLoading(true);
			}

			if (OAObject.class.isAssignableFrom(clazz)) {
				obj = (T) om.readValue(json, OAObject.class); // will call OAJacksonDeserializer
			} else {
				obj = (T) om.readValue(json, clazz);
			}
		} finally {
			if (!bUseValidation) {
				OAThreadLocalDelegate.setLoading(false);
			}
			OAThreadLocalDelegate.setOAJackson(null);
			readObjectClass = null;
		}

		return obj;
	}

	/**
	 * Convert a JSON file to an OAObject graph.
	 */
	public <T extends OAObject> T readObject(final File file, final Class<T> clazz, final boolean bUseValidation)
			throws JsonProcessingException, IOException {
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
			obj = (T) om.readValue(file, OAObject.class);
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

					String id = OAJson.convertObjectKeyToJsonSinglePartId(key);

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
						OAObjectKey ok = OAJson.convertNumberToObjectKey(getReadObjectClass(), node.asInt());

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
							OAObjectKey ok = OAJson.convertJsonSinglePartIdToObjectKey(getReadObjectClass(), s);

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

	public static OAObjectKey convertJsonSinglePartIdToObjectKey(final Class<? extends OAObject> clazz, final String strSinglePartId) {
		OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(clazz);

		String[] ids = strSinglePartId.split("/-");
		Object[] ids2 = new Object[ids.length];
		int i = 0;
		for (OAPropertyInfo pi : oi.getPropertyInfos()) {
			if (pi.getId()) {
				ids2[i] = OAConv.convert(pi.getClassType(), ids[i]);
				i++;
			}
		}
		OAObjectKey ok = new OAObjectKey(ids2);
		return ok;
	}

	public static OAObjectKey convertNumberToObjectKey(final Class<? extends OAObject> clazz, final int id) {
		OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(clazz);

		Object[] ids2 = new Object[1];
		for (OAPropertyInfo pi : oi.getPropertyInfos()) {
			if (pi.getId()) {
				ids2[0] = OAConv.convert(pi.getClassType(), id);
				break;
			}
		}
		OAObjectKey ok = new OAObjectKey(ids2);
		return ok;
	}

	public static String convertObjectKeyToJsonSinglePartId(OAObjectKey oaObjKey) {
		if (oaObjKey == null) {
			return null;
		}

		String ids = null;
		Object[] objs = oaObjKey.getObjectIds();
		if (objs != null) {
			boolean bHasId = false;
			for (Object obj : objs) {
				bHasId |= (obj != null);
				if (ids == null) {
					ids = "" + obj;
				} else {
					ids += "-" + OAConv.toString(obj);
				}
			}
			if (!bHasId) {
				ids = "guid." + oaObjKey.getGuid();
			}
		}
		return ids;
	}

	/**
	 * Used to serialize the arguments into a Json array. This will also include json properties for setting(/casting) if the object is
	 * different then the parameter type.
	 */
	public static String convertMethodArgumentsToJson(final Method method, final Object[] argValues,
			final List<String>[] lstIncludePropertyPathss, final int[] skipParams) throws Exception {

		final OAJson oaj = new OAJson();
		final ObjectMapper om = oaj.createObjectMapper();

		final ArrayNode arrayNode = om.createArrayNode();

		if (argValues == null) {
			return null;
		}

		final Parameter[] mps = method.getParameters();

		int i = -1;
		for (Object obj : argValues) {
			i++;

			if (skipParams != null && skipParams.length > 0) {
				boolean b = false;
				for (int p : skipParams) {
					if (p == i) {
						b = true;
						break;
					}
				}
				if (b) {
					continue;
				}
			}

			final Parameter param = mps[i];
			final Class paramClass = param.getType();
			if (obj != null && !obj.getClass().equals(paramClass) && !paramClass.isPrimitive()) {
				// need to know the correct cast
				String s = methodNextArgumentParamClass + obj.getClass().getName();
				arrayNode.add(s);
			}

			JsonNode node = om.valueToTree(obj);

			arrayNode.add(node);
		}

		return arrayNode.toPrettyString();
	}

	private static final String methodNextArgumentParamClass = "OANextParamClass:";

	/**
	 * Convert a json array to the argument values of a method.
	 */
	public static Object[] convertJsonToMethodArguments(String jsonArray, Method method) throws Exception {

		final OAJson oaj = new OAJson();
		final ObjectMapper om = oaj.createObjectMapper();

		JsonNode nodeRoot = om.readTree(jsonArray);

		ArrayNode nodeArray;

		if (nodeRoot instanceof ArrayNode) {
			nodeArray = (ArrayNode) nodeRoot;
		} else {
			nodeArray = om.createArrayNode();
			if (nodeRoot != null) {
				nodeArray.add(nodeRoot);
			}
		}

		Object[] objs = convertJsonToMethodArguments(oaj, nodeArray, method, null);
		return objs;
	}

	public static Object[] convertJsonToMethodArguments(ArrayNode nodeArray, Method method, final int[] skipParams) throws Exception {
		final OAJson oaj = new OAJson();
		final ObjectMapper om = oaj.createObjectMapper();

		Object[] objs = convertJsonToMethodArguments(oaj, nodeArray, method, null);
		return objs;
	}

	protected static Object[] convertJsonToMethodArguments(OAJson oaj, ArrayNode nodeArray, Method method, final int[] skipParams)
			throws Exception {
		if (nodeArray == null || method == null) {
			return null;
		}

		Parameter[] mps = method.getParameters();
		if (mps == null) {
			return null;
		}
		final Object[] margs = new Object[mps.length];

		final int nodeArraySize = nodeArray.size();

		int nodeArrayPos = 0;
		for (int i = 0; i < mps.length && nodeArrayPos < nodeArraySize; i++) {
			if (skipParams != null && skipParams.length > 0) {
				boolean b = false;
				for (int p : skipParams) {
					if (p == i) {
						b = true;
						break;
					}
				}
				if (b) {
					continue;
				}
			}

			final Parameter param = mps[i];
			Class paramClass = param.getType();

			JsonNode node = nodeArray.get(nodeArrayPos);

			if (node instanceof TextNode) {
				String s = ((TextNode) node).asText();
				if (s.startsWith(methodNextArgumentParamClass)) {
					s = s.substring(methodNextArgumentParamClass.length());
					paramClass = Class.forName(s);
					nodeArrayPos++;
					node = nodeArray.get(nodeArrayPos);
				}
			}

			Object objx;
			if (OAObject.class.isAssignableFrom(paramClass)) {
				objx = oaj.readObject(node.toString(), paramClass, false);
			} else {
				ObjectMapper om = oaj.createObjectMapper();
				objx = om.readValue(node.toString(), paramClass);
			}
			margs[i] = objx;
			nodeArrayPos++;
		}
		return margs;
	}

	public JsonNode readTree(String json) throws Exception {
		JsonNode node = createObjectMapper().readTree(json);
		return node;
	}
}
