package com.viaoa.jackson;

import java.io.IOException;
import java.util.ArrayList;

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
			Object objx = OAConv.convert(pi.getClassType(), jn.asText());
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
				} else {
					objx = OAConv.convert(pi.getClassType(), jn.asText());
				}
			}
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

			JsonNode nodex = node.get(li.getLowerName());

			OAObject objx = getLinkObject(objNew, li, nodex);
			objNew.setProperty(li.getName(), objx);
		}

		// load many links
		for (OALinkInfo li : oi.getLinkInfos()) {
			if (li.getType() != li.TYPE_MANY) {
				continue;
			}
			if (li.getPrivateMethod()) {
				continue;
			}

			Hub hub = (Hub) li.getValue(objNew);

			JsonNode nodex = node.get(li.getLowerName());
			if (nodex instanceof ArrayNode) {
				ArrayNode nodeArray = (ArrayNode) nodex;

				int x = nodeArray.size();
				for (int i = 0; i < x; i++) {
					nodex = nodeArray.get(i);
					OAObject objx = getLinkObject(objNew, li, nodex);
					hub.add(objx);
				}
			}
		}

		return objNew;

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
				OAObjectKey ok = convertJsonSinglePartIdToObjectKey(li.getToClass(), s);

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

}
