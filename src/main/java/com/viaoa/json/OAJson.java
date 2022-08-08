package com.viaoa.json;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.viaoa.datasource.OADataSource;
import com.viaoa.hub.Hub;
import com.viaoa.json.jackson.OAJacksonModule;
import com.viaoa.object.OACascade;
import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectCacheDelegate;
import com.viaoa.object.OAObjectImportMatchDelegate.ImportMatch;
import com.viaoa.object.OAObjectInfo;
import com.viaoa.object.OAObjectInfoDelegate;
import com.viaoa.object.OAObjectKey;
import com.viaoa.object.OAPropertyInfo;
import com.viaoa.object.OAThreadLocalDelegate;
import com.viaoa.util.OAConv;
import com.viaoa.util.OAString;

/**
 * JSON serialization for OA. Works dynamically with OAObject Graphs to allow property paths to be included.
 * <p>
 * This is also able to work with POJO classes that dont always have pkey properties, but instead use importMatch properties. OAJson will
 * find (or create) the correct OAObject that does have the matching value(s). <br>
 * ex: Customer.id, and Customer.custNumber, where OAObject Customer has an Id (pkey) and custNumber (int prop, but not key). The Pojo class
 * does not have to have the Id.<br>
 * also, a link could be an importMatch.
 * <p>
 * Internally uses (/depends on) Jackson's ObjectMapper.
 * <p>
 * Note: this is not thread safe, create and use a separate instance.
 *
 * @author vvia
 */
public class OAJson {
	private static volatile ObjectMapper objectMapper;

	private final ArrayList<String> alPropertyPath = new ArrayList<>();
	private boolean bIncludeOwned = true;
	private boolean bIncludeAll;

	private List<ImportMatch> alImportMatch = new ArrayList<>();

	public List<ImportMatch> getImportMatchList() {
		if (alImportMatch == null) {
			alImportMatch = new ArrayList<>();
		}
		return alImportMatch;
	}

	/**
	 * Make compatible with pojo version of oaobj, where importMatch property(ies) and link(s) are used instead of autoseq property Id
	 */
	private boolean bWriteAsPojo;

	private OAObject root;
	private Class readObjectClass;

	private Stack<OALinkInfo> stackLinkInfo;

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

	protected void reset() {
		if (alImportMatch != null) {
			alImportMatch.clear();
		}
		this.stackLinkInfo = new Stack<>();
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

	private static final Object lock = new Object();

	public static ObjectMapper createObjectMapper() {
		if (objectMapper == null) {
			synchronized (lock) {
				if (objectMapper == null) {
					ObjectMapper objectMapperx = new ObjectMapper();
					objectMapperx.registerModule(new JavaTimeModule());
					objectMapperx.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
					objectMapperx.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
					objectMapperx.setSerializationInclusion(Include.NON_NULL);
					// objectMapper.setDefaultPropertyInclusion(Include.NON_DEFAULT);

					objectMapperx.registerModule(new OAJacksonModule());
					objectMapperx.enable(SerializationFeature.INDENT_OUTPUT);
					objectMapper = objectMapperx;
				}
			}
		}
		return objectMapper;
	}

	/**
	 * Convert OAObject to a JSON string, including any owned Links, and links in propertyPaths.
	 */
	public String write(Object obj) throws JsonProcessingException {
		this.stackLinkInfo = new Stack<>();
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
	 * Convert OAObject to a JSON string, including any owned Links, and links in propertyPaths.
	 */
	public void write(Object obj, File file) throws JsonProcessingException, IOException {
		this.stackLinkInfo = new Stack<>();
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
	 * Convert OAObject to a JSON stream, including any owned Links, and links in propertyPaths.
	 */
	public void write(Object obj, final OutputStream stream) throws JsonProcessingException, IOException {
		this.stackLinkInfo = new Stack<>();
		this.cascade = null;
		String json;
		try {
			OAThreadLocalDelegate.setOAJackson(this);

			createObjectMapper().writerWithDefaultPrettyPrinter().writeValue(stream, obj);

		} finally {
			OAThreadLocalDelegate.setOAJackson(null);
		}
	}

	/**
	 * Read Object from JSON. If OAObject, then first search and find matching objects to read into.
	 */
	public <T> T readObject(final String json, final Class<T> clazz) throws JsonProcessingException {
		T t = readObject(json, clazz, false);
		return t;
	}

	private final LinkedList<Object> llStack = new LinkedList<>();

	public LinkedList<Object> getStack() {
		return llStack;
	}

	/**
	 * Root object used from call to readIntoObject.
	 */
	public OAObject getRoot() {
		return this.root;
	}

	/**
	 * Read JSON into an existing root Object.
	 */
	public void readIntoObject(final String json, OAObject root, final boolean bUseValidation) throws JsonProcessingException {
		if (root == null) {
			return;
		}
		this.root = root;
		readObject(json, root.getClass(), bUseValidation);
	}

	/**
	 * Convert a JSON string to an Object graph. If OAObject, then first search and find matching objects to read into.
	 *
	 * @param bUseValidation if false (default) then setLoading(true) will be used before loading.
	 */
	public <T> T readObject(final String json, final Class<T> clazz, final boolean bUseValidation)
			throws JsonProcessingException {
		reset();
		this.readObjectClass = clazz;
		ObjectMapper om = createObjectMapper();

		hmGuidObject = null;
		getGuidMap();

		T obj;
		try {
			OAThreadLocalDelegate.setOAJackson(this);
			if (!bUseValidation) {
				OAThreadLocalDelegate.setLoading(true);
			} else {
				OAThreadLocalDelegate.setSyncThread(true);
			}

			Class c = clazz;
			if (OAObject.class.isAssignableFrom(clazz)) {
				c = OAObject.class;
			}
			JavaType jt = om.getTypeFactory().constructType(c);

			obj = (T) om.readValue(json, jt);

		} finally {
			if (!bUseValidation) {
				OAThreadLocalDelegate.setLoading(false);
			} else {
				OAThreadLocalDelegate.setSyncThread(false);
			}
			OAThreadLocalDelegate.setOAJackson(null);
			readObjectClass = null;
		}

		return obj;
	}

	protected void afterReadJson() {
		//qqqqqqqqqqqqqqqqqqq

	}

	/**
	 * Read JSON from stream into Object.
	 */
	public <T> T readObject(final InputStream stream, final Class<T> clazz, final boolean bUseValidation)
			throws JsonProcessingException, IOException {
		reset();
		this.readObjectClass = clazz;
		ObjectMapper om = createObjectMapper();
		this.stackLinkInfo = new Stack<>();

		hmGuidObject = null;
		Map<Integer, OAObject> hmGuidMap = getGuidMap();

		T obj;
		try {
			OAThreadLocalDelegate.setOAJackson(this);
			if (!bUseValidation) {
				OAThreadLocalDelegate.setLoading(true);
			}

			Class c = clazz;
			if (OAObject.class.isAssignableFrom(clazz)) {
				c = OAObject.class;
			}
			JavaType jt = om.getTypeFactory().constructType(c);

			obj = (T) om.readValue(stream, jt);

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
	public <T> T readObject(final File file, final Class<T> clazz, final boolean bUseValidation)
			throws JsonProcessingException, IOException {
		reset();
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

			Class c = clazz;
			if (OAObject.class.isAssignableFrom(clazz)) {
				c = OAObject.class;
			}
			JavaType jt = om.getTypeFactory().constructType(c);

			obj = (T) om.readValue(file, jt);

			//qqqqqqqqqqqqqqqqqvv

		} finally {
			if (!bUseValidation) {
				OAThreadLocalDelegate.setLoading(false);
			}
			OAThreadLocalDelegate.setOAJackson(null);
			readObjectClass = null;
		}

		return obj;
	}

	public <K, V> Map<K, V> readMap(final String json, final Class<K> clazzKey, final Class<V> clazzValue,
			final boolean bUseValidation)
			throws JsonProcessingException, IOException {
		reset();
		this.readObjectClass = clazzValue;
		ObjectMapper om = createObjectMapper();
		hmGuidObject = null;

		Map<K, V> map;
		try {
			OAThreadLocalDelegate.setOAJackson(this);
			if (!bUseValidation) {
				OAThreadLocalDelegate.setLoading(true);
			}

			Class c = clazzValue;
			if (OAObject.class.isAssignableFrom(clazzValue)) {
				c = OAObject.class;
			}

			MapType mt = om.getTypeFactory().constructMapType(Map.class, clazzKey, c);

			map = (Map<K, V>) om.readValue(json, mt);

			//qqqqqqqqqqqqqqqqqvv

		} finally {
			if (!bUseValidation) {
				OAThreadLocalDelegate.setLoading(false);
			}
			OAThreadLocalDelegate.setOAJackson(null);
			readObjectClass = null;
		}

		return map;
	}

	public <T> List<T> readList(final String json, final Class<T> clazz, final boolean bUseValidation)
			throws JsonProcessingException, IOException {
		reset();
		this.readObjectClass = clazz;
		ObjectMapper om = createObjectMapper();
		hmGuidObject = null;

		List<T> list;
		try {
			OAThreadLocalDelegate.setOAJackson(this);
			if (!bUseValidation) {
				OAThreadLocalDelegate.setLoading(true);
			}

			Class c = clazz;
			if (OAObject.class.isAssignableFrom(clazz)) {
				c = OAObject.class;
			}
			CollectionType ct = om.getTypeFactory().constructCollectionType(List.class, c);

			list = (List<T>) om.readValue(json, ct);

		} finally {
			if (!bUseValidation) {
				OAThreadLocalDelegate.setLoading(false);
			}
			OAThreadLocalDelegate.setOAJackson(null);
			readObjectClass = null;
		}

		return list;
	}

	public <T> List<T> readList(final File file, final Class<T> clazz, final boolean bUseValidation)
			throws JsonProcessingException, IOException {
		reset();
		this.readObjectClass = clazz;
		ObjectMapper om = createObjectMapper();
		hmGuidObject = null;

		List<T> list;
		try {
			OAThreadLocalDelegate.setOAJackson(this);
			if (!bUseValidation) {
				OAThreadLocalDelegate.setLoading(true);
			}

			Class c = clazz;
			if (OAObject.class.isAssignableFrom(clazz)) {
				c = OAObject.class;
			}
			CollectionType ct = om.getTypeFactory().constructCollectionType(List.class, c);

			list = (List<T>) om.readValue(file, ct);
		} finally {
			if (!bUseValidation) {
				OAThreadLocalDelegate.setLoading(false);
			}
			OAThreadLocalDelegate.setOAJackson(null);
			readObjectClass = null;
		}

		return list;
	}

	public <T> List<T> readList(final InputStream stream, final Class<T> clazz, final boolean bUseValidation)
			throws JsonProcessingException, IOException {
		reset();
		this.readObjectClass = clazz;
		ObjectMapper om = createObjectMapper();
		hmGuidObject = null;

		List<T> list;
		try {
			OAThreadLocalDelegate.setOAJackson(this);
			if (!bUseValidation) {
				OAThreadLocalDelegate.setLoading(true);
			}

			Class c = clazz;
			if (OAObject.class.isAssignableFrom(clazz)) {
				c = OAObject.class;
			}
			CollectionType ct = om.getTypeFactory().constructCollectionType(List.class, c);

			list = (List<T>) om.readValue(stream, ct);

			//qqqqqqqqqqqqqqqqqvv

		} finally {
			if (!bUseValidation) {
				OAThreadLocalDelegate.setLoading(false);
			}
			OAThreadLocalDelegate.setOAJackson(null);
			readObjectClass = null;
		}

		return list;
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

	public void write(final Hub<? extends OAObject> hub, File file) throws JsonProcessingException, IOException {
		this.stackLinkInfo = new Stack<>();
		this.cascade = null;
		try {
			OAThreadLocalDelegate.setOAJackson(this);

			createObjectMapper().writerWithDefaultPrettyPrinter().writeValue(file, hub);

		} finally {
			OAThreadLocalDelegate.setOAJackson(null);
		}
	}

	public String write(final Hub<? extends OAObject> hub) throws JsonProcessingException {
		this.stackLinkInfo = new Stack<>();
		this.cascade = null;
		String json;
		try {
			OAThreadLocalDelegate.setOAJackson(this);
			final ObjectMapper objectMapper = createObjectMapper();
			json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(hub);
		} finally {
			OAThreadLocalDelegate.setOAJackson(null);
		}
		return json;
	}

	public <T extends OAObject> void readIntoHub(final File file, final Hub<T> hub, final boolean bUseValidation) throws Exception {
		ObjectMapper om = createObjectMapper();
		final JsonNode nodeRoot = om.readTree(file);
		readIntoHub(om, nodeRoot, hub, bUseValidation);
	}

	public <T extends OAObject> void readIntoHub(final String json, final Hub<T> hub, final boolean bUseValidation) throws Exception {
		ObjectMapper om = createObjectMapper();
		final JsonNode nodeRoot = om.readTree(json);
		readIntoHub(om, nodeRoot, hub, bUseValidation);
	}

	public <T extends OAObject> void readIntoHub(final ObjectMapper om, final JsonNode nodeRoot, final Hub<T> hub,
			final boolean bUseValidation) throws Exception {

		reset();
		this.readObjectClass = hub.getObjectClass();

		hmGuidObject = null;
		Map<Integer, OAObject> hmGuidMap = getGuidMap();

		try {
			OAThreadLocalDelegate.setOAJackson(this);

			if (nodeRoot.isArray()) {
				ArrayNode nodeArray = (ArrayNode) nodeRoot;
				int x = nodeArray.size();
				for (int i = 0; i < x; i++) {
					JsonNode node = nodeArray.get(i);
					if (node.isObject()) {
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
				// hub.add(readObject(json, hub.getObjectClass(), bUseValidation));
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
		oaj.stackLinkInfo = new Stack<>();

		try {
			OAThreadLocalDelegate.setOAJackson(oaj);

			return _convertMethodArgumentsToJson(oaj, method, argValues, lstIncludePropertyPathss, skipParams);
		} finally {
			OAThreadLocalDelegate.setOAJackson(null);
		}
	}

	protected static String _convertMethodArgumentsToJson(final OAJson oaj, final Method method, final Object[] argValues,
			final List<String>[] lstIncludePropertyPathss, final int[] skipParams) throws Exception {

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

				//qqqqqqqqqqqqqqqqqvv

			}
			margs[i] = objx;
			nodeArrayPos++;
		}
		return margs;
	}

	public JsonNode readTree(String json) throws Exception {
		reset();
		JsonNode node = createObjectMapper().readTree(json);
		return node;
	}

	// todo:  under constructions[]

	public JsonNode getNode(JsonNode parentNode, String propertyPath) {
		String[] ss = propertyPath.split("\\.");
		for (String prop : ss) {
			String s = OAString.field(prop, "[", 2);
			prop = OAString.field(prop, "[", 1);

			JsonNode jn = parentNode.get(prop);
			parentNode = jn;
		}
		return parentNode;
	}

	/**
	 * Propertypath that is currently being read/written.
	 */
	public String getCurrentPropertyPath() {
		String pp = "";
		if (stackLinkInfo != null) {
			OALinkInfo liPrev = null;
			for (OALinkInfo li : stackLinkInfo) {
				if (li == liPrev) {
					continue; // recursive
				}
				if (pp.length() > 0) {
					pp += ".";
				}
				pp += li.getLowerName();
				liPrev = li;
			}
		}
		return pp;
	}

	public Stack<OALinkInfo> getStackLinkInfo() {
		return this.stackLinkInfo;
	}

	// called during read/write
	public String getPropertyNameCallback(Object obj, String defaultName) {
		return defaultName;
	}

	// called during read/write
	public Object getPropertyValueCallback(Object obj, String propertyName, Object defaultValue) {
		return defaultValue;
	}

	// called during read/write
	public boolean getUsePropertyCallback(Object obj, String propertyName) {
		return true;
	}

	public void beforeReadCallback(JsonNode node) {
	}

	public void afterReadCallback(JsonNode node, Object objNew) {
	}

	public void setWriteAsPojo(boolean b) {
		this.bWriteAsPojo = b;
	}

	public boolean getWriteAsPojo() {
		return this.bWriteAsPojo;
	}
}
