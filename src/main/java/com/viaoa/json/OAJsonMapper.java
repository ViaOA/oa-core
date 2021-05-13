package com.viaoa.json;

import java.io.StringReader;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParser.Event;

import com.viaoa.hub.Hub;
import com.viaoa.jaxb.OAJaxb;
import com.viaoa.json.node.OAJsonArrayNode;
import com.viaoa.json.node.OAJsonBooleanNode;
import com.viaoa.json.node.OAJsonNode;
import com.viaoa.json.node.OAJsonNullNode;
import com.viaoa.json.node.OAJsonNumberNode;
import com.viaoa.json.node.OAJsonObjectNode;
import com.viaoa.json.node.OAJsonStringNode;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectInfo;
import com.viaoa.object.OAObjectInfoDelegate;
import com.viaoa.object.OAObjectKey;
import com.viaoa.util.OAConv;
import com.viaoa.util.OAConverter;
import com.viaoa.util.OADate;
import com.viaoa.util.OADateTime;
import com.viaoa.util.OAString;
import com.viaoa.util.OATime;

/*
	see: https://jaunt-api.com
		https://jaunt-api.com/javadocs/index.html
*/

/**
 * JSON Mapper for converting between json and objects.
 * <p>
 *
 * @author vvia
 */
public class OAJsonMapper {

	/**
	 * load flat json into a map. Only loads json key/string values.
	 */
	public Map<String, String> loadIntoFlatMap(final String json) {
		final Map<String, String> map = new HashMap();

		final JsonParser parser = Json.createParser(new StringReader(json));
		String key = null;
		while (parser.hasNext()) {
			final Event event = parser.next();
			if (event == Event.KEY_NAME) {
				key = parser.getString();
			} else if (event == Event.VALUE_STRING) {
				if (key != null) {
					String value = parser.getString();
					map.put(key, value);
					key = null;
				}
			} else if (event == Event.VALUE_NULL) {
				if (key != null) {
					map.put(key, "null");
					key = null;
				}
			} else if (event == Event.VALUE_NUMBER) {
				if (key != null) {
					String value = parser.getBigDecimal().toString();
					map.put(key, value);
					key = null;
				}
			} else if (event == Event.VALUE_TRUE) {
				if (key != null) {
					map.put(key, "true");
					key = null;
				}
			} else if (event == Event.VALUE_FALSE) {
				if (key != null) {
					map.put(key, "false");
					key = null;
				}
			}
		}
		parser.close();
		return map;
	}

	/**
	 * Convert a value to a json string. Includes arrays.
	 * <P>
	 */
	public static String convertToJsonValue(Object value) {
		String result;
		if (value != null && value.getClass().isArray()) {
			result = "[";
			int x = Array.getLength(value);
			for (int i = 0; i < x; i++) {
				if (i > 0) {
					result += ", ";
				}
				Object obj = Array.get(value, i);
				String s = _convertToJsonValue(obj);
				result += s;
			}

		} else {
			result = _convertToJsonValue(value);
		}
		return result;
	}

	protected static String _convertToJsonValue(Object value) {
		if (value == null) {
			return "null";
		}

		if (value != null && !(value instanceof String) && OAConverter.getConverter(value.getClass()) == null) {
			return null;
		}

		String result;
		if (value instanceof String) {
			result = OAString.convert((String) value, "\"", "\\\"");
		} else if (value instanceof Number) {
			result = OAConv.toString(value);
		} else if (value instanceof OADate) {
			result = ("\"" + ((OADate) value).toString("yyyy-MM-dd") + "\"");
			//qqqqqqqqqqq add LocalDate qqqqqqqqqqqq etc
		} else if (value instanceof OATime) {
			result = ("\"" + ((OATime) value).toString("HH:mm:ss") + "\"");
		} else if (value instanceof OADateTime) {
			result = ("\"" + ((OADateTime) value).toString("yyyy-MM-dd'T'HH:mm:ss") + "\""); // "2020-12-26T19:21:09"
		} else {
			value = OAConv.toString(value);
			value = OAString.convert((String) value, "\"", "\\\"");
			result = ("\"");
			result += ((String) value);
			result += ("\"");
		}
		return result;
	}

	public static <T> T convertJsonToObject(final String json, Class<T> toClass) throws Exception {
		T obj = convertJsonToObject(json, toClass, null);
		return obj;
	}

	public static <T> T convertJsonToObject(final String json, Class<T> toClass, final Class classInCollection) throws Exception {
		if (OAString.isEmpty(json)) {
			return null;
		}
		OAJson oaj = new OAJson();
		OAJsonNode node = oaj.load(json);
		T obj = convertJsonToObject(node, toClass, classInCollection);
		return obj;
	}

	public static <T> T convertJsonToObject(final OAJsonNode fromNode, Class<T> toClass, final Class classInCollection) throws Exception {
		if (fromNode == null || toClass == null) {
			return null;
		}

		if (fromNode instanceof OAJsonNullNode) {
			return null;
		}

		final Class origToClass = toClass;

		final boolean bIsArray = origToClass.isArray();
		final boolean bIsHub = Hub.class.isAssignableFrom(origToClass);
		final boolean bIsList = !bIsHub && List.class.isAssignableFrom(origToClass);

		if ((bIsList || bIsHub) && classInCollection == null) {
			throw new Exception("listClass can not be null");
		}

		toClass = bIsArray ? origToClass.getComponentType() : (bIsList ? classInCollection : origToClass);

		if (fromNode.getClass().isAssignableFrom(origToClass)) {
			return (T) fromNode;
		}

		Object objResult = null;
		if (toClass.equals(String.class)) {
			if (fromNode instanceof OAJsonStringNode) {
				objResult = ((OAJsonStringNode) fromNode).getValue();
			} else {
				objResult = fromNode.toJson();
			}
			return (T) objResult;
		}

		if (OAObject.class.isAssignableFrom(toClass)) {
			OAJaxb jaxb = new OAJaxb(toClass);
			String sJson = fromNode.toJson();
			if (bIsArray) {
				objResult = jaxb.convertArrayFromJSON(sJson);
			} else if (bIsList) {
				objResult = jaxb.convertListFromJSON(sJson);
			} else if (bIsHub) {
				objResult = jaxb.convertHubFromJSON(sJson);
			} else {
				objResult = jaxb.convertFromJSON(sJson);
			}
			return (T) objResult;
		}

		if (toClass.equals(void.class) || toClass.equals(Void.class)) {
			objResult = null;
			return (T) objResult;
		}

		if (bIsArray || bIsList || bIsHub) {
			if (!(fromNode instanceof OAJsonArrayNode)) {
				throw new Exception("JSON node expected to be JsonArrayNode for collection, toClass=" + origToClass);
			}

			ArrayList al = new ArrayList();
			for (OAJsonNode node : ((OAJsonArrayNode) fromNode).getArray()) {
				Object obj = node;

				if (obj == null || obj instanceof OAJsonNullNode) {
					continue;
				} else if (obj instanceof OAJsonStringNode) {
					obj = ((OAJsonStringNode) obj).getValue();
					obj = OAConv.convert(toClass, obj);
				} else if (obj instanceof OAJsonBooleanNode) {
					obj = ((OAJsonBooleanNode) obj).getValue();
					obj = OAConv.convert(toClass, obj);
				} else if (obj instanceof OAJsonNumberNode) {
					obj = ((OAJsonNumberNode) obj).getValue();
					obj = OAConv.convert(toClass, obj);
				} else {
					// objects
					OAJaxb jaxb = new OAJaxb(toClass);
					obj = jaxb.convertFromJSON(node.toJson());
				}
				al.add(obj);
			}

			if (bIsArray) {
				objResult = Array.newInstance(toClass, al.size());
				int i = 0;
				for (Object objx : al) {
					Array.set(objResult, i++, objx);
				}
			} else if (bIsHub) {
				Hub hub = new Hub(toClass);
				hub.add(al);
				objResult = hub;
			} else {
				objResult = al;
			}
			return (T) objResult;
		}

		if (Map.class.isAssignableFrom(origToClass)) {
			if (fromNode instanceof OAJsonObjectNode) {
				Map map = new HashMap();
				OAJsonObjectNode nodex = (OAJsonObjectNode) fromNode;
				for (String sx : nodex.getChildrenPropertyNames()) {
					OAJsonNode child = nodex.getChildNode(sx);
					Object val = convertJsonToObject(child, classInCollection, null);
					map.put(sx, val);
				}
				objResult = map;
				return (T) objResult;
			}
		}

		if (fromNode instanceof OAJsonStringNode) {
			objResult = ((OAJsonStringNode) fromNode).getValue();
			objResult = OAConv.convert(toClass, objResult);
		} else if (fromNode instanceof OAJsonBooleanNode) {
			objResult = ((OAJsonBooleanNode) fromNode).getValue();
			objResult = OAConv.convert(toClass, objResult);
		} else if (fromNode instanceof OAJsonNumberNode) {
			objResult = ((OAJsonNumberNode) fromNode).getValue();
			objResult = OAConv.convert(toClass, objResult);
		} else if (fromNode == null || fromNode instanceof OAJsonNullNode) {
			objResult = null;
		} else {
			OAJaxb jaxb = new OAJaxb(toClass);
			objResult = jaxb.convertFromJSON(fromNode.toJson());
		}

		return (T) objResult;
	}

	public static String convertObjectToJson(final Object obj) throws Exception {
		OAJsonNode node = convertObjectToJsonNode(obj, null);
		return node.toJson();
	}

	public static String convertObjectToJson(final Object obj, List<String> lstIncludePropertyPaths) throws Exception {
		OAJsonNode node = convertObjectToJsonNode(obj, lstIncludePropertyPaths);
		return node.toJson();
	}

	public static String convertObjectToJson(final Object obj, List<String> lstIncludePropertyPaths, final boolean bIncludeOwned)
			throws Exception {
		OAJsonNode node = convertObjectToJsonNode(obj, lstIncludePropertyPaths, bIncludeOwned);
		return node.toJson();
	}

	public static OAJsonNode convertObjectToJsonNode(final Object obj) throws Exception {
		return convertObjectToJsonNode(obj, null, false);
	}

	public static OAJsonNode convertObjectToJsonNode(final Object obj, List<String> lstIncludePropertyPaths) throws Exception {
		return convertObjectToJsonNode(obj, lstIncludePropertyPaths, false);
	}

	public static OAJsonNode convertObjectToJsonNode(final Object obj, final List<String> lstIncludePropertyPaths,
			final boolean bIncludeOwned)
			throws Exception {

		OAJsonNode nodeResult = null;
		if (obj == null) {
			return new OAJsonNullNode();
		}

		if (obj instanceof OAJsonNode) {
			return (OAJsonNode) obj;
		}

		Class c = obj.getClass();
		boolean bIsArray = c.isArray();
		if (bIsArray) {
			c = c.getComponentType();
		}

		boolean bIsHub = (obj instanceof Hub);
		if (bIsHub) {
			c = ((Hub) obj).size() == 0 ? null : ((Hub) obj).get(0).getClass();
		}

		boolean bIsList = !bIsHub && (obj instanceof List);
		if (bIsList) {
			c = ((List) obj).size() == 0 ? null : ((List) obj).get(0).getClass();
		}

		if (OAObject.class.isAssignableFrom(c)) {
			OAJaxb jaxb = new OAJaxb(obj.getClass());
			jaxb.setUseReferences(false);
			jaxb.setIncludeGuids(false);
			jaxb.setIncludeOwned(bIncludeOwned);
			if (lstIncludePropertyPaths != null) {
				for (String s : lstIncludePropertyPaths) {
					jaxb.addPropertyPath(s);
				}
			}

			if (bIsArray) {
				nodeResult = new OAJsonArrayNode();

				int x = Array.getLength(obj);
				for (int i = 0; i < x; i++) {
					Object objx = Array.get(obj, i);

					String s = jaxb.convertToJSON((OAObject) objx);

					OAJson oaJson = new OAJson();
					OAJsonNode node = oaJson.load(s);

					((OAJsonArrayNode) nodeResult).add(node);
				}
			} else if (bIsList) {
				nodeResult = new OAJsonArrayNode();

				for (Object objx : (List) obj) {
					String s = jaxb.convertToJSON((OAObject) objx);

					OAJson oaJson = new OAJson();
					OAJsonNode node = oaJson.load(s);
					((OAJsonArrayNode) nodeResult).add(node);
				}
			} else if (bIsHub) {
				nodeResult = new OAJsonArrayNode();

				for (Object objx : (Hub) obj) {
					String s = jaxb.convertToJSON((OAObject) objx);

					OAJson oaJson = new OAJson();
					OAJsonNode node = oaJson.load(s);
					((OAJsonArrayNode) nodeResult).add(node);
				}
			} else {
				String s = jaxb.convertToJSON((OAObject) obj);
				OAJson oaJson = new OAJson();
				nodeResult = oaJson.load(s);
			}
		} else if (bIsArray) {
			nodeResult = new OAJsonArrayNode();
			int x = Array.getLength(obj);
			for (int i = 0; i < x; i++) {
				Object objx = Array.get(obj, i);
				OAJsonNode nodex = convertObjectToJsonNode(objx, lstIncludePropertyPaths);
				((OAJsonArrayNode) nodeResult).add(nodex);
			}
		} else if (bIsList) {
			nodeResult = new OAJsonArrayNode();
			for (Object objx : (List) obj) {
				OAJsonNode nodex = convertObjectToJsonNode(objx, lstIncludePropertyPaths);
				((OAJsonArrayNode) nodeResult).add(nodex);
			}
		} else if (bIsHub) {
			nodeResult = new OAJsonArrayNode();
			for (Object objx : (Hub) obj) {
				OAJsonNode nodex = convertObjectToJsonNode(objx, lstIncludePropertyPaths);
				((OAJsonArrayNode) nodeResult).add(nodex);
			}
		} else if (obj instanceof Map) {
			nodeResult = new OAJsonObjectNode();
			for (Object key : ((Map) obj).keySet()) {
				String s = OAConv.toString(key);
				Object val = ((Map) obj).get(key);
				OAJsonNode nodex = convertObjectToJsonNode(val, null);
				((OAJsonObjectNode) nodeResult).set(s, nodex);
			}
		} else {
			if (obj == null) {
				nodeResult = new OAJsonNullNode();
			} else if (obj instanceof String) {
				nodeResult = new OAJsonStringNode((String) obj);
			} else if (obj instanceof OAJsonNode) {
				nodeResult = (OAJsonNode) obj;
			} else if (obj instanceof Boolean) {
				nodeResult = new OAJsonBooleanNode((Boolean) obj);
			} else if (obj instanceof Number) {
				nodeResult = new OAJsonNumberNode((Number) obj);
			} else if (obj instanceof OADate) {
				nodeResult = new OAJsonStringNode(((OADate) obj).toString("yyyy-MM-dd"));
			} else if (obj instanceof OATime) {
				nodeResult = new OAJsonStringNode(((OATime) obj).toString("HH:mm:ss"));
			} else if (obj instanceof OADateTime) {
				//qqqqqqqq localdate, etc
				nodeResult = new OAJsonStringNode(((OADateTime) obj).toString("yyyy-MM-dd'T'HH:mm:ss"));
			} else if (obj instanceof Class) {
				nodeResult = new OAJsonStringNode(((Class) obj).getName());
			} else if (obj instanceof OAObjectKey) {
				Object[] objs = ((OAObjectKey) obj).getObjectIds();
				String ok = null;
				for (int i = 0; objs != null && i < objs.length; i++) {
					if (i > 0) {
						ok += "-";
					} else if (ok == null) {
						ok = "";
					}
					ok += OAConv.toString(objs[i]);
				}
				nodeResult = new OAJsonStringNode(ok);
			} else {
				OAJaxb jaxb = new OAJaxb(obj.getClass());
				jaxb.setUseReferences(false);
				jaxb.setIncludeGuids(false);
				jaxb.setIncludeOwned(bIncludeOwned);
				if (lstIncludePropertyPaths != null) {
					for (String s : lstIncludePropertyPaths) {
						jaxb.addPropertyPath(s);
					}
				}

				String s = jaxb.convertToJson(obj);
				OAJson oaJson = new OAJson();
				nodeResult = oaJson.load(s);
			}
		}
		return nodeResult;
	}

	/**
	 * Used by proxy invoke to serialize the arguments into a Json array. This will also include json properties for setting(/casting) if
	 * the object is different then the parameter type.
	 */
	public static OAJsonArrayNode convertMethodArgumentsToJson(final Method method, final Object[] argValues,
			final List<String>[] lstIncludePropertyPathss, final int[] skipParams) throws Exception {
		final OAJsonArrayNode arrayNode = new OAJsonArrayNode();

		if (argValues == null) {
			return arrayNode;
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
				arrayNode.add(new OAJsonStringNode(s));
			}

			OAJsonNode node = convertObjectToJsonNode(	obj,
														lstIncludePropertyPathss != null && i < lstIncludePropertyPathss.length
																? lstIncludePropertyPathss[i]
																: null);
			arrayNode.add(node);
		}

		return arrayNode;
	}

	private static final String methodNextArgumentParamClass = "OANextParamClass:";

	/**
	 * Convert an array of objects to the argument values of a method.
	 */
	public static Object[] convertJsonToMethodArguments(String jsonArray, Method method) throws Exception {
		final OAJson oajson = new OAJson();
		final OAJsonArrayNode nodeArray = oajson.loadArray(jsonArray);
		Object[] objs = convertJsonToMethodArguments(nodeArray, method, null);
		return objs;
	}

	public static Object[] convertJsonToMethodArguments(OAJsonArrayNode nodeArray, Method method, final int[] skipParams) throws Exception {
		if (nodeArray == null || method == null) {
			return null;
		}

		Parameter[] mps = method.getParameters();
		if (mps == null) {
			return null;
		}
		final Object[] margs = new Object[mps.length];

		final int x = nodeArray.getSize();

		int ii = 0;
		for (int i = 0; i < mps.length && i < x; i++) {

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

			OAJsonNode node = nodeArray.get(ii);

			if (node instanceof OAJsonStringNode) {
				String s = ((OAJsonStringNode) node).getValue();
				if (s.startsWith(methodNextArgumentParamClass)) {
					s = s.substring(methodNextArgumentParamClass.length());
					paramClass = Class.forName(s);
					ii++;
					node = nodeArray.get(ii);
				}
			}

			Object objx = convertJsonToObject(node, paramClass, null);
			margs[i] = objx;
			ii++;
		}

		return margs;
	}

	/**
	 * Takes an Object Id that is a String and converts it to an OAObjectKey.
	 * <p>
	 * Json uses a single String value to represent an Object Id/Reference.<br>
	 * OA converts these into a "-" separated String value.
	 *
	 * @param clazz type of class that the Id is for.
	 * @param id    object Id value, "-" separated for multipart keys
	 */
	public static OAObjectKey convertJsonSinglePartIdToObjectKey(Class clazz, String id) {
		if (clazz == null || id == null) {
			return null;
		}

		OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(clazz);
		String[] ids = oi.getIdProperties();

		OAObjectKey ok;
		if (ids == null || ids.length == 0) {
			ok = new OAObjectKey(id);
			;
		} else if (ids.length == 1) {
			Class c = OAObjectInfoDelegate.getPropertyClass(clazz, ids[0]);
			Object obj = OAConverter.convert(c, id, null);
			ok = new OAObjectKey(obj);
		} else {
			Object[] objs = new Object[ids.length];
			int cntField = 1;
			for (int i = 0; ids != null && i < ids.length; i++) {
				Class c = OAObjectInfoDelegate.getPropertyClass(clazz, ids[i]);

				String idx;
				if (c.equals(String.class)) {
					if (i + 1 == ids.length) {
						idx = OAString.field(id, "-", cntField, 99);
					} else {
						idx = OAString.field(id, "-", cntField++);
					}
				} else {
					idx = OAString.field(id, "-", cntField++);
				}
				objs[i] = OAConverter.convert(c, idx, null);
			}
			ok = new OAObjectKey(objs);
		}
		return ok;
	}

	/**
	 * Convert an OAObjectKey to a single String, used by Json single part id.
	 * <p>
	 * Json uses a single String value to represent an Object Id/Reference.<br>
	 * OA converts these into a "-" separated String value.
	 */
	public static String convertObjectKeyToJsonSinglePartId(OAObjectKey oaObjKey) {
		if (oaObjKey == null) {
			return null;
		}

		String ids = null;
		Object[] objs = oaObjKey.getObjectIds();
		if (objs != null) {
			for (Object obj : objs) {
				if (ids == null) {
					ids = "" + obj;
				} else {
					ids += "-" + OAConv.toString(obj);
				}
			}
		}
		return ids;
	}
}
