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
import com.viaoa.json.node.OAJsonStringNode;
import com.viaoa.object.OAObject;
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
 * JSON Util methods for converting from/to json/objects.
 *
 * @author vvia
 */
public class OAJsonUtil {

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
	 * qqqqqqqq Note: only used in unittest
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

	/**
	 * Used by proxy invoke to serialize the arguments into a Json array.
	 */
	public static OAJsonArrayNode convertMethodArgumentsToJson(final Method method, final Object[] argValues,
			final List<String>[] lstIncludePropertyPathss, boolean bSkipFirst) throws Exception {
		final OAJsonArrayNode arrayNode = new OAJsonArrayNode();

		if (argValues == null) {
			return arrayNode;
		}

		int i = -1;
		for (Object obj : argValues) {
			i++;
			if (i == 0 && bSkipFirst) {
				continue;
			}
			OAJsonNode node = convertObjectToJsonNode(	obj,
														lstIncludePropertyPathss != null && i < lstIncludePropertyPathss.length
																? lstIncludePropertyPathss[i]
																: null);
			arrayNode.add(node);
		}

		return arrayNode;
	}

	/**
	 * Convert an array of objects to the argument values of a method.
	 */
	public static Object[] convertJsonToMethodArguments(String jsonArray, Method method) throws Exception {
		final OAJson oajson = new OAJson();
		final OAJsonArrayNode nodeArray = oajson.loadArray(jsonArray);
		Object[] objs = convertJsonToMethodArguments(nodeArray, method);
		return objs;
	}

	public static Object[] convertJsonToMethodArguments(OAJsonArrayNode nodeArray, Method method) throws Exception {
		if (nodeArray == null || method == null) {
			return null;
		}

		Parameter[] mps = method.getParameters();
		if (mps == null) {
			return null;
		}
		final Object[] margs = new Object[mps.length];

		final int x = nodeArray.getSize();

		for (int i = 0; i < mps.length && i < x; i++) {
			final Parameter param = mps[i];
			final Class paramClass = param.getType();

			OAJsonNode node = nodeArray.get(i);
			Object objx = convertJsonToObject(node, param.getType(), null);
			margs[i] = objx;
		}

		return margs;
	}

	public static Object convertJsonToObject(final String json, Class toClass) throws Exception {
		Object obj = convertJsonToObject(json, toClass, null);
		return obj;
	}

	public static Object convertJsonToObject(final String json, Class toClass, final Class classInCollection) throws Exception {
		OAJson oaj = new OAJson();
		OAJsonNode node = oaj.load(json);
		Object obj = convertJsonToObject(node, toClass, classInCollection);
		return obj;
	}

	public static Object convertJsonToObject(final OAJsonNode fromNode, Class toClass, final Class classInCollection) throws Exception {
		if (fromNode == null || toClass == null) {
			return null;
		}

		final Class origToClass = toClass;

		final boolean bIsArray = origToClass.isArray();
		final boolean bIsHub = List.class.isAssignableFrom(origToClass);
		final boolean bIsList = !bIsHub && List.class.isAssignableFrom(origToClass);

		if ((bIsList || bIsHub) && classInCollection == null) {
			throw new Exception("listClass can not be null");
		}

		toClass = bIsArray ? origToClass.getComponentType() : (bIsList ? classInCollection : origToClass);

		if (fromNode.getClass().isAssignableFrom(origToClass)) {
			return fromNode;
		}

		Object objResult = null;
		if (toClass.equals(String.class)) {
			objResult = fromNode.toJson();
			return objResult;
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
			return objResult;
		}

		if (toClass.equals(void.class) || toClass.equals(Void.class)) {
			objResult = null;
			return objResult;
		}

		if (toClass.equals(String.class)) {
			objResult = fromNode.toJson();
			return objResult;
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
			return objResult;
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

		return objResult;
	}

	public static String convertObjectToJson(final Object obj) throws Exception {
		OAJsonNode node = convertObjectToJsonNode(obj, null);
		return node.toJson();
	}

	public static String convertObjectToJson(final Object obj, List<String> lstIncludePropertyPaths) throws Exception {
		OAJsonNode node = convertObjectToJsonNode(obj, lstIncludePropertyPaths);
		return node.toJson();
	}

	public static OAJsonNode convertObjectToJsonNode(final Object obj, List<String> lstIncludePropertyPaths)
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
		boolean bIsList = (obj instanceof List);
		if (bIsList) {
			c = ((List) obj).size() == 0 ? null : ((List) obj).get(0).getClass();
		}
		boolean bIsHub = (obj instanceof Hub);
		if (bIsHub) {
			c = ((Hub) obj).size() == 0 ? null : ((Hub) obj).get(0).getClass();
		}

		if (OAObject.class.isAssignableFrom(c)) {
			OAJaxb jaxb = new OAJaxb(obj.getClass());
			jaxb.setUseReferences(false);
			jaxb.setIncludeGuids(false);
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
			} else {
				OAJaxb jaxb = new OAJaxb(obj.getClass());
				jaxb.setUseReferences(false);
				jaxb.setIncludeGuids(false);
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
}
