package com.viaoa.json.jackson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
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
import com.viaoa.filter.OAQueryFilter;
import com.viaoa.hub.Hub;
import com.viaoa.json.OAJson;
import com.viaoa.object.OAFinder;
import com.viaoa.object.OAFkeyInfo;
import com.viaoa.object.OALinkInfo;
import com.viaoa.object.OAObject;
import com.viaoa.object.OAObjectCacheDelegate;
import com.viaoa.object.OAObjectDelegate;
import com.viaoa.object.OAObjectImportMatchDelegate;
import com.viaoa.object.OAObjectImportMatchDelegate.ImportMatch;
import com.viaoa.object.OAObjectImportMatchDelegate.ImportMatchDetail;
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
 * Used by OAJson to convert JSON to OAObject(s).
 * <p>
 * This supports object graphs, and the use of importMatch properties.
 */
public class OAJacksonDeserializer extends JsonDeserializer<OAObject> {

	// https://fasterxml.github.io/jackson-databind/javadoc/2.9/com/fasterxml/jackson/databind/JsonDeserializer.html

	@Override
	public OAObject deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JacksonException {

		final OAJson oaj = OAThreadLocalDelegate.getOAJackson();
		final Class clazz = oaj.getReadObjectClass();

		JsonNode node = jp.getCodec().readTree(jp);

		OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(clazz);

		OAObject obj = getObject(oi, node, null, null);

		// use the import match property values to finish populating object graph
		List<ImportMatch> alImportMatch = oaj.getImportMatchList();
		for (ImportMatch im : alImportMatch) {
			OAObjectImportMatchDelegate.process(im);
		}

		return obj;
	}

	protected OAObject getObject(final OAObjectInfo oi, final JsonNode node, final String extraWhereClause, final Object extraWhereParam) {
		final OAJson oaj = OAThreadLocalDelegate.getOAJackson();
		final Class clazz = oi.getForClass();

		oaj.beforeReadCallback(node);

		LinkedList<Object> llStack = oaj.getStack();

		OAObject root = oaj.getRoot();
		boolean bUseRoot = false;
		if (root != null) {
			if (root.getClass().equals(clazz)) {
				if (llStack.size() == 0) {
					bUseRoot = true;
				}
			}
		}

		OAObject obj = _getObject1(oaj, oi, node, (bUseRoot ? root : null), extraWhereClause, extraWhereParam);
		return obj;
	}

	protected void loadObject(final OAObject oaObject, final JsonNode node) {
		if (oaObject == null) {
			return;
		}
		final OAJson oaj = OAThreadLocalDelegate.getOAJackson();
		OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(oaObject);

		_getObject1(oaj, oi, node, oaObject, null, null);
	}

	private OAObject _getObject1(final OAJson oaj, final OAObjectInfo oi, final JsonNode node,
			final OAObject oaObjectToUse,
			final String extraWhereClause, final Object extraWhereParam) {

		LinkedList<Object> llStack = oaj.getStack();
		llStack.add(node);
		OAObject objx;
		try {
			objx = _getObject2(oaj, oi, node, oaObjectToUse, extraWhereClause, extraWhereParam);

		} finally {
			llStack.remove(node);
		}
		return objx;
	}

	private OAObject _getObject2(final OAJson oaj, final OAObjectInfo oi, final JsonNode node, final OAObject oaObjectToUse,
			final String extraWhereClause, final Object extraWhereParam) {

		final List<ImportMatch> alImportMatchInfo = oaj.getImportMatchList();

		OAObject oaObj = _getObject3(oaj, oi, node, oaObjectToUse, extraWhereClause, extraWhereParam, alImportMatchInfo);

		return oaObj;
	}

	private OAObject _getObject3(final OAJson oaj, final OAObjectInfo oi, final JsonNode node, final OAObject oaObjectToUse,
			final String extraWhereClause, final Object extraWhereParam, final List<ImportMatch> alImportMatch) {

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

		OAObject objNew = null;
		OAObjectKey objKey = null;
		Object[] importMatchValues = null;

		if (oaObjectToUse != null) {
			objNew = oaObjectToUse;
		} else if (!bIdMissing) {
			objKey = new OAObjectKey(alKeys.toArray());
			objNew = (OAObject) OAObjectCacheDelegate.get(clazz, objKey);

			if (objNew == null) {
				objNew = (OAObject) OADataSource.getObject(clazz, objKey);
			}
		} else {
			JsonNode jn = node.get("guid");
			if (jn != null) {
				int guid = jn.asInt();

				objNew = oaj.getGuidMap().get(guid);
				if (objNew == null) {
					objKey = new OAObjectKey(null, guid, false);
					objNew = (OAObject) OAObjectCacheDelegate.get(clazz, objKey);
					if (objNew == null) {
						objKey = new OAObjectKey(null, guid, true);
						objNew = (OAObject) OAObjectCacheDelegate.get(clazz, objKey);
					}

				}
			}
			if (objNew == null && OAString.isNotEmpty(oi.getImportMatchPropertyNames())) {
				// try to use importMatches
				final String[] importMatchPropertyNames = oi.getImportMatchPropertyNames();

				final Map<String, Tuple<OAPropertyPath, Object>> hmNameValue = new HashMap<>();

				int pos = 0;
				String sql = null;

				for (String propertyName : importMatchPropertyNames) {
					String propertyPath = oi.getImportMatchPropertyPaths()[pos++];
					OAPropertyPath pp = new OAPropertyPath(oi.getForClass(), propertyPath);

					jn = node.get(propertyName);
					if (jn == null) {
						if (pp.getEndPropertyInfo() != null && pp.getEndPropertyInfo().isKey()) {
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

					OAPropertyInfo pi;
					if (OAString.isEmpty(propertyPath)) {
						sql += propertyName + " = ?";
						pi = oi.getPropertyInfo(propertyName);
					} else {
						sql += propertyPath + " = ?";
						pi = pp.getEndPropertyInfo();
					}

					Object val = convert(jn, pi);

					if (val == null) {
						sql = null;
						break;
					}

					importMatchValues = OAArray.add(Object.class, importMatchValues, val);
				}
				if (sql != null) {
					// first, see if extraWhere can be used to find objNew
					boolean bWasSearched = false;
					if (OAString.isNotEmpty(extraWhereClause) && extraWhereParam instanceof OAObject) {
						String s = OAString.field(extraWhereClause, " = ", 1);
						OALinkInfo li = oi.getLinkInfo(s);
						OALinkInfo rli = li.getReverseLinkInfo();
						Object objx = rli.getValue(extraWhereParam);
						if (objx instanceof Hub) {
							Hub hub = (Hub) objx;

							OAFinder finder = new OAFinder();
							OAQueryFilter filter = new OAQueryFilter(oi.getForClass(), sql, importMatchValues);
							finder.addFilter(filter);
							objNew = finder.findFirst(hub);

							bWasSearched = true;
						} else if (objx instanceof OAObject) {
							OAObject oaobj = (OAObject) objx;

							OAFinder finder = new OAFinder();
							OAQueryFilter filter = new OAQueryFilter(oi.getForClass(), sql, importMatchValues);
							finder.addFilter(filter);
							objNew = finder.findFirst(oaobj);

							bWasSearched = true;
						}
					}

					if (!bWasSearched) {
						if (OAString.isNotEmpty(extraWhereClause)) {
							sql += " AND " + extraWhereClause;
							importMatchValues = OAArray.add(Object.class, importMatchValues, extraWhereParam);
						}

						OASelect sel = new OASelect(oi.getForClass(), sql, importMatchValues, "");
						objNew = sel.next();
						sel.close();

						if (objNew == null) {
							OAFinder finder = new OAFinder();
							OAQueryFilter filter = new OAQueryFilter(oi.getForClass(), sql, importMatchValues);
							finder.addFilter(filter);
							objNew = (OAObject) OAObjectCacheDelegate.find(oi.getForClass(), finder);
						}
					}
				}
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

		if (objNew != null) {
			objKey = objNew.getObjectKey();
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

			if (nodex != null && nodex.isNull()) {
				objNew.setProperty(li.getLowerName(), null);
			} else if (nodex != null) {
				hsLinkFound.add(li);
				OAObject objx = getLinkObject(objNew, li, nodex);

				objx = (OAObject) oaj.getPropertyValueCallback(objNew, li.getLowerName(), objx);
				if (OAThreadLocalDelegate.isLoading()) {
					OAObjectPropertyDelegate.setProperty(objNew, li.getLowerName(), objx);
					final OALinkInfo liRev = li.getReverseLinkInfo();
					if (liRev.getType() == liRev.TYPE_ONE) {
						if (!liRev.getPrivateMethod()) {
							OAObjectPropertyDelegate.setProperty(objx, liRev.getLowerName(), objNew);
						}
					}
				} else {
					objNew.setProperty(li.getLowerName(), objx);
				}
			} else {

				if (li.getFkeyInfos().size() > 0) {
					ArrayList<Object> al = new ArrayList();
					for (OAFkeyInfo fi : li.getFkeyInfos()) {
						if (fi.getFromPropertyInfo() == null) {
							continue;
						}
						String s = fi.getFromPropertyInfo().getName();
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

						OAObject objx = (OAObject) OAObjectCacheDelegate.get(li.getToClass(), okx);
						if (objx == null) {
							objx = (OAObject) OADataSource.getObject(li.getToClass(), okx);
							if (objx == null) {
								objx = (OAObject) OAObjectReflectDelegate.createNewObject(li.getToClass());
								// need to populate
								int i = 0;
								for (OAFkeyInfo fi : li.getFkeyInfos()) {
									String s = fi.getFromPropertyInfo().getName();
									Object val = al.get(i++);
									objx.setProperty(s, val);
								}
							}
						}
						objNew.setProperty(li.getLowerName(), objx);
					}
				} else {
					ArrayList<Object> al = new ArrayList();
					OAObjectInfo oix = li.getToObjectInfo();
					int pos = 0;
					for (OAPropertyInfo pi : oix.getPropertyInfos()) {
						if (!pi.getId()) {
							continue;
						}

						String name;
						if (li.getFkeyInfos() != null && pos < li.getFkeyInfos().size()) {
							name = li.getFkeyInfos().get(pos++).getFromPropertyInfo().getName();
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

						OAObject objx = (OAObject) OAObjectCacheDelegate.get(li.getToClass(), okx);
						if (objx == null) {
							objx = (OAObject) OADataSource.getObject(li.getToClass(), okx);
							if (objx == null) {
								objx = (OAObject) OAObjectReflectDelegate.createNewObject(li.getToClass());
								// need to populate
								int i = 0;
								for (OAPropertyInfo pi : oix.getPropertyInfos()) {
									if (!pi.getId()) {
										continue;
									}
									Object val = objs[i++];
									objx.setProperty(pi.getName(), val);
								}
							}
						}
						objNew.setProperty(li.getLowerName(), objx);
					}
				}
			}
		}

		// load any (optional) ImportMatch properties for finding OneLinks.
		//   These are "extra" properties added to Pojos that can use importMatch value(s) instead of p/fkey(s)
		for (final OALinkInfo li : oi.getLinkInfos()) {
			if (li.getType() != li.TYPE_ONE) {
				continue;
			}
			if (li.getPrivateMethod()) {
				continue;
			}
			if (hsLinkFound.contains(li)) {
				continue;
			}

			if (!oaj.getUsePropertyCallback(objNew, li.getLowerName())) {
				continue;
			}

			final OAObjectInfo oix = li.getToObjectInfo();
			final String[] importMatchPropertyNames = oix.getImportMatchPropertyNames();

			if (importMatchPropertyNames == null || importMatchPropertyNames.length == 0) {
				continue;
			}

			hsLinkFound.add(li);
			final Map<String, Tuple<OAPropertyPath, Object>> hmNameValue = new HashMap<>();

			boolean bIsNull = false;
			int pos = 0;
			for (final String propertyName : importMatchPropertyNames) {
				String propertyPath = oix.getImportMatchPropertyPaths()[pos];

				OAPropertyPath pp;
				if (OAString.isEmpty(propertyPath)) {
					pp = new OAPropertyPath(li.getToClass(), propertyName);
				} else {
					pp = new OAPropertyPath(li.getToClass(), propertyPath);
				}
				OAPropertyInfo pi = pp.getEndPropertyInfo();

				jn = node.get(propertyName);
				if (jn != null) {
					if (jn.isNull()) {
						bIsNull = true;
					} else {
						Object val = convert(jn, pi);
						hmNameValue.put(propertyName, new Tuple(pp, val));
					}
				}
				pos++;
			}

			if (bIsNull) {
				objNew.setProperty(li.getLowerName(), null);
				continue;
			}

			if (hmNameValue.size() == 0) {
				continue;
			}

			// see if oaObjKey can be created.  This is only when the linkTo's pkey prop values are included.
			//   otherwise, the other values could be importMatch values that can be used to find the link object (at later time)

			final String[] keyProps = li.getToObjectInfo().getKeyProperties();
			final List<Object> alKey = new ArrayList<>();
			for (String keyProp : keyProps) {
				OAPropertyInfo pi = li.getToObjectInfo().getPropertyInfo(keyProp);
				for (String propertyName : importMatchPropertyNames) {
					Tuple<OAPropertyPath, Object> t = hmNameValue.get(propertyName);
					if (t == null || t.b == null) {
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

				OAObject objx = (OAObject) OAObjectCacheDelegate.get(li.getToClass(), okx);
				if (objx == null) {
					objx = (OAObject) OADataSource.getObject(li.getToClass(), okx);
					if (objx == null) {
						objx = (OAObject) OAObjectReflectDelegate.createNewObject(li.getToClass());
						// need to populate
						int i = 0;
						for (String keyProp : keyProps) {
							Object val = alKey.get(i++);
							objx.setProperty(keyProp, val);
						}

					}
				}
				objNew.setProperty(li.getLowerName(), objx);

			} else {
				ImportMatch imi = new ImportMatch();
				imi.fromObject = objNew;
				imi.liTo = li;

				boolean bValid = false;
				pos = 0;

				for (String propertyName : importMatchPropertyNames) {
					String propertyPath = oix.getImportMatchPropertyPaths()[pos++];

					final OAPropertyPath pp = new OAPropertyPath(oix.getForClass(),
							OAString.isNotEmpty(propertyPath) ? propertyPath : propertyName);

					Tuple<OAPropertyPath, Object> t = hmNameValue.get(propertyName);
					if (t == null) {
						bValid = false;
						break;
					}
					bValid = true;

					ImportMatchDetail imd = new ImportMatchDetail();
					imi.importMatchDetails.add(imd);
					imd.propertyName = propertyName;
					imd.value = t.b;
					imd.propertyPath = t.a.getPropertyPath();
				}

				if (bValid) {
					alImportMatch.add(imi);
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
					fmt = pi.getFormat();
					if (OAString.isEmpty(fmt)) {
						fmt = "yyyy-MM-dd";
					}
				} else if (paramClass.equals(OADateTime.class)) {
					fmt = pi.getFormat();
					if (OAString.isEmpty(fmt)) {
						fmt = "yyyy-MM-dd'T'HH:mm:ss";
					}
				} else if (paramClass.equals(OATime.class)) {
					fmt = pi.getFormat();
					if (OAString.isEmpty(fmt)) {
						fmt = "HH:mm:ss";
					}
				} else {
					fmt = pi.getFormat();
				}
				objx = OAConv.convert(pi.getClassType(), jn.asText(), fmt);
			} else {
				objx = OAConv.convert(pi.getClassType(), jn.toString()); //was: asText()
			}
		}
		return objx;
	}

	protected OAObject getLinkObject(OAObject fromObject, OALinkInfo li, JsonNode nodeForLinkProperty) {
		final OAJson oaj = OAThreadLocalDelegate.getOAJackson();

		OAObject objNew = null;
		if (nodeForLinkProperty.isObject()) {
			if (li.getAutoCreateNew() && li.getType() == OALinkInfo.ONE) {
				// was: if (li.getOwner() && li.getAutoCreateNew() && li.getType() == OALinkInfo.ONE) {
				objNew = (OAObject) li.getValue(fromObject);
				loadObject(objNew, nodeForLinkProperty);
			} else {
				String extraWhereClause = null;
				Object extraWhereParam = null;

				boolean b = li.getOwner();
				if (!b) {
					final String uniqueName = li.getUniqueProperty();
					if (OAString.isNotEmpty(uniqueName)) {
						OAObjectInfo oi = OAObjectInfoDelegate.getOAObjectInfo(li.getToClass());
						OAPropertyInfo pi = oi.getPropertyInfo(uniqueName);
						if (pi != null) {
							b = pi.getImportMatch();
						} else {
							OALinkInfo lix = oi.getLinkInfo(uniqueName);
							if (lix != null) {
								b = lix.getImportMatch();
							}
						}
					}
				}

				if (b) {
					OALinkInfo rli = li.getReverseLinkInfo();
					extraWhereClause = rli.getName() + " = ?";
					extraWhereParam = fromObject;

				}
				objNew = getObject(li.getToObjectInfo(), nodeForLinkProperty, extraWhereClause, extraWhereParam);
			}
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
