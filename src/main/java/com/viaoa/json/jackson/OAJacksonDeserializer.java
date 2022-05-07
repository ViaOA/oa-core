package com.viaoa.json.jackson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.viaoa.datasource.OADataSource;
import com.viaoa.datasource.OASelect;
import com.viaoa.hub.Hub;
import com.viaoa.json.OAJson;
import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectCacheDelegate;
import com.viaoa.object.OAObjectDelegate;
import com.viaoa.object.OAObjectEventDelegate;
import com.viaoa.object.OAObjectInfo;
import com.viaoa.object.OAObjectInfoDelegate;
import com.viaoa.object.OAObjectKey;
import com.viaoa.object.OAObjectPropertyDelegate;
import com.viaoa.object.OAObjectReflectDelegate;
import com.viaoa.object.OAPropertyInfo;
import com.viaoa.object.OAThreadLocalDelegate;
import com.viaoa.util.OAArray;
import com.viaoa.util.OAConv;
import com.viaoa.util.OADate;
import com.viaoa.util.OADateTime;
import com.viaoa.util.OAPropertyPath;
import com.viaoa.util.OAString;
import com.viaoa.util.OATime;
import com.viaoa.util.Tuple;

/**
 * Used by OAJackson to convert JSON to OAObject(s).
 */
public class OAJacksonDeserializer extends JsonDeserializer<OAObject> {

	// https://fasterxml.github.io/jackson-databind/javadoc/2.9/com/fasterxml/jackson/databind/JsonDeserializer.html

	@Override
	public OAObject deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JacksonException {

		final OAJson oaj = OAThreadLocalDelegate.getOAJackson();
		final Class clazz = oaj.getReadObjectClass();

		JsonNode node = jp.getCodec().readTree(jp);

		OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(clazz);

		OAObject obj = this.getObject(oi, node);

		return obj;
	}

	protected OAObject getObject(final OAObjectInfo oi, final JsonNode node) {

		final OAJson oaj = OAThreadLocalDelegate.getOAJackson();
		final Class clazz = oi.getForClass();

		oaj.beforeReadCallback(node);

		boolean bIdMissing = false;
		ArrayList alKeys = new ArrayList();
		for (OAPropertyInfo pi : oi.getPropertyInfos()) {
			if (!pi.getId()) {
				continue;
			}

			String propertyName = pi.getLowerName();
			if (!oaj.getUsePropertyCallback(null, propertyName)) {
				continue;
			}
			propertyName = oaj.getPropertyNameCallback(null, propertyName);

			JsonNode jn = node.get(propertyName);

			if (jn == null) {
				bIdMissing = true;
				continue;
			}

			Object objx = convert(jn, pi);

			objx = oaj.getPropertyValueCallback(null, pi.getLowerName(), objx);

			alKeys.add(objx);
		}

		OAObjectKey ok = null;
		OAObject objNew = null;

		if (!bIdMissing) {
			ok = new OAObjectKey(alKeys.toArray());
			objNew = (OAObject) OAObjectCacheDelegate.get(clazz, ok);

			if (objNew == null) {
				objNew = (OAObject) OADataSource.getObject(clazz, ok);
			}
		} else {
			JsonNode jn = node.get("guid");
			if (jn != null) {
				int guid = jn.asInt();

				objNew = oaj.getGuidMap().get(guid);
				if (objNew == null) {
					ok = new OAObjectKey(null, guid, false);
					objNew = (OAObject) OAObjectCacheDelegate.get(clazz, ok);
					if (objNew == null) {
						ok = new OAObjectKey(null, guid, true);
						objNew = (OAObject) OAObjectCacheDelegate.get(clazz, ok);
					}

				}
			}
			if (objNew == null) {
				// try to use importMatches
				final String[] importMatchPropertyNames = oi.getImportMatchPropertyNames();
				if (importMatchPropertyNames != null && importMatchPropertyNames.length > 0) {

					final Map<String, Tuple<OAPropertyPath, Object>> hmNameValue = new HashMap<>();

					int pos = 0;
					String sql = null;
					Object[] params = null;

					for (String propertyName : importMatchPropertyNames) {
						String propertyPath = oi.getImportMatchPropertyPaths()[pos++];
						OAPropertyPath pp = new OAPropertyPath(oi.getForClass(), propertyPath);

						jn = node.get(propertyName);
						if (jn == null) {
							if (pp.getEndPropertyInfo().isKey()) {
								continue;
							}
							sql = null;
							break;
						}

						if (sql == null) {
							sql = "";
						} else {
							sql += " AND ";
						}
						sql += propertyPath + " = ?";

						OAPropertyInfo pi = pp.getEndPropertyInfo();
						Object val = convert(jn, pi);

						if (val == null) {
							sql = null;
							break;
						}

						params = OAArray.add(Object.class, params, val);
					}
					if (sql != null) {
						OASelect sel = new OASelect(oi.getForClass(), sql, params, "");

						objNew = sel.next();
						sel.close();
					}
				}
			}

			if (objNew != null) {
				ok = objNew.getObjectKey();
			}
		}

		if (objNew == null) {
			objNew = (OAObject) OAObjectReflectDelegate.createNewObject(clazz);

			int pos = 0;
			for (OAPropertyInfo pi : oi.getPropertyInfos()) {
				if (!pi.getId()) {
					continue;
				}

				if (!oaj.getUsePropertyCallback(objNew, pi.getLowerName())) {
					continue;
				}

				if (alKeys.size() > pos) {
					Object objx = alKeys.get(pos++);
					objx = oaj.getPropertyValueCallback(objNew, pi.getLowerName(), objx);

					objNew.setProperty(pi.getLowerName(), objx);
				}
			}

			if (OAThreadLocalDelegate.isLoading()) {
				OAObjectDelegate.initializeAfterLoading((OAObject) objNew, false, false);
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

			if (!oaj.getUsePropertyCallback(objNew, pi.getLowerName())) {
				continue;
			}

			String propertyName = oaj.getPropertyNameCallback(objNew, pi.getLowerName());

			jn = node.get(propertyName);
			Object objx = convert(jn, pi);

			objx = oaj.getPropertyValueCallback(objNew, pi.getLowerName(), objx);

			objNew.setProperty(pi.getLowerName(), objx);
		}

		// load links of type=one
		final Set<OALinkInfo> hsLinkFound = new HashSet<>();
		for (final OALinkInfo li : oi.getLinkInfos()) {
			if (li.getType() != li.TYPE_ONE) {
				continue;
			}
			if (li.getPrivateMethod()) {
				continue;
			}

			if (!oaj.getUsePropertyCallback(objNew, li.getLowerName())) {
				continue;
			}

			String propertyName = oaj.getPropertyNameCallback(objNew, li.getLowerName());
			JsonNode nodex = node.get(propertyName);

			if (nodex != null) {
				hsLinkFound.add(li);
				OAObject objx = getLinkObject(objNew, li, nodex);

				objx = (OAObject) oaj.getPropertyValueCallback(objNew, li.getLowerName(), objx);

				objNew.setProperty(li.getLowerName(), objx);
			} else {
				final String[] ss = li.getUsesProperties();
				if (ss != null && ss.length > 0) {
					ArrayList<Object> al = new ArrayList();
					for (String s : ss) {
						OAPropertyInfo pi = oi.getPropertyInfo(s);
						jn = node.get(pi.getLowerName());
						if (jn == null) {
							al.clear();
							break;
						}
						Object objx = convert(jn, pi);
						objx = oaj.getPropertyValueCallback(objNew, pi.getLowerName(), objx);
						al.add(objx);
					}
					if (al.size() > 0) {
						hsLinkFound.add(li);
						Object[] objs = al.toArray(new Object[al.size()]);
						OAObjectKey okx = new OAObjectKey(objs);

						objNew.setProperty(li.getLowerName(), okx);
					}
				} else {
					final String[] pojoNames = li.getPojoNames();

					ArrayList<Object> al = new ArrayList();
					OAObjectInfo oix = li.getToObjectInfo();
					int pos = 0;
					for (OAPropertyInfo pi : oix.getPropertyInfos()) {
						if (!pi.getId()) {
							continue;
						}

						String name;
						if (pojoNames != null && pos < pojoNames.length) {
							name = pojoNames[pos++];
						} else {
							name = li.getLowerName() + pi.getName();
						}

						jn = node.get(name);
						if (jn == null) {
							al.clear();
							break;
						}
						Object objx = convert(jn, pi);
						al.add(objx);
					}
					if (al.size() > 0) {
						hsLinkFound.add(li);
						Object[] objs = al.toArray(new Object[al.size()]);
						OAObjectKey okx = new OAObjectKey(objs);

						objNew.setProperty(li.getLowerName(), okx);
					}
				}
			}
		}

		// 20220504
		// load any (optional) ImportMatch properties for One-Links.
		//   These are "extra" properties added to Pojos that can use importMatch value(s) instead of pkey(s)
		final String[] importMatchPropertyNames = oi.getImportMatchPropertyNames();
		if (importMatchPropertyNames != null && importMatchPropertyNames.length > 0) {

			final Map<String, Tuple<OAPropertyPath, Object>> hmNameValue = new HashMap<>();

			int pos = 0;
			for (String propertyName : importMatchPropertyNames) {
				String propertyPath = oi.getImportMatchPropertyPaths()[pos];
				OAPropertyPath pp = new OAPropertyPath(objNew.getClass(), propertyPath);
				OAPropertyInfo pi = pp.getEndPropertyInfo();

				jn = node.get(propertyName);
				if (jn != null) {
					Object val = convert(jn, pi);
					if (val != null || pi.getKey()) {
						hmNameValue.put(propertyName, new Tuple(pp, val));
					}
				}
				pos++;
			}

			if (hmNameValue.size() > 0) {
				// see if oaObjKey can be created.  This is only when the linkTo's pkey prop values are included.
				//   otherwise, the other values could be importMatch values that can be used to find the link object (at later time)
				for (final OALinkInfo li : oi.getLinkInfos()) {
					if (li.getType() != li.TYPE_ONE) {
						continue;
					}
					if (hsLinkFound.contains(li)) {
						continue;
					}

					final String[] keyProps = li.getToObjectInfo().getKeyProperties();
					final List<Object> alKey = new ArrayList<>();
					for (String keyProp : keyProps) {
						OAPropertyInfo pi = li.getToObjectInfo().getPropertyInfo(keyProp);
						for (String propertyName : importMatchPropertyNames) {
							Tuple<OAPropertyPath, Object> t = hmNameValue.get(propertyName);
							if (t == null) {
								continue;
							}

							OAPropertyPath pp = t.a;
							Object val = t.b;

							OALinkInfo[] lis = pp.getLinkInfos();
							if (lis != null && lis.length == 1 && pp.getEndPropertyInfo() == pi) {
								alKey.add(val);
							}
						}
					}
					if (alKey.size() == keyProps.length) {
						Object[] objs = alKey.toArray(new Object[alKey.size()]);
						OAObjectKey okx = new OAObjectKey(objs);

						Object objValue = OAObjectPropertyDelegate.getProperty(objNew, li.getName());
						Object objx = objValue;
						if (objx instanceof OAObject) {
							objx = ((OAObject) objx).getObjectKey();
						}
						if (objx instanceof OAObjectKey) {
							if (!okx.equals(objx)) {
								OAObjectPropertyDelegate.setProperty(objNew, li.getName(), okx);
								if (objValue instanceof OAObject) {
									OAObjectEventDelegate.sendHubPropertyChange(objNew, li.getName(), objValue, okx, li);
								}
							}
						}
					} else {
						// try to use importMatch values to query for the correct object
						pos = 0;
						String sql = "";
						Object[] params = null;
						for (String propertyName : importMatchPropertyNames) {
							String propertyPath = oi.getImportMatchPropertyPaths()[pos++];
							OAPropertyPath pp = new OAPropertyPath(objNew.getClass(), propertyPath);
							OALinkInfo[] lis = pp.getLinkInfos();
							if (lis == null || lis.length == 0 || lis[0] != li) {
								continue;
							}

							OAPropertyInfo pix = pp.getEndPropertyInfo();
							if (pix.getId()) { // only using the importMathes for this
								continue;
							}

							Tuple<OAPropertyPath, Object> t = hmNameValue.get(propertyName);
							Object val;
							if (t == null) {
								val = null;
							} else {
								val = t.b;
							}

							if (OAString.isNotEmpty(sql)) {
								sql += " AND ";
							}
							sql += OAString.field(t.a.getPropertyPath(), ".", 2, 99) + " = ?";
							params = OAArray.add(Object.class, params, val);
						}
						OASelect sel = new OASelect(li.getToObjectInfo().getForClass(), sql, params, "");

						Object objLookup = sel.next();
						Object objCurrent = OAObjectPropertyDelegate.getProperty(objNew, li.getName());

						boolean bChanged = (objLookup != objCurrent);

						if (bChanged && objCurrent instanceof OAObjectKey) {
							if (objLookup instanceof OAObject) {
								OAObjectKey okx = ((OAObject) objLookup).getObjectKey();
								bChanged = !okx.equals(objCurrent);
							}
						}

						if (bChanged) {
							objNew.setProperty(li.getName(), objLookup);
						}
					}
				}
			}
		}

		// load links of type=many
		for (OALinkInfo li : oi.getLinkInfos()) {
			if (li.getType() != li.TYPE_MANY) {
				continue;
			}
			if (li.getPrivateMethod()) {
				continue;
			}

			if (!oaj.getUsePropertyCallback(objNew, li.getLowerName())) {
				continue;
			}

			String propertyName = oaj.getPropertyNameCallback(objNew, li.getLowerName());
			JsonNode nodex = node.get(propertyName);
			if (nodex instanceof ArrayNode) {

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
		oaj.afterReadCallback(node, objNew);

		return objNew;
	}

	protected Object convert(final JsonNode jn, final OAPropertyInfo pi) {
		if (jn == null) {
			return null;
		}
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
		final OAJson oaj = OAThreadLocalDelegate.getOAJackson();

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
				OAObjectKey ok = OAJson.convertJsonSinglePartIdToObjectKey(li.getToClass(), s);

				objNew = (OAObject) OAObjectCacheDelegate.get(li.getToClass(), ok);
				if (objNew != null) {
					fromObject.setProperty(li.getName(), objNew);
				} else {
					// just save the objectKey in the object's property ??
					Object objx = OAObjectPropertyDelegate.getProperty(fromObject, li.getName(), false, true);
					if (objx instanceof OAObjectKey) {
						OAObjectPropertyDelegate.setPropertyCAS(fromObject, li.getName(), ok, objx);
					} else if (objx instanceof OAObject) {
						// need to get and replace, since it's loaded
					} else if (objx == null) {
						//  need to get and replace, since it could be loaded
					}

					// get from DS
					objNew = (OAObject) OADataSource.getObject(li.getToClass(), ok);
				}
			}
		}
		return objNew;
	}
}
