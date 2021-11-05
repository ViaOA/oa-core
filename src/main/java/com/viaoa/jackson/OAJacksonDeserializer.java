package com.viaoa.jackson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.viaoa.datasource.OADataSource;
import com.viaoa.hub.Hub;
import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectCacheDelegate;
import com.viaoa.object.OAObjectInfo;
import com.viaoa.object.OAObjectInfoDelegate;
import com.viaoa.object.OAObjectKey;
import com.viaoa.object.OAObjectPropertyDelegate;
import com.viaoa.object.OAObjectReflectDelegate;
import com.viaoa.object.OAPropertyInfo;
import com.viaoa.object.OAThreadLocalDelegate;
import com.viaoa.util.OAConv;
import com.viaoa.util.OADate;
import com.viaoa.util.OADateTime;
import com.viaoa.util.OATime;

/**
 * Used by OAJackson to convert JSON to OAObject(s).
 */
public class OAJacksonDeserializer extends JsonDeserializer<OAObject> {

	// https://fasterxml.github.io/jackson-databind/javadoc/2.9/com/fasterxml/jackson/databind/JsonDeserializer.html

	@Override
	public OAObject deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JacksonException {

		final OAJackson oaj = OAThreadLocalDelegate.getOAJackson();
		final Class clazz = oaj.getReadObjectClass();

		JsonNode node = jp.getCodec().readTree(jp);

		OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(clazz);

		OAObject obj = getObject(oi, node);

		return obj;
	}

	protected OAObject getObject(OAObjectInfo oi, JsonNode node) {

		final OAJackson oaj = OAThreadLocalDelegate.getOAJackson();
		Class clazz = oi.getForClass();

		ArrayList alKeys = new ArrayList();
		for (OAPropertyInfo pi : oi.getPropertyInfos()) {
			if (!pi.getId()) {
				continue;
			}

			JsonNode jn = node.get(pi.getLowerName());

			Object objx = convert(jn, pi);
			alKeys.add(objx);
		}
		OAObjectKey ok = new OAObjectKey(alKeys.toArray());

		OAObject objNew = (OAObject) OAObjectCacheDelegate.get(clazz, ok);

		if (objNew == null) {
			objNew = (OAObject) OADataSource.getObject(clazz, ok);
			if (objNew == null) {
				objNew = (OAObject) OAObjectReflectDelegate.createNewObject(clazz);

				int i = 0;
				for (OAPropertyInfo pi : oi.getPropertyInfos()) {
					if (!pi.getId()) {
						continue;
					}
					objNew.setProperty(pi.getName(), alKeys.get(i++));
				}
			}
		}

		JsonNode jn = node.get("guid");
		if (jn != null) {
			int guid = jn.asInt();
			if (oaj != null) {
				oaj.getGuidMap().put(guid, objNew);
			}
		}

		// load properties
		for (OAPropertyInfo pi : oi.getPropertyInfos()) {
			if (pi.getId()) {
				continue;
			}

			jn = node.get(pi.getLowerName());
			Object objx = convert(jn, pi);
			objNew.setProperty(pi.getName(), objx);
		}

		// load links
		for (OALinkInfo li : oi.getLinkInfos()) {
			if (li.getType() != li.TYPE_ONE) {
				continue;
			}
			if (li.getPrivateMethod()) {
				continue;
			}

			//qqqqqqqqq check property to see if it's an ObjectKey (not loaded)
			JsonNode nodex = node.get(li.getLowerName());

			//qqqqqqq test between having a null value, vs not sent (blank) qqqqqqqqq

			if (nodex != null) {
				OAObject objx = getLinkObject(objNew, li, nodex);

				//qqqqqqqqqqqq need to take into account if only objKey is loaded

				objNew.setProperty(li.getName(), objx);
			}
		}

		// load many links
		for (OALinkInfo li : oi.getLinkInfos()) {
			if (li.getType() != li.TYPE_MANY) {
				continue;
			}
			if (li.getPrivateMethod()) {
				continue;
			}

			JsonNode nodex = node.get(li.getLowerName());
			if (nodex instanceof ArrayNode) {

				//qqqqqqq will need to add/remove objects in hub qqqqqqqqqqq

				Hub<OAObject> hub = (Hub<OAObject>) li.getValue(objNew);
				ArrayNode nodeArray = (ArrayNode) nodex;

				List<OAObject> alNew = new ArrayList();
				int x = nodeArray.size();
				for (int i = 0; i < x; i++) {
					nodex = nodeArray.get(i);
					OAObject objx = getLinkObject(objNew, li, nodex);
					alNew.add(objx);
				}

				List<OAObject> alRemove = new ArrayList();
				for (OAObject objx : hub) {
					if (!alNew.contains(objx)) {
						alRemove.add(objx);
					}
				}
				for (OAObject objx : alNew) {
					if (!hub.contains(objx)) {
						hub.add(objx);
					}
				}
				for (OAObject objx : alRemove) {
					hub.remove(objx);
				}

				// same order
				int i = 0;
				for (OAObject objx : alNew) {
					int pos = hub.getPos(objx);
					hub.move(pos, i);
					i++;
				}
			}
		}

		return objNew;

	}

	protected Object convert(final JsonNode jn, final OAPropertyInfo pi) {
		Object objx;
		if (jn.isNull()) {
			objx = null;
		} else {
			if (pi.isNameValue()) {
				objx = jn.asText();
				if (objx != null) {
					for (int i = 0; i < pi.getNameValues().size(); i++) {
						if (((String) objx).equalsIgnoreCase(pi.getNameValues().get(i))) {
							objx = i;
							break;
						}
					}
				}
			} else if (jn.isTextual()) {
				Class paramClass = pi.getClassType();
				String fmt = null;
				if (paramClass.equals(OADate.class)) {
					fmt = "yyyy-MM-dd";
				} else if (paramClass.equals(OADateTime.class)) {
					fmt = "yyyy-MM-dd'T'HH:mm:ss";
				} else if (paramClass.equals(OATime.class)) {
					fmt = "HH:mm:ss";
				}
				objx = OAConv.convert(pi.getClassType(), jn.asText(), fmt);
			} else {
				objx = OAConv.convert(pi.getClassType(), jn.asText());
			}
		}
		return objx;
	}

	protected OAObject getLinkObject(OAObject fromObject, OALinkInfo li, JsonNode nodeForLinkProperty) {
		final OAJackson oaj = OAThreadLocalDelegate.getOAJackson();

		OAObject objNew = null;
		if (nodeForLinkProperty.isObject()) {
			objNew = getObject(li.getToObjectInfo(), nodeForLinkProperty);

		} else if (nodeForLinkProperty.isNull()) {
			// no-op
		} else if (nodeForLinkProperty.isNumber()) {
			// single part id
			OAObjectKey ok = new OAObjectKey(nodeForLinkProperty.asLong());
			objNew = (OAObject) OAObjectCacheDelegate.get(li.getToClass(), ok);
			if (objNew == null) {
				objNew = (OAObject) OADataSource.getObject(li.getToClass(), ok);
			}
		} else if (nodeForLinkProperty.isTextual()) {
			String s = nodeForLinkProperty.asText();
			if (s.indexOf("guid.") == 0) {
				s = s.substring(5);
				int guid = Integer.parseInt(s);
				if (oaj != null) {
					objNew = oaj.getGuidMap().get(guid);
				}
			} else {
				OAObjectKey ok = OAJackson.convertJsonSinglePartIdToObjectKey(li.getToClass(), s);

				objNew = (OAObject) OAObjectCacheDelegate.get(li.getToClass(), ok);
				if (objNew != null) {
					fromObject.setProperty(li.getName(), objNew);
				} else {
					// just save the objectKey in the object's property ??
					Object objx = OAObjectPropertyDelegate.getProperty(fromObject, li.getName(), false, true);
					if (objx instanceof OAObjectKey) {
						OAObjectPropertyDelegate.setPropertyCAS(fromObject, li.getName(), ok, objx);
					} else if (objx instanceof OAObject) {
						//qqqq need to get and replace, since it's loaded
					} else if (objx == null) {
						//qqqq need to get and replace, since it could be loaded
					}

					// get from DS
					objNew = (OAObject) OADataSource.getObject(li.getToClass(), ok);
				}
			}
		}
		return objNew;
	}
}
